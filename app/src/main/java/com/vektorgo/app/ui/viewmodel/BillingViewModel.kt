package com.vektorgo.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vektorgo.app.data.arca.AutomationStepStatus
import com.vektorgo.app.data.local.AppDatabase
import com.vektorgo.app.data.local.entity.ArcaConfigEntity
import com.vektorgo.app.data.local.entity.AuditLogEntity
import com.vektorgo.app.data.local.entity.InvoiceEntity
import com.vektorgo.app.data.local.entity.PaymentBillingStatus
import com.vektorgo.app.data.local.entity.PaymentEntity
import com.vektorgo.app.data.repository.BillingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BillingUiState(
    val payments: List<PaymentEntity> = emptyList(),
    val invoices: List<InvoiceEntity> = emptyList(),
    val config: ArcaConfigEntity = ArcaConfigEntity(),
    val auditLogs: List<AuditLogEntity> = emptyList(),
    val selectedInvoice: InvoiceEntity? = null,
    val selectedPayment: PaymentEntity? = null,
    val totalApprovedRevenue: Double = 0.0,
    val totalInvoicedAmount: Double = 0.0,
    val isProcessing: Boolean = false,
    val statusMessage: String? = null,
    val filterStatus: PaymentBillingStatus? = null,
    val searchQuery: String = "",
    val activeTab: Int = 0, // 0: Dashboard, 1: Transacciones, 2: Detalle Comprobante, 3: Configuración, 4: Arquitectura, 5: Auditoría
    val wsaaStatus: String = "ACTIVO",

    // Automated Certificate Provisioning (Option B) State
    val isAutoProvisioning: Boolean = false,
    val provisioningStepNumber: Int = 0,
    val provisioningTotalSteps: Int = 6,
    val provisioningStepTitle: String = "",
    val provisioningStepDesc: String = "",
    val provisioningError: String? = null,
    val isProvisioningSuccess: Boolean = false,
    val showAutoProvisionDialog: Boolean = false,
    val showMpOAuthDialog: Boolean = false
)

class BillingViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BillingRepository(AppDatabase.getDatabase(application))

    private val _selectedInvoice = MutableStateFlow<InvoiceEntity?>(null)
    private val _selectedPayment = MutableStateFlow<PaymentEntity?>(null)
    private val _isProcessing = MutableStateFlow(false)
    private val _statusMessage = MutableStateFlow<String?>(null)
    private val _filterStatus = MutableStateFlow<PaymentBillingStatus?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _activeTab = MutableStateFlow(0)
    private val _wsaaStatus = MutableStateFlow("CONECTADO (Homologación)")

    // Provisioning states
    private val _isAutoProvisioning = MutableStateFlow(false)
    private val _provisioningStepNumber = MutableStateFlow(0)
    private val _provisioningTotalSteps = MutableStateFlow(6)
    private val _provisioningStepTitle = MutableStateFlow("")
    private val _provisioningStepDesc = MutableStateFlow("")
    private val _provisioningError = MutableStateFlow<String?>(null)
    private val _isProvisioningSuccess = MutableStateFlow(false)
    private val _showAutoProvisionDialog = MutableStateFlow(false)
    private val _showMpOAuthDialog = MutableStateFlow(false)

    val uiState: StateFlow<BillingUiState> = combine(
        combine(
            repository.allPayments,
            repository.allInvoices,
            repository.config,
            repository.auditLogs,
            repository.totalApprovedRevenue
        ) { p, i, c, a, r ->
            Tuple5(p, i, c ?: ArcaConfigEntity(), a, r ?: 0.0)
        },
        combine(
            repository.totalInvoicedAmount,
            _selectedInvoice,
            _selectedPayment,
            _isProcessing,
            _statusMessage
        ) { invAmt, selInv, selPay, isProc, statusMsg ->
            Tuple5(invAmt ?: 0.0, selInv, selPay, isProc, statusMsg)
        },
        combine(
            _filterStatus,
            _searchQuery,
            _activeTab,
            _wsaaStatus,
            _isAutoProvisioning
        ) { fStat, sQuery, tab, wsaa, isProv ->
            Tuple5(fStat, sQuery, tab, wsaa, isProv)
        },
        combine(
            _provisioningStepNumber,
            _provisioningTotalSteps,
            _provisioningStepTitle,
            _provisioningStepDesc,
            _provisioningError
        ) { pStep, pTotal, pTitle, pDesc, pErr ->
            Tuple5(pStep, pTotal, pTitle, pDesc, pErr)
        },
        combine(
            _isProvisioningSuccess,
            _showAutoProvisionDialog,
            _showMpOAuthDialog
        ) { pSucc, showProvDiag, showMpDiag ->
            Triple(pSucc, showProvDiag, showMpDiag)
        }
    ) { t1, t2, t3, t4, t5 ->
        val payments = t1.a
        val invoices = t1.b
        val config = t1.c
        val auditLogs = t1.d
        val revenue = t1.e

        val invoiced = t2.a
        val selectedInvoice = t2.b
        val selectedPayment = t2.c
        val isProcessing = t2.d
        val statusMessage = t2.e

        val filterStatus = t3.a
        val searchQuery = t3.b
        val activeTab = t3.c
        val wsaaStatus = t3.d
        val isAutoProvisioning = t3.e

        val provisioningStepNumber = t4.a
        val provisioningTotalSteps = t4.b
        val provisioningStepTitle = t4.c
        val provisioningStepDesc = t4.d
        val provisioningError = t4.e

        val isProvisioningSuccess = t5.first
        val showAutoProvisionDialog = t5.second
        val showMpOAuthDialog = t5.third

        val filteredPayments = payments.filter { p ->
            val matchesFilter = filterStatus == null || p.billingStatus == filterStatus
            val matchesQuery = searchQuery.isBlank() ||
                    p.description.contains(searchQuery, ignoreCase = true) ||
                    p.payerEmail.contains(searchQuery, ignoreCase = true) ||
                    p.payerDniCuit.contains(searchQuery, ignoreCase = true) ||
                    p.id.toString().contains(searchQuery) ||
                    p.payerFirstName.contains(searchQuery, ignoreCase = true) ||
                    p.payerLastName.contains(searchQuery, ignoreCase = true)
            matchesFilter && matchesQuery
        }

        BillingUiState(
            payments = filteredPayments,
            invoices = invoices,
            config = config,
            auditLogs = auditLogs,
            selectedInvoice = selectedInvoice ?: invoices.firstOrNull(),
            selectedPayment = selectedPayment,
            totalApprovedRevenue = revenue,
            totalInvoicedAmount = invoiced,
            isProcessing = isProcessing,
            statusMessage = statusMessage,
            filterStatus = filterStatus,
            searchQuery = searchQuery,
            activeTab = activeTab,
            wsaaStatus = wsaaStatus,
            isAutoProvisioning = isAutoProvisioning,
            provisioningStepNumber = provisioningStepNumber,
            provisioningTotalSteps = provisioningTotalSteps,
            provisioningStepTitle = provisioningStepTitle,
            provisioningStepDesc = provisioningStepDesc,
            provisioningError = provisioningError,
            isProvisioningSuccess = isProvisioningSuccess,
            showAutoProvisionDialog = showAutoProvisionDialog,
            showMpOAuthDialog = showMpOAuthDialog
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BillingUiState()
    )

    fun selectTab(tab: Int) {
        _activeTab.value = tab
    }

    fun selectInvoice(invoice: InvoiceEntity) {
        _selectedInvoice.value = invoice
        _activeTab.value = 2 // Switch to Invoice Detail tab
    }

    fun selectInvoiceByPaymentId(paymentId: Long) {
        val invoice = uiState.value.invoices.find { it.paymentId == paymentId }
        if (invoice != null) {
            selectInvoice(invoice)
        }
    }

    fun selectPayment(payment: PaymentEntity) {
        _selectedPayment.value = payment
    }

    fun setFilterStatus(status: PaymentBillingStatus?) {
        _filterStatus.value = status
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleAutoInvoice(enabled: Boolean) {
        viewModelScope.launch {
            val current = repository.getConfig()
            repository.saveConfig(current.copy(autoInvoiceEnabled = enabled))
            _statusMessage.value = if (enabled) "Facturación automática activada" else "Facturación automática pausada"
        }
    }

    fun simulatePayment(scenarioIndex: Int) {
        viewModelScope.launch {
            _isProcessing.value = true
            _statusMessage.value = "Simulando pago de Mercado Pago y solicitando CAE a ARCA..."
            val result = repository.simulateIncomingPayment(scenarioIndex)
            _isProcessing.value = false
            if (result.isSuccess) {
                val invoice = result.getOrNull()
                _statusMessage.value = if (invoice != null) {
                    "Pago aprobado y ${invoice.cbteTipoNombre} emitida con CAE ${invoice.cae}"
                } else {
                    "Pago recibido y guardado"
                }
            } else {
                _statusMessage.value = "Error al procesar: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun retryInvoice(payment: PaymentEntity) {
        viewModelScope.launch {
            _isProcessing.value = true
            _statusMessage.value = "Reintentando emisión en ARCA para pago #${payment.id}..."
            val config = repository.getConfig()
            val res = repository.issueInvoiceForPayment(payment, config)
            _isProcessing.value = false
            if (res.isSuccess) {
                val inv = res.getOrNull()
                _selectedInvoice.value = inv
                _statusMessage.value = "Factura emitida con éxito. CAE: ${inv?.cae}"
            } else {
                _statusMessage.value = "Error en ARCA: ${res.exceptionOrNull()?.message}"
            }
        }
    }

    fun retryAllPending() {
        viewModelScope.launch {
            _isProcessing.value = true
            _statusMessage.value = "Sincronizando y reintentando pagos pendientes..."
            val count = repository.retryPendingPayments()
            _isProcessing.value = false
            _statusMessage.value = "Se emitieron $count comprobantes pendientes correctamente"
        }
    }

    fun testWsaaConnection() {
        viewModelScope.launch {
            _isProcessing.value = true
            _wsaaStatus.value = "PROBANDO..."
            _statusMessage.value = "Contactando ARCA WSAA..."
            val result = repository.testWsaaAuth()
            _isProcessing.value = false
            if (result.isSuccess) {
                _wsaaStatus.value = "CONECTADO (${uiState.value.config.environment})"
                _statusMessage.value = "Autenticación WSAA exitosa. Token obtenido."
            } else {
                _wsaaStatus.value = "ERROR"
                _statusMessage.value = "Fallo WSAA: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun saveConfig(config: ArcaConfigEntity) {
        viewModelScope.launch {
            _isProcessing.value = true
            repository.saveConfig(config)
            _isProcessing.value = false
            _statusMessage.value = "Configuración guardada correctamente"
        }
    }

    /**
     * Executes Option B (Automated Certificate Provisioning via Clave Fiscal in background)
     */
    fun startAutoProvisioning(
        cuit: Long,
        claveFiscal: String,
        razonSocial: String,
        otpToken: String = ""
    ) {
        viewModelScope.launch {
            _isAutoProvisioning.value = true
            _provisioningError.value = null
            _isProvisioningSuccess.value = false

            repository.executeAfipAutoProvisioning(
                cuit = cuit,
                claveFiscal = claveFiscal,
                razonSocial = razonSocial,
                otpToken = otpToken
            ).collect { status ->
                when (status) {
                    is AutomationStepStatus.Progress -> {
                        _provisioningStepNumber.value = status.stepIndex
                        _provisioningTotalSteps.value = status.totalSteps
                        _provisioningStepTitle.value = status.title
                        _provisioningStepDesc.value = status.description
                    }
                    is AutomationStepStatus.Success -> {
                        _isAutoProvisioning.value = false
                        _isProvisioningSuccess.value = true
                        _provisioningStepTitle.value = "¡Vinculación Exitosa!"
                        _provisioningStepDesc.value = status.message
                        _wsaaStatus.value = "CONECTADO (${status.alias})"
                        _statusMessage.value = "ARCA configurado y operativo para CUIT ${status.cuit}"
                    }
                    is AutomationStepStatus.Error -> {
                        _isAutoProvisioning.value = false
                        _provisioningError.value = status.errorMessage
                        _statusMessage.value = "Error: ${status.errorMessage}"
                    }
                }
            }
        }
    }

    fun openAutoProvisionDialog() {
        _showAutoProvisionDialog.value = true
        _provisioningError.value = null
        _isProvisioningSuccess.value = false
        _provisioningStepNumber.value = 0
        _provisioningStepTitle.value = ""
        _provisioningStepDesc.value = ""
    }

    fun closeAutoProvisionDialog() {
        _showAutoProvisionDialog.value = false
        _isAutoProvisioning.value = false
    }

    fun openMpOAuthDialog() {
        _showMpOAuthDialog.value = true
    }

    fun closeMpOAuthDialog() {
        _showMpOAuthDialog.value = false
    }

    fun connectMercadoPagoOAuth(
        userName: String = "Rodrigo Timoner",
        collectorId: Long = 104928192L,
        accessToken: String = ""
    ) {
        viewModelScope.launch {
            _isProcessing.value = true
            repository.connectMercadoPagoOAuth(userName, collectorId, accessToken)
            _isProcessing.value = false
            _showMpOAuthDialog.value = false
            _statusMessage.value = "Mercado Pago conectado exitosamente (Collector: $collectorId)"
        }
    }

    /**
     * Handles Deep Link returned from OAuth flow (factuar://mp-connected?access_token=...&user_id=...&name=...)
     */
    fun handleOAuthDeepLink(uri: android.net.Uri) {
        val collectorIdStr = uri.getQueryParameter("user_id")
        val userName = uri.getQueryParameter("name")?.takeIf { it.isNotBlank() } ?: "Cuenta Mercado Pago"
        val token = uri.getQueryParameter("access_token") ?: ""
        val collectorId = collectorIdStr?.toLongOrNull() ?: 0L

        if (token.isBlank()) {
            _statusMessage.value = "No se pudo completar la vinculación: Mercado Pago no devolvió un token válido."
            return
        }

        viewModelScope.launch {
            _isProcessing.value = true
            repository.connectMercadoPagoOAuth(userName, collectorId, token)
            _isProcessing.value = false
            _activeTab.value = 3 // Switch to Settings screen to show confirmation
            _statusMessage.value = "¡Conexión Automática Exitosa! Mercado Pago vinculado (ID: $collectorId)"
        }
    }

    fun disconnectMercadoPago() {
        viewModelScope.launch {
            repository.disconnectMercadoPago()
            _statusMessage.value = "Mercado Pago desvinculado"
        }
    }

    fun disconnectArca() {
        viewModelScope.launch {
            repository.disconnectArca()
            _wsaaStatus.value = "DESCONECTADO"
            _statusMessage.value = "ARCA desvinculado"
        }
    }

    fun seedDemoData() {
        viewModelScope.launch {
            _isProcessing.value = true
            repository.seedInitialDemoData()
            _isProcessing.value = false
            _statusMessage.value = "Datos de prueba generados con éxito"
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            _selectedInvoice.value = null
            _selectedPayment.value = null
            _statusMessage.value = "Historial y comprobantes limpiados"
        }
    }

    fun activateSubscription(provider: String = "MERCADO_PAGO", price: Double = 14999.0) {
        viewModelScope.launch {
            _isProcessing.value = true
            val current = repository.getConfig()
            val validUntil = System.currentTimeMillis() + (30L * 24 * 3600 * 1000) // 30 days
            repository.saveConfig(
                current.copy(
                    isSubscribed = true,
                    subscriptionPlan = "Plan Mensual Ilimitado",
                    subscriptionPriceArs = price,
                    subscriptionValidUntil = validUntil,
                    subscriptionProvider = provider
                )
            )
            _isProcessing.value = false
            _statusMessage.value = "¡Suscripción mensual activada con éxito! Facturación ilimitada habilitada."
        }
    }

    fun cancelSubscription() {
        viewModelScope.launch {
            val current = repository.getConfig()
            repository.saveConfig(
                current.copy(
                    isSubscribed = false
                )
            )
            _statusMessage.value = "Suscripción pausada"
        }
    }

    fun toggleSubscription(enabled: Boolean) {
        if (enabled) {
            activateSubscription()
        } else {
            cancelSubscription()
        }
    }

    fun testWebhookRelay() {
        viewModelScope.launch {
            _isProcessing.value = true
            val config = repository.getConfig()
            val result = com.vektorgo.app.data.monitoring.TelemetryManager.pingWebhookRelay(
                urlStr = config.webhookRelayUrl,
                cuit = config.cuitEmisor.toString(),
                collectorId = config.mpCollectorId
            )
            _isProcessing.value = false
            _statusMessage.value = result.message
        }
    }

    fun dismissStatusMessage() {
        _statusMessage.value = null
    }
}

// Data holders for combining flows
private data class Tuple5<A, B, C, D, E>(val a: A, val b: B, val c: C, val d: D, val e: E)
