package com.vektorgo.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vektorgo.app.ui.screens.ArchitectureGuideScreen
import com.vektorgo.app.ui.screens.AuditLogsScreen
import com.vektorgo.app.ui.screens.DashboardScreen
import com.vektorgo.app.ui.screens.InvoiceDetailScreen
import com.vektorgo.app.ui.screens.OnboardingScreen
import com.vektorgo.app.ui.screens.SettingsScreen
import com.vektorgo.app.ui.screens.SubscriptionPaywallScreen
import com.vektorgo.app.ui.screens.TransactionsScreen
import com.vektorgo.app.ui.theme.ArcaBlue
import com.vektorgo.app.ui.theme.MpBlue
import com.vektorgo.app.ui.theme.MpGreen
import com.vektorgo.app.ui.theme.VektorGoTheme
import com.vektorgo.app.ui.viewmodel.BillingViewModel

/**
 * Base URL of the Vektor Go Mercado Pago OAuth bridge (a small serverless
 * function that holds the Client Secret so it never lives on-device).
 * Supabase Edge Functions now, replacing the Vercel mp-oauth-service (still
 * in the repo under mp-oauth-service/, kept as a fallback reference) to stay
 * at $0/month as the subscriber base grows. See supabase/README.md.
 */
private const val MP_OAUTH_SERVICE_URL = "https://aaalabfxyrdpunhcanbu.supabase.co/functions/v1"

class MainActivity : ComponentActivity() {
    private val viewModel: BillingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleMpOAuthIntent(intent)
        // KEEP: safe to call on every launch, never resets or duplicates the
        // already-scheduled periodic job.
        com.vektorgo.app.work.MpSyncWorker.schedule(applicationContext)
        val lastCrash = VektorGoApplication.getLastCrash(applicationContext)
        setContent {
            VektorGoTheme {
                if (lastCrash != null) {
                    CrashReportScreen(
                        crashText = lastCrash,
                        onDismiss = { VektorGoApplication.clearLastCrash(applicationContext); recreate() }
                    )
                    return@VektorGoTheme
                }
                var showSplash by remember { mutableStateOf(true) }
                if (showSplash) {
                    com.vektorgo.app.ui.screens.VektorSplashScreen(onFinished = { showSplash = false })
                } else {
                    BillingApp(
                        viewModel = viewModel,
                        onOpenMpOAuth = { openMercadoPagoOAuth() },
                        onOpenSubscriptionCheckout = { openSubscriptionCheckout() }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleMpOAuthIntent(intent)
    }

    /**
     * Opens Mercado Pago's real consent screen in the browser. The user
     * approves with their own account there; Mercado Pago redirects to our
     * Supabase bridge, which exchanges the code for a token and bounces back
     * to this app via the factuar://mp-connected deep link.
     */
    private fun openMercadoPagoOAuth() {
        val uri = Uri.parse("$MP_OAUTH_SERVICE_URL/mp-authorize")
        startActivity(Intent(Intent.ACTION_VIEW, uri))
    }

    /**
     * Opens Vektor Go's own subscription checkout. This charges INTO
     * Vektor Go's Mercado Pago account (not the merchant's) — see
     * create-subscription in supabase/functions/ and its notes on the
     * separate business account this requires.
     */
    private fun openSubscriptionCheckout() {
        val uri = Uri.parse("$MP_OAUTH_SERVICE_URL/create-subscription")
        startActivity(Intent(Intent.ACTION_VIEW, uri))
    }

    private fun handleMpOAuthIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme != "factuar") return

        when (uri.host) {
            "mp-connected" -> {
                val accessToken = uri.getQueryParameter("access_token")
                if (accessToken.isNullOrBlank()) return
                val userId = uri.getQueryParameter("user_id")?.toLongOrNull() ?: 0L
                val name = uri.getQueryParameter("name").orEmpty()
                viewModel.completeMercadoPagoOAuth(accessToken, userId, name)
            }
            "subscription-activated" -> {
                val preapprovalId = uri.getQueryParameter("preapproval_id").orEmpty()
                val status = uri.getQueryParameter("status").orEmpty()
                viewModel.completeSubscriptionActivation(preapprovalId, status)
            }
        }
    }
}

data class NavItem(
    val title: String,
    val icon: ImageVector,
    val tabIndex: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillingApp(
    viewModel: BillingViewModel = viewModel(),
    onOpenMpOAuth: () -> Unit = {},
    onOpenSubscriptionCheckout: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.statusMessage) {
        state.statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissStatusMessage()
        }
    }

    // Bypassed in debug builds on purpose: the Vektor Go business Mercado
    // Pago account + subscription plan don't exist yet, so
    // create-subscription can't succeed — gating debug builds too would
    // make every other feature untestable in the meantime. The gate is
    // real in release builds (BuildConfig.DEBUG is false there).
    if (!com.vektorgo.app.BuildConfig.DEBUG && !state.config.isSubscribed) {
        SubscriptionPaywallScreen(onSubscribe = onOpenSubscriptionCheckout)
        return
    }

