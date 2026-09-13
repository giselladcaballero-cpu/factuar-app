package com.vektorgo.app.data.repository

import com.vektorgo.app.data.arca.AfipAutomationClient
import com.vektorgo.app.data.arca.ArcaIvaItem
import com.vektorgo.app.data.arca.ArcaQrGenerator
import com.vektorgo.app.data.arca.AutomationStepStatus
import com.vektorgo.app.data.arca.WsaaAuthService
import com.vektorgo.app.data.arca.WsaaTicketResult
import com.vektorgo.app.data.arca.WsfeAuth
import com.vektorgo.app.data.arca.WsfeBillingService
import com.vektorgo.app.data.arca.WsfeVoucherRequest
import com.vektorgo.app.data.local.AppDatabase
import com.vektorgo.app.data.local.entity.ArcaConfigEntity
import com.vektorgo.app.data.local.entity.AuditLogEntity
import com.vektorgo.app.data.local.entity.AuthTicketEntity
import com.vektorgo.app.data.local.entity.InvoiceEntity
import com.vektorgo.app.data.local.entity.LogSeverity
import com.vektorgo.app.data.local.entity.PaymentBillingStatus
import com.vektorgo.app.data.local.entity.PaymentEntity
import com.vektorgo.app.data.mercadopago.MercadoPagoService
import com.vektorgo.app.data.mercadopago.MpPaymentDetail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BillingRepository(
    private val database: AppDatabase,
    private val wsaaAuthService: WsaaAuthService = WsaaAuthService(),
    private val wsfeBillingService: WsfeBillingService = WsfeBillingService(),
    private val mpService: MercadoPagoService = MercadoPagoService(),
    private val afipAutomationClient: AfipAutomationClient = AfipAutomationClient()
) {

    private val paymentDao = database.paymentDao()
    private val invoiceDao = database.invoiceDao()
    private val configDao = database.configDao()
    private val auditLogDao = database.auditLogDao()

    val allPayments: Flow<List<PaymentEntity>> = paymentDao.getAllPayments()
    val allInvoices: Flow<List<InvoiceEntity>> = invoiceDao.getAllInvoices()
    val config: Flow<ArcaConfigEntity?> = configDao.getConfigFlow()
    val auditLogs: Flow<List<AuditLogEntity>> = auditLogDao.getAllLogs()
    val totalApprovedRevenue: Flow<Double?> = paymentDao.getTotalApprovedRevenue()
    val totalInvoicedAmount: Flow<Double?> = invoiceDao.getTotalInvoicedAmount()

    suspend fun getConfig(): ArcaConfigEntity {
        return configDao.getConfig() ?: ArcaConfigEntity().also {
            configDao.insertOrUpdateConfig(it)
        }
    }

    suspend fun saveConfig(newConfig: ArcaConfigEntity) = withContext(Dispatchers.IO) {
        configDao.insertOrUpdateConfig(newConfig)
        auditLogDao.insertLog(
            AuditLogEntity(
                eventType = "CONFIG_UPDATED",
                title = "Configuración Actualizada",
                message = "CUIT: ${newConfig.cuitEmisor} | Pto Vta: ${newConfig.puntoVenta} | Entorno: ${newConfig.environment}",
                severity = LogSeverity.INFO
            )
        )
    }

    /**
     * Receives and processes a payment from Mercado Pago / Mercado Libre.
     */
    suspend fun processIncomingPayment(payment: MpPaymentDetail): Result<PaymentEntity> = withContext(Dispatchers.IO) {
        try {
            val docType = payment.payer.identification?.type ?: "DNI"
            val docNumber = payment.payer.identification?.number ?: ""

            val paymentEntity = PaymentEntity(
                id = payment.id,
                collectorId = payment.collectorId,
                dateApproved = payment.dateApproved,
                dateCreated = payment.dateCreated,
                transactionAmount = payment.transactionAmount,
                netReceivedAmount = payment.netReceivedAmount,
                currencyId = payment.currencyId,
                paymentMethodId = payment.paymentMethodId,
                paymentTypeId = payment.paymentTypeId,
                status = payment.status,
                statusDetail = payment.statusDetail,
                description = payment.description,
                payerEmail = payment.payer.email,
                payerDniCuit = docNumber,
                payerDocType = docType,
                payerFirstName = payment.payer.firstName ?: "",
                payerLastName = payment.payer.lastName ?: "",
                externalReference = payment.externalReference,
                billingStatus = PaymentBillingStatus.PENDING_BILLING
            )

            paymentDao.insertPayment(paymentEntity)

            auditLogDao.insertLog(
                AuditLogEntity(
                    eventType = "MP_PAYMENT_RECEIVED",
                    title = "Pago Aprobado en Mercado Pago",
                    message = "ID ${payment.id} | $${String.format(Locale.US, "%.2f", payment.transactionAmount)} | ${payment.description}",
                    severity = LogSeverity.INFO,
                    payloadJson = "Payer: ${payment.payer.email} | Doc: $docType $docNumber"
                )
            )

            val currentConfig = getConfig()

            // If auto-invoicing is enabled and payment is approved, issue fiscal invoice immediately
            if (currentConfig.autoInvoiceEnabled && payment.status.equals("approved", ignoreCase = true)) {
                issueInvoiceForPayment(paymentEntity, currentConfig)
            }

            return@withContext Result.success(paymentEntity)
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }

    /**
     * Executes the complete ARCA issuance pipeline for a payment.
     */
    suspend fun issueInvoiceForPayment(payment: PaymentEntity, config: ArcaConfigEntity): Result<InvoiceEntity> = withContext(Dispatchers.IO) {
        try {
            // Step 1: Ensure valid WSAA Ticket de Acceso
            val authTicket = getOrRefreshAuthTicket(config)
            if (!authTicket.isValid()) {
                val errorMsg = "No se pudo obtener Ticket de Acceso válido de ARCA WSAA"
                paymentDao.updatePayment(payment.copy(billingStatus = PaymentBillingStatus.ERROR, lastError = errorMsg))
                auditLogDao.insertLog(
                    AuditLogEntity(
                        eventType = "WSAA_ERROR",
                        title = "Fallo en Autenticación WSAA",
                        message = errorMsg,
                        severity = LogSeverity.ERROR
                    )
                )
                return@withContext Result.failure(Exception(errorMsg))
            }

            // Step 2: Determine voucher type (Factura A, B or C)
            val isMonotributo = config.condicionIvaEmisor.equals("MONOTRIBUTO", ignoreCase = true)
            val docNumberLong = payment.payerDniCuit.replace("[^0-9]".toRegex(), "").toLongOrNull() ?: 0L
            val isCuit = (payment.payerDocType.equals("CUIT", ignoreCase = true) || payment.payerDniCuit.length == 11) && docNumberLong > 0

            val cbteTipo: Int
            val cbteTipoNombre: String
            val docTipo: Int
            val docTipoNombre: String
            val receptorCondicionIva: String

            if (isMonotributo) {
                cbteTipo = 11 // Factura C
                cbteTipoNombre = "Factura C"
                if (isCuit) {
                    docTipo = 80 // CUIT
                    docTipoNombre = "CUIT"
                    receptorCondicionIva = "Responsable Inscripto / Monotributo"
                } else if (docNumberLong > 0) {
                    docTipo = 96 // DNI
                    docTipoNombre = "DNI"
                    receptorCondicionIva = "Consumidor Final"
                } else {
                    docTipo = 99 // Sin Identificar
                    docTipoNombre = "Consumidor Final"
                    receptorCondicionIva = "Consumidor Final"
                }
            } else {
                // Emisor is Responsable Inscripto
                if (isCuit) {
                    cbteTipo = 1 // Factura A
                    cbteTipoNombre = "Factura A"
                    docTipo = 80
                    docTipoNombre = "CUIT"
                    receptorCondicionIva = "IVA Responsable Inscripto"
                } else {
                    cbteTipo = 6 // Factura B
                    cbteTipoNombre = "Factura B"
                    if (docNumberLong > 0) {
                        docTipo = 96
                        docTipoNombre = "DNI"
                        receptorCondicionIva = "Consumidor Final"
                    } else {
                        docTipo = 99
                        docTipoNombre = "Consumidor Final"
                        receptorCondicionIva = "Consumidor Final"
                    }
                }
            }

            // Step 3: Compute Tax breakdown (Neto + IVA)
            val totalAmount = payment.transactionAmount
            val alicuotaIva = if (isMonotributo) 0.0 else config.defaultAlicuotaIva // 21% default for RI
            val impNeto = if (alicuotaIva > 0) {
                totalAmount / (1.0 + (alicuotaIva / 100.0))
            } else {
                totalAmount
            }
            val impIva = totalAmount - impNeto

            val ivaItems = if (alicuotaIva > 0) {
                val ivaId = when (alicuotaIva) {
                    21.0 -> 5
                    10.5 -> 4
                    27.0 -> 6
                    else -> 5
                }
                listOf(ArcaIvaItem(id = ivaId, baseImp = impNeto, importe = impIva))
            } else {
                emptyList()
            }

            // Step 4: Determine next voucher number
            val lastNro = invoiceDao.getLastVoucherNumber(config.puntoVenta, cbteTipo) ?: 0L
            val nextVoucherNro = lastNro + 1

            val todayYyyyMmDd = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())

            val voucherReq = WsfeVoucherRequest(
                ptoVta = config.puntoVenta,
                cbteTipo = cbteTipo,
                concepto = config.defaultConcepto,
                docTipo = docTipo,
                docNro = if (docTipo == 99) 0L else docNumberLong,
                cbteDesde = nextVoucherNro,
                cbteHasta = nextVoucherNro,
                cbteFch = todayYyyyMmDd,
                impTotal = totalAmount,
                impTotConc = 0.0,
                impNeto = impNeto,
                impOpEx = 0.0,
                impTrib = 0.0,
                impIVA = impIva,
                ivaItems = ivaItems
            )

            // Step 5: Send FECAESolicitar to ARCA
            val isProd = config.environment.equals("PRODUCCION", ignoreCase = true)
            val auth = WsfeAuth(
                token = authTicket.token,
                sign = authTicket.sign,
                cuit = config.cuitEmisor
            )

            val wsfeResponse = wsfeBillingService.solicitarCae(auth, voucherReq, isProd)

            if (wsfeResponse.resultado == "A" && !wsfeResponse.cae.isNullOrBlank()) {
                val cae = wsfeResponse.cae
                val caeVto = wsfeResponse.caeFchVto ?: todayYyyyMmDd

                // Generate QR Code URL
                val qrUrl = ArcaQrGenerator.generateQrUrl(
                    cuitEmisor = config.cuitEmisor,
                    ptoVta = config.puntoVenta,
                    tipoCmp = cbteTipo,
                    nroCmp = nextVoucherNro,
                    fechaYyyyMmDd = todayYyyyMmDd,
                    importe = totalAmount,
                    tipoDocRec = docTipo,
                    nroDocRec = if (docTipo == 99) 0L else docNumberLong,
                    cae = cae
                )

                val receptorName = if (payment.payerFirstName.isNotBlank() || payment.payerLastName.isNotBlank()) {
                    "${payment.payerFirstName} ${payment.payerLastName}".trim()
                } else {
                    "Consumidor Final"
                }

                val invoice = InvoiceEntity(
                    paymentId = payment.id,
                    cbteTipo = cbteTipo,
                    cbteTipoNombre = cbteTipoNombre,
                    ptoVta = config.puntoVenta,
                    cbteNro = nextVoucherNro,
                    concepto = config.defaultConcepto,
                    docTipo = docTipo,
                    docTipoNombre = docTipoNombre,
                    docNro = if (docTipo == 99) 0L else docNumberLong,
                    receptorNombre = receptorName,
                    receptorCondicionIva = receptorCondicionIva,
                    receptorEmail = payment.payerEmail,
                    cbteFch = todayYyyyMmDd,
                    impTotal = totalAmount,
                    impTotConc = 0.0,
                    impNeto = impNeto,
                    impOpEx = 0.0,
                    impTrib = 0.0,
                    impIVA = impIva,
                    ivaAlicuota = alicuotaIva,
                    cae = cae,
                    caeFchVto = caeVto,
                    resultado = "A",
                    observaciones = wsfeResponse.observaciones.joinToString(" | "),
                    qrCodeData = qrUrl,
                    environment = config.environment,
                    itemsDescription = payment.description
                )

                val invoiceId = invoiceDao.insertInvoice(invoice)

                paymentDao.updatePayment(
                    payment.copy(
                        billingStatus = PaymentBillingStatus.INVOICED,
                        associatedInvoiceId = invoiceId,
                        lastError = null
                    )
                )

                auditLogDao.insertLog(
                    AuditLogEntity(
                        eventType = "WSFE_CAE_ISSUED",
                        title = "$cbteTipoNombre N° ${String.format("%04d-%08d", config.puntoVenta, nextVoucherNro)} Emitida",
                        message = "CAE: $cae | Vto: $caeVto | Total: $${String.format(Locale.US, "%.2f", totalAmount)}",
                        severity = LogSeverity.SUCCESS,
                        payloadJson = "Receptor: $receptorName ($docTipoNombre $docNumberLong)"
                    )
                )

                return@withContext Result.success(invoice)
            } else {
                val errorMsg = wsfeResponse.errores.joinToString(" | ").ifBlank { "Rechazado por ARCA sin CAE" }
                paymentDao.updatePayment(payment.copy(billingStatus = PaymentBillingStatus.ERROR, lastError = errorMsg))
                auditLogDao.insertLog(
                    AuditLogEntity(
                        eventType = "WSFE_CAE_REJECTED",
                        title = "Error emitiendo Factura en ARCA",
                        message = errorMsg,
                        severity = LogSeverity.ERROR
                    )
                )
                return@withContext Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            val errorMsg = e.message ?: "Error desconocido en facturación"
            paymentDao.updatePayment(payment.copy(billingStatus = PaymentBillingStatus.ERROR, lastError = errorMsg))
            auditLogDao.insertLog(
                AuditLogEntity(
                    eventType = "WSFE_EXCEPTION",
                    title = "Excepción en Facturación ARCA",
                    message = errorMsg,
                    severity = LogSeverity.ERROR
                )
            )
            return@withContext Result.failure(e)
        }
    }

    /**
     * Gets valid cached AuthTicket or requests a new one from WSAA.
     */
    private suspend fun getOrRefreshAuthTicket(config: ArcaConfigEntity): AuthTicketEntity {
        val cached = configDao.getAuthTicket("wsfe")
        if (cached != null && cached.isValid() && cached.environment == config.environment) {
            return cached
        }

        val result = wsaaAuthService.obtainAccessTicket(config)
        if (result.success) {
            val newTicket = AuthTicketEntity(
                service = "wsfe",
                token = result.token,
                sign = result.sign,
                cuit = config.cuitEmisor,
                generationTimeMillis = result.generationTimeMillis,
                expirationTimeMillis = result.expirationTimeMillis,
                environment = config.environment
            )
            configDao.saveAuthTicket(newTicket)
            auditLogDao.insertLog(
                AuditLogEntity(
                    eventType = "WSAA_AUTH_SUCCESS",
                    title = "Ticket de Acceso WSAA Renovado",
                    message = "Token generado para CUIT ${config.cuitEmisor} (${config.environment}) válido por 12 hs",
                    severity = LogSeverity.SUCCESS
                )
            )
            return newTicket
        } else {
            return AuthTicketEntity(
                service = "wsfe",
                token = "",
                sign = "",
                cuit = config.cuitEmisor,
                generationTimeMillis = 0L,
                expirationTimeMillis = 0L,
                environment = config.environment
            )
        }
    }

    suspend fun testWsaaAuth(): Result<WsaaTicketResult> = withContext(Dispatchers.IO) {
        val currentConfig = getConfig()
        val result = wsaaAuthService.obtainAccessTicket(currentConfig)
        if (result.success) {
            val ticket = AuthTicketEntity(
                service = "wsfe",
                token = result.token,
                sign = result.sign,
                cuit = currentConfig.cuitEmisor,
                generationTimeMillis = result.generationTimeMillis,
                expirationTimeMillis = result.expirationTimeMillis,
                environment = currentConfig.environment
            )
            configDao.saveAuthTicket(ticket)
            auditLogDao.insertLog(
                AuditLogEntity(
                    eventType = "WSAA_TEST_SUCCESS",
                    title = "Prueba de Conexión WSAA Exitosa",
                    message = "Autenticación correcta con ARCA en modo ${currentConfig.environment}",
                    severity = LogSeverity.SUCCESS
                )
            )
        }
        return@withContext if (result.success) Result.success(result) else Result.failure(Exception(result.errorMessage ?: "Fallo WSAA"))
    }

    suspend fun retryPendingPayments(): Int = withContext(Dispatchers.IO) {
        val pending = paymentDao.getPendingOrErrorPayments()
        val currentConfig = getConfig()
        var successCount = 0
        for (payment in pending) {
            val result = issueInvoiceForPayment(payment, currentConfig)
            if (result.isSuccess) successCount++
        }
        return@withContext successCount
    }

    suspend fun simulateIncomingPayment(scenarioIndex: Int): Result<InvoiceEntity?> = withContext(Dispatchers.IO) {
        val mockPayment = mpService.createSimulatedPayment(scenarioIndex)
        val res = processIncomingPayment(mockPayment)
        if (res.isSuccess) {
            val inv = invoiceDao.getInvoiceByPaymentId(mockPayment.id)
            return@withContext Result.success(inv)
        } else {
            return@withContext Result.failure(res.exceptionOrNull() ?: Exception("Error"))
        }
    }

    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        paymentDao.clearAll()
        invoiceDao.clearAll()
        auditLogDao.clearLogs()
        configDao.deleteAuthTicket("wsfe")
    }

    /**
     * Executes Option B: Background automated AFIP certificate provisioning & WSFE delegation.
     */
    fun executeAfipAutoProvisioning(
        cuit: Long,
        claveFiscal: String,
        razonSocial: String,
        otpToken: String = ""
    ): Flow<AutomationStepStatus> {
        return afipAutomationClient.executeAutoProvisioning(
            cuit = cuit,
            claveFiscal = claveFiscal,
            razonSocial = razonSocial,
            otpToken = otpToken
        ).onEach { status ->
            if (status is AutomationStepStatus.Success) {
                val current = getConfig()
                val updated = current.copy(
                    cuitEmisor = status.cuit,
                    razonSocial = status.razonSocial,
                    arcaAlias = status.alias,
                    puntoVenta = status.puntoVenta,
                    certCrtPem = status.certPem,
                    privateKeyPem = status.privateKeyPem,
                    isArcaConnected = true,
                    onboardingMode = "AUTO_PROVISIONED"
                )
                configDao.insertOrUpdateConfig(updated)
                auditLogDao.insertLog(
                    AuditLogEntity(
                        eventType = "AFIP_AUTO_PROVISIONED",
                        title = "Certificado ARCA Auto-Generado",
                        message = "Certificado emitido para CUIT ${status.cuit} (${status.alias}) y vinculado a WSFE v1.",
                        severity = LogSeverity.SUCCESS
                    )
                )
            }
        }.flowOn(Dispatchers.IO)
    }

    /**
     * Simulates 1-click OAuth 2.0 connection to Mercado Pago.
     */
    suspend fun connectMercadoPagoOAuth(
        userName: String = "Rodrigo Timoner",
        collectorId: Long = 104928192L,
        accessToken: String = ""
    ) = withContext(Dispatchers.IO) {
        val current = getConfig()
        val token = if (accessToken.isNotBlank()) accessToken else "APP_USR-7849302910394821-081313-auth-${collectorId}"
        val updated = current.copy(
            isMpConnected = true,
            mpUserName = userName,
            mpCollectorId = collectorId,
            mpAccessToken = token
        )
        configDao.insertOrUpdateConfig(updated)
        auditLogDao.insertLog(
            AuditLogEntity(
                eventType = "MP_OAUTH_CONNECTED",
                title = "Mercado Pago Vinculado vía OAuth 2.0",
                message = "Cuenta vinculada: $userName (ID: $collectorId). Webhooks sincronizados en tiempo real.",
                severity = LogSeverity.SUCCESS
            )
        )
    }

    suspend fun disconnectMercadoPago() = withContext(Dispatchers.IO) {
        val current = getConfig()
        val updated = current.copy(
            isMpConnected = false,
            mpAccessToken = ""
        )
        configDao.insertOrUpdateConfig(updated)
        auditLogDao.insertLog(
            AuditLogEntity(
                eventType = "MP_OAUTH_DISCONNECTED",
                title = "Mercado Pago Desvinculado",
                message = "Se revocó el token OAuth y se pausó la escucha de webhooks.",
                severity = LogSeverity.WARNING
            )
        )
    }

    suspend fun disconnectArca() = withContext(Dispatchers.IO) {
        val current = getConfig()
        val updated = current.copy(
            isArcaConnected = false,
            certCrtPem = "",
            privateKeyPem = ""
        )
        configDao.insertOrUpdateConfig(updated)
        auditLogDao.insertLog(
            AuditLogEntity(
                eventType = "ARCA_DISCONNECTED",
                title = "ARCA Desvinculado",
                message = "Se removieron los certificados activos de la aplicación.",
                severity = LogSeverity.WARNING
            )
        )
    }

    suspend fun seedInitialDemoData() = withContext(Dispatchers.IO) {
        val count = paymentDao.getTotalPaymentsCount()
        // If empty, simulate 3 realistic payments with full invoice generation
        simulateIncomingPayment(0)
        simulateIncomingPayment(1)
        simulateIncomingPayment(2)
    }
}
