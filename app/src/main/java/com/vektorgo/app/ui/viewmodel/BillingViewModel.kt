package com.vektorgo.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vektorgo.app.data.arca.CsrGenerationStatus
import com.vektorgo.app.data.local.AppDatabase
import com.vektorgo.app.data.local.entity.ArcaConfigEntity
import com.vektorgo.app.data.local.entity.AuditLogEntity
import com.vektorgo.app.data.local.entity.InvoiceEntity
import com.vektorgo.app.data.local.entity.PaymentBillingStatus
import com.vektorgo.app.data.local.entity.PaymentEntity
import com.vektorgo.app.data.repository.BillingRepository
import com.vektorgo.app.util.SoundPlayer
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

    // ARCA CSR generation state (local key pair + PKCS#10, no Clave Fiscal involved)
    val csr: CsrUiState = CsrUiState(),

    // Mercado Pago manual Access Token connection state
    val mpConnect: MpConnectUiState = MpConnectUiState()
)

data class CsrUiState(
    val isGenerating: Boolean = false,
    val stepNumber: Int = 0,
    val totalSteps: Int = 2,
    val stepTitle: String = "",
    val stepDesc: String = "",
    val error: String? = null,
    val isReady: Boolean = false,
    val csrPem: String = "",
    val privateKeyPem: String = "",
    val selfSignedCertPem: String = "",
    val cuit: Long = 0L,
    val razonSocial: String = "",
    val isSaved: Boolean = false,
    val showDialog: Boolean = false
)

data class MpConnectUiState(
    val showDialog: Boolean = false,
    val isConnecting: Boolean = false,
    val error: String? = null
)

class BillingViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val AUTO_SYNC_INTERVAL_MS = 10 * 60 * 1000L // 10 minutes
    }

    private val repository = BillingRepository(AppDatabase.getDatabase(application))

    private val _selectedInvoice = MutableStateFlow<InvoiceEntity?>(null)
    private val _selectedPayment = MutableStateFlow<PaymentEntity?>(null)
    private val _isProcessing = MutableStateFlow(false)
    private val _statusMessage = MutableStateFlow<String?>(null)
    private val _filterStatus = MutableStateFlow<PaymentBillingStatus?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _activeTab = MutableStateFlow(0)
    private val _wsaaStatus = MutableStateFlow("CONECTADO (Homologación)")

    // ARCA CSR generation + Mercado Pago manual token connection state
    private val _csrUiState = MutableStateFlow(CsrUiState())
    private val _mpConnectUiState = MutableStateFlow(MpConnectUiState())

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
            _wsaaStatus
        ) { fStat, sQuery, tab, wsaa ->
            listOf(fStat, sQuery, tab, wsaa)
        },
        combine(
            _csrUiState,
            _mpConnectUiState
        ) { csr, mp -> csr to mp }
    ) { t1, t2, t3, t4 ->
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

        @Suppress("UNCHECKED_CAST")
        val filterStatus = t3[0] as PaymentBillingStatus?
        val searchQuery = t3[1] as String
        val activeTab = t3[2] as Int
        val wsaaStatus = t3[3] as String

        val csr = t4.first
        val mpConnect = t4.second

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
            csr = csr,
            mpConnect = mpConnect
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BillingUiState()
    )

    init {
        startAutoSyncLoop()
    }

    /**
     * Keeps Mercado Pago movements (payments + transfers) up to date without
     * the merchant having to remember to tap "Sincronizar". Runs only while
     * this ViewModel/app process is alive — Android enforces a 15-minute
     * floor on WorkManager's PeriodicWorkRequest, so a real background job
     * can't honor a 10-minute cadence; this in-process loop can, at the cost
     * of pausing whenever the app is killed (it picks back up, and reconciles
     * the gap, next time it's opened).
     */
    private fun startAutoSyncLoop() {
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(AUTO_SYNC_INTERVAL_MS)
                if (uiState.value.config.isMpConnected) {
                    autoSyncMercadoPagoMovementsSilently()
                }
            }
        }
    }

    /**
     * Same reconciliation as syncMercadoPagoMovements(), but doesn't toggle
     * isProcessing (no spinner flash every 10 minutes) and only surfaces a
     * status message when there's actually something new to report.
     */
    private suspend fun autoSyncMercadoPagoMovementsSilently() {
        val result = repository.syncMercadoPagoMovements()
        val count = result.getOrNull() ?: return
        if (count > 0) {
            _statusMessage.value = "Se detectaron $count movimientos nuevos de Mercado Pago"
        }
    }

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
                    SoundPlayer.playInvoiceEmitted(getApplication())
                    "Pago aprobado y ${invoice.cbteTipoNombre} emitida con CAE ${invoice.cae}"
                } else {
                    "Pago recibido y guardado"
                }
            } else {
                _statusMessage.value = "Error al procesar: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun emitCreditNote(invoice: InvoiceEntity) {
        viewModelScope.launch {
            _isProcessing.value = true
            _statusMessage.value = "Emitiendo Nota de Crédito en ARCA..."
            val res = repository.emitCreditNoteForInvoice(invoice.id)
            _isProcessing.value = false
            if (res.isSuccess) {
                val nc = res.getOrNull()
                _selectedInvoice.value = nc
                SoundPlayer.playInvoiceEmitted(getApplication())
                _statusMessage.value = "Nota de Crédito emitida con éxito. CAE: ${nc?.cae}"
            } else {
                _statusMessage.value = "Error en ARCA: ${res.exceptionOrNull()?.message}"
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
                SoundPlayer.playInvoiceEmitted(getApplication())
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

    fun syncMercadoPagoMovements() {
        viewModelScope.launch {
            _isProcessing.value = true
            _statusMessage.value = "Sincronizando movimientos de Mercado Pago (pagos y transferencias)..."
            val result = repository.syncMercadoPagoMovements()
            _isProcessing.value = false
            _statusMessage.value = if (result.isSuccess) {
                val count = result.getOrDefault(0)
                if (count > 0) "Se incorporaron $count movimientos nuevos de Mercado Pago" else "Ya estabas al día, no había movimientos nuevos"
            } else {
                "Error sincronizando Mercado Pago: ${result.exceptionOrNull()?.message}"
            }
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
     * Generates a real RSA key pair + PKCS#10 CSR on-device. Never touches the
     * Clave Fiscal — the user uploads the CSR themselves on afip.gob.ar and
     * pastes back the certificate ARCA issues, saved via [saveArcaCertificate].
     */
    fun startCsrGeneration(cuit: Long, razonSocial: String) {
        viewModelScope.launch {
            _csrUiState.update {
                it.copy(isGenerating = true, error = null, isReady = false, isSaved = false)
            }

            repository.generateArcaCsr(cuit, razonSocial).collect { status ->
                when (status) {
                    is CsrGenerationStatus.Progress -> {
                        _csrUiState.update {
                            it.copy(
                                stepNumber = status.stepIndex,
                                totalSteps = status.totalSteps,
                                stepTitle = status.title,
                                stepDesc = status.description
                            )
                        }
                    }
                    is CsrGenerationStatus.Ready -> {
                        _csrUiState.update {
                            it.copy(
                                isGenerating = false,
                                isReady = true,
                                csrPem = status.csrPem,
                                privateKeyPem = status.privateKeyPem,
                                selfSignedCertPem = status.selfSignedCertPem,
                                cuit = status.cuit,
                                razonSocial = status.razonSocial
                            )
                        }
                    }
                    is CsrGenerationStatus.Error -> {
                        _csrUiState.update { it.copy(isGenerating = false, error = status.errorMessage) }
                        _statusMessage.value = "Error: ${status.errorMessage}"
                    }
                }
            }
        }
    }

    /**
     * Saves the real certificate ARCA issued for the CSR generated by
     * [startCsrGeneration], together with the matching private key.
     */
    fun saveArcaCertificate(certPem: String) {
        val csr = _csrUiState.value
        if (!csr.isReady || csr.privateKeyPem.isBlank()) return
        viewModelScope.launch {
            _isProcessing.value = true
            repository.saveArcaCertificate(csr.cuit, csr.razonSocial, csr.privateKeyPem, certPem)
            _isProcessing.value = false
            _csrUiState.update { it.copy(isSaved = true) }
            _wsaaStatus.value = "CONECTADO (${csr.razonSocial})"
            _statusMessage.value = "Certificado ARCA guardado para CUIT ${csr.cuit}"
        }
    }

    fun openCsrDialog() {
        _csrUiState.value = CsrUiState(showDialog = true)
    }

    fun closeCsrDialog() {
        _csrUiState.update { it.copy(showDialog = false, isGenerating = false) }
    }

    fun openMpTokenDialog() {
        _mpConnectUiState.value = MpConnectUiState(showDialog = true)
    }

    fun closeMpTokenDialog() {
        _mpConnectUiState.value = MpConnectUiState(showDialog = false)
    }

    /**
     * Called when the OAuth deep link (factuar://mp-connected) comes back
     * from the Mercado Pago bridge with an already-validated access token —
     * no need to call the MP API again, just persist it.
     */
    fun completeMercadoPagoOAuth(accessToken: String, userId: Long, name: String) {
        viewModelScope.launch {
            repository.saveMercadoPagoConnection(accessToken, userId, name)
            _mpConnectUiState.value = MpConnectUiState(showDialog = false)
            _statusMessage.value = "Mercado Pago conectado correctamente"
        }
    }

    fun connectMercadoPago(accessToken: String) {
        viewModelScope.launch {
            _mpConnectUiState.update { it.copy(isConnecting = true, error = null) }
            val result = repository.connectMercadoPago(accessToken)
            if (result.isSuccess) {
                _mpConnectUiState.value = MpConnectUiState(showDialog = false)
                _statusMessage.value = "Mercado Pago conectado correctamente"
            } else {
                _mpConnectUiState.update {
                    it.copy(isConnecting = false, error = result.exceptionOrNull()?.message ?: "No se pudo conectar")
                }
            }
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

    fun dismissStatusMessage() {
        _statusMessage.value = null
    }
}

// Data holders for combining flows
private data class Tuple5<A, B, C, D, E>(val a: A, val b: B, val c: C, val d: D, val e: E)