    // Mandatory single screen for both required one-time setups (ARCA +
    // Mercado Pago) instead of sending a brand new merchant into Ajustes to
    // find the right cards among unrelated sections. Once both are done,
    // this never shows again — state.config flips and the tabs below render
    // normally on the next recomposition.
    val isArcaActive = state.config.isArcaConnected &&
        (state.config.certCrtPem.isNotBlank() || state.config.cuitEmisor > 0)
    if (!isArcaActive || !state.config.isMpConnected) {
        // Needs its own SnackbarHost: this branch returns before reaching the
        // Scaffold below, so without this the WSAA/Punto de Venta
        // verification result (and any other status message) would be
        // enqueued on a SnackbarHostState that's never actually composed on
        // screen -- silently swallowed right when it matters most.
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                OnboardingScreen(
                    state = state,
                    onSaveConfig = { viewModel.saveConfig(it) },
                    onOpenAutoProvisioning = { viewModel.openCsrDialog() },
                    onCloseAutoProvisioning = { viewModel.closeCsrDialog() },
                    onGenerateCsr = { cuit, razon -> viewModel.startCsrGeneration(cuit, razon) },
                    onSaveArcaCertificate = { certPem -> viewModel.saveArcaCertificate(certPem) },
                    onOpenMpOAuth = onOpenMpOAuth,
                    onCloseMpOAuth = { viewModel.closeMpTokenDialog() },
                    onConnectMp = { accessToken -> viewModel.connectMercadoPago(accessToken) }
                )
            }
        }
        return
    }

    val navItems = listOf(
        NavItem("Dashboard", Icons.Default.Dashboard, 0),
        NavItem("Ventas", Icons.Default.ReceiptLong, 1),
        NavItem("Factura", Icons.Default.Receipt, 2),
        NavItem("Auditoría", Icons.Default.History, 5),
        NavItem("Arquitectura", Icons.Default.AccountTree, 4),
        NavItem("Ajustes", Icons.Default.Settings, 3)
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            com.vektorgo.app.ui.components.AnimatedVektorMark(
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Facturador ARCA",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Mercado Pago Auto-Billing",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    actions = {
                        // Service Status Indicators
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(20.dp)
                                )
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(MpGreen, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "WSFE v1",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                if (state.isProcessing) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                modifier = Modifier.border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                )
            ) {
                navItems.forEach { item ->
                    val isSelected = state.activeTab == item.tabIndex
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { viewModel.selectTab(item.tabIndex) },
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = {
                            Text(
                                text = item.title,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (state.activeTab) {
                0 -> DashboardScreen(
                    state = state,
                    onToggleAutoInvoice = { viewModel.toggleAutoInvoice(it) },
                    onSimulatePayment = { viewModel.simulatePayment(it) },
                    onRetryPayment = { viewModel.retryInvoice(it) },
                    onSelectInvoice = { viewModel.selectInvoice(it) },
                    onSelectInvoiceByPaymentId = { viewModel.selectInvoiceByPaymentId(it) },
                    onNavigateToTransactions = { viewModel.selectTab(1) },
                    onNavigateToSettings = { viewModel.selectTab(3) }
                )

                1 -> TransactionsScreen(
                    state = state,
                    onSearchChange = { viewModel.setSearchQuery(it) },
                    onFilterChange = { viewModel.setFilterStatus(it) },
                    onSelectInvoice = { viewModel.selectInvoice(it) },
                    onRetryPayment = { viewModel.retryInvoice(it) },
                    onRetryAllPending = { viewModel.retryAllPending() }
                )

                2 -> InvoiceDetailScreen(
                    state = state,
                    onBack = { viewModel.selectTab(1) },
                    onEmitCreditNote = { viewModel.emitCreditNote(it) }
                )

                3 -> SettingsScreen(
                    state = state,
                    onSaveConfig = { viewModel.saveConfig(it) },
                    onTestWsaa = { viewModel.testWsaaConnection() },
                    onSeedDemo = { viewModel.seedDemoData() },
                    onClearAll = { viewModel.clearAllData() },
                    onOpenAutoProvisioning = { viewModel.openCsrDialog() },
                    onCloseAutoProvisioning = { viewModel.closeCsrDialog() },
                    onGenerateCsr = { cuit, razon ->
                        viewModel.startCsrGeneration(cuit, razon)
                    },
                    onSaveArcaCertificate = { certPem ->
                        viewModel.saveArcaCertificate(certPem)
                    },
                    onOpenMpOAuth = onOpenMpOAuth,
                    onOpenMpManual = { viewModel.openMpTokenDialog() },
                    onCloseMpOAuth = { viewModel.closeMpTokenDialog() },
                    onConnectMp = { accessToken ->
                        viewModel.connectMercadoPago(accessToken)
                    },
                    onSyncMpMovements = { viewModel.syncMercadoPagoMovements() },
                    onDisconnectMp = { viewModel.disconnectMercadoPago() },
                    onDisconnectArca = { viewModel.disconnectArca() }
                )

                4 -> ArchitectureGuideScreen()

                5 -> AuditLogsScreen(
                    state = state
                )

                else -> DashboardScreen(
                    state = state,
                    onToggleAutoInvoice = { viewModel.toggleAutoInvoice(it) },
                    onSimulatePayment = { viewModel.simulatePayment(it) },
                    onRetryPayment = { viewModel.retryInvoice(it) },
                    onSelectInvoice = { viewModel.selectInvoice(it) },
                    onSelectInvoiceByPaymentId = { viewModel.selectInvoiceByPaymentId(it) },
                    onNavigateToTransactions = { viewModel.selectTab(1) },
                    onNavigateToSettings = { viewModel.selectTab(3) }
                )
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}

/**
 * Shown once, right after a crash, instead of the app just closing with
 * nothing to go on. Lets the crash text be copied so it can be sent for
 * diagnosis, then clears it and restarts normally.
 */
@Composable
fun CrashReportScreen(crashText: String, onDismiss: () -> Unit) {
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "La app se cerró inesperadamente",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Copiá este texto y mandalo para poder arreglarlo.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            androidx.compose.foundation.text.selection.SelectionContainer {
                Column(modifier = Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())) {
                    Text(
                        text = crashText,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        androidx.compose.material3.OutlinedButton(
            onClick = { clipboard.setText(androidx.compose.ui.text.AnnotatedString(crashText)) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Copiar error")
        }
        androidx.compose.material3.Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Continuar")
        }
    }
}
