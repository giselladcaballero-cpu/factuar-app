package com.vektorgo.app.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vektorgo.app.data.local.entity.ArcaConfigEntity
import com.vektorgo.app.ui.components.AutoProvisioningDialog
import com.vektorgo.app.ui.components.MercadoPagoOAuthDialog
import com.vektorgo.app.ui.theme.ArcaBlue
import com.vektorgo.app.ui.theme.MpBlue
import com.vektorgo.app.ui.theme.MpGreen
import com.vektorgo.app.ui.viewmodel.BillingUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: BillingUiState,
    onSaveConfig: (ArcaConfigEntity) -> Unit,
    onTestWsaa: () -> Unit,
    onSeedDemo: () -> Unit,
    onClearAll: () -> Unit,
    onOpenAutoProvisioning: () -> Unit,
    onCloseAutoProvisioning: () -> Unit,
    onStartAutoProvisioning: (cuit: Long, claveFiscal: String, razonSocial: String, otpToken: String) -> Unit,
    onOpenMpOAuth: () -> Unit,
    onCloseMpOAuth: () -> Unit,
    onAuthorizeMpOAuth: (userName: String, collectorId: Long, token: String) -> Unit,
    onDisconnectMp: () -> Unit,
    onDisconnectArca: () -> Unit,
    onToggleAutoInvoice: (Boolean) -> Unit = {},
    onToggleSubscription: (Boolean) -> Unit = {},
    onTestWebhookRelay: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var cuitText by remember(state.config) { mutableStateOf(state.config.cuitEmisor.toString()) }
    var razonSocial by remember(state.config) { mutableStateOf(state.config.razonSocial) }
    var domicilioFiscal by remember(state.config) { mutableStateOf(state.config.domicilioFiscal) }
    var puntoVentaText by remember(state.config) { mutableStateOf(state.config.puntoVenta.toString()) }
    var environment by remember(state.config) { mutableStateOf(state.config.environment) }
    var condicionIva by remember(state.config) { mutableStateOf(state.config.condicionIvaEmisor) }
    var mpAccessToken by remember(state.config) { mutableStateOf(state.config.mpAccessToken) }
    var mpWebhookSecret by remember(state.config) { mutableStateOf(state.config.mpWebhookSecret) }
    var webhookRelayUrl by remember(state.config) { mutableStateOf(state.config.webhookRelayUrl) }
    var certPem by remember(state.config) { mutableStateOf(state.config.certCrtPem) }
    var keyPem by remember(state.config) { mutableStateOf(state.config.privateKeyPem) }
    var defaultConcepto by remember(state.config) { mutableStateOf(state.config.defaultConcepto) }

    var showManualArcaKeys by remember { mutableStateOf(false) }
    var showDelegationGuide by remember { mutableStateOf(false) }

    // Dialogs
    if (state.showAutoProvisionDialog) {
        AutoProvisioningDialog(
            state = state,
            onDismiss = onCloseAutoProvisioning,
            onStartProvisioning = onStartAutoProvisioning
        )
    }

    if (state.showMpOAuthDialog) {
        MercadoPagoOAuthDialog(
            cuit = state.config.cuitEmisor,
            relayBaseUrl = state.config.webhookRelayUrl.substringBeforeLast("/"),
            onDismiss = onCloseMpOAuth,
            onAuthorize = onAuthorizeMpOAuth
        )
    }

    var showArchitectureView by remember { mutableStateOf(false) }

    if (showArchitectureView) {
        ArchitectureGuideScreen(
            onBack = { showArchitectureView = false },
            modifier = modifier
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // HEADER: Onboarding Hub Title
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Centro de Vinculación y Ajustes",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Flujos guiados sin fricción con ARCA y Mercado Pago",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // CARD 0: Facturación Automática Master Switch & Operational Status
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (state.config.autoInvoiceEnabled)
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                else
                    MaterialTheme.colorScheme.surfaceVariant
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (state.config.autoInvoiceEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(
                                if (state.config.autoInvoiceEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlashOn,
                            contentDescription = null,
                            tint = if (state.config.autoInvoiceEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Facturación Automática",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (state.config.autoInvoiceEnabled)
                                "Mercado Pago ➔ CAE ARCA instantáneo"
                            else
                                "Pausada: Los pagos ingresan a cola sin facturar",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = state.config.autoInvoiceEnabled,
                    onCheckedChange = onToggleAutoInvoice,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                )
            }
        }

        // CARD 1: ARCA / AFIP Connection (Option B Automation + Option A Delegation)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ARCA (ex AFIP) WSFE v1",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Certificado Digital X.509 y Clave RSA",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Connection status pill
                    val isArcaActive = state.config.isArcaConnected && (state.config.certCrtPem.isNotBlank() || state.config.cuitEmisor > 0)
                    Box(
                        modifier = Modifier
                            .background(
                                if (isArcaActive) MpGreen.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(20.dp)
                            )
                            .border(
                                1.dp,
                                if (isArcaActive) MpGreen.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outlineVariant,
                                RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(if (isArcaActive) MpGreen else Color.Gray, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isArcaActive) "Vinculado" else "No Conectado",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isArcaActive) MpGreen else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Summary info when connected
                if (state.config.isArcaConnected) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "CUIT Emisor: ${state.config.cuitEmisor} (${state.config.razonSocial})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Alias: ${state.config.arcaAlias} | Punto de Venta: #${state.config.puntoVenta} (Web Services)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Entorno: ${state.config.environment} | Token WSAA: ${state.wsaaStatus}",
                                fontSize = 11.sp,
                                color = ArcaBlue,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Automated Certificate Generation Button (Prominent - Primary Choice for clients)
                Button(
                    onClick = onOpenAutoProvisioning,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (state.config.isArcaConnected) "Re-vincular Automáticamente con AFIP" else "⚡ Vincular AFIP Automático (Solo CUIT y Clave)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                // Option A: Delegation Guide (Alternative Accordion)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDelegationGuide = !showDelegationGuide }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.HelpOutline, contentDescription = null, tint = ArcaBlue, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Opción A: Instructivo Delegación en AFIP (3 clics)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ArcaBlue
                        )
                    }
                    Icon(
                        imageVector = if (showDelegationGuide) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                AnimatedVisibility(visible = showDelegationGuide) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ArcaBlue.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                            .border(1.dp, ArcaBlue.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Pasos para delegar el servicio en AFIP sin tocar certificados:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        DelegationStepRow(1, "Ingresá a afip.gob.ar con Clave Fiscal y abrí 'Administrador de Relaciones'.")
                        DelegationStepRow(2, "Seleccioná 'Nueva Relación' -> 'ARCA' -> 'Web Services' -> 'Facturación Electrónica'.")
                        DelegationStepRow(3, "Pegá el CUIT de la App (30-71829384-9) como Representante y confirmá.")
                    }
                }

                // Option C: Manual CRT / KEY (For Advanced users / Accountants)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showManualArcaKeys = !showManualArcaKeys }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Modo Manual / Contadores (.crt y .key PEM)",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = if (showManualArcaKeys) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                AnimatedVisibility(visible = showManualArcaKeys) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = certPem,
                            onValueChange = { certPem = it },
                            label = { Text("Certificado Digital X.509 (.crt PEM)") },
                            placeholder = { Text("-----BEGIN CERTIFICATE----- ...") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                focusedBorderColor = MaterialTheme.colorScheme.primary
                            )
                        )

                        OutlinedTextField(
                            value = keyPem,
                            onValueChange = { keyPem = it },
                            label = { Text("Clave Privada RSA (.key PEM)") },
                            placeholder = { Text("-----BEGIN PRIVATE KEY----- ...") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                focusedBorderColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                // WSAA Test Button
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilledTonalButton(
                        onClick = onTestWsaa,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Probar WSAA", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    if (state.config.isArcaConnected) {
                        OutlinedButton(
                            onClick = onDisconnectArca,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                        ) {
                            Icon(imageVector = Icons.Default.LinkOff, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Desvincular", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // CARD 2: Mercado Pago (OAuth 2.0 Connection)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(MpBlue.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Payment,
                                contentDescription = null,
                                tint = MpBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Mercado Pago",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Vinculación OAuth 2.0 & Webhooks",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // MP status pill
                    val isMpActive = state.config.isMpConnected && state.config.mpAccessToken.isNotBlank()
                    Box(
                        modifier = Modifier
                            .background(
                                if (isMpActive) MpBlue.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(20.dp)
                            )
                            .border(
                                1.dp,
                                if (isMpActive) MpBlue.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outlineVariant,
                                RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(if (isMpActive) MpBlue else Color.Gray, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isMpActive) "Conectado" else "Sin Conexión",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isMpActive) MpBlue else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (state.config.isMpConnected) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MpBlue.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                            .border(1.dp, MpBlue.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Cuenta vinculada: ${state.config.mpUserName} (ID: ${state.config.mpCollectorId})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MpBlue
                            )
                            Text(
                                text = "Escucha de webhooks activa en tiempo real. Token OAuth renovado.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 1-Click OAuth Button
                Button(
                    onClick = onOpenMpOAuth,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MpBlue)
                ) {
                    Icon(imageVector = Icons.Default.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (state.config.isMpConnected) "Re-autorizar con Mercado Pago" else "Conectar con Mercado Pago (OAuth 2.0)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                if (state.config.isMpConnected) {
                    OutlinedButton(
                        onClick = onDisconnectMp,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                    ) {
                        Icon(imageVector = Icons.Default.LinkOff, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Desvincular Cuenta de Mercado Pago", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // CARD 3: Fiscal Parameters & Business Info
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Parámetros Fiscales del Emisor",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Environment selector (Homologación vs Producción)
                Text(text = "Entorno Web Services ARCA:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = environment == "HOMOLOGACION",
                        onClick = { environment = "HOMOLOGACION" },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text("Homologación (Test)")
                    }
                    SegmentedButton(
                        selected = environment == "PRODUCCION",
                        onClick = { environment = "PRODUCCION" },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text("Producción (Fiscal)")
                    }
                }

                OutlinedTextField(
                    value = cuitText,
                    onValueChange = { cuitText = it.filter { c -> c.isDigit() } },
                    label = { Text("CUIT del Emisor (11 dígitos)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )

                OutlinedTextField(
                    value = razonSocial,
                    onValueChange = { razonSocial = it },
                    label = { Text("Razón Social / Nombre Fantasía") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = puntoVentaText,
                        onValueChange = { puntoVentaText = it.filter { c -> c.isDigit() } },
                        label = { Text("Punto Venta") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    OutlinedTextField(
                        value = domicilioFiscal,
                        onValueChange = { domicilioFiscal = it },
                        label = { Text("Domicilio Fiscal") },
                        modifier = Modifier.weight(2f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                // Condición IVA
                Text(text = "Condición IVA Emisor:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = condicionIva == "RESPONSABLE_INSCRIPTO",
                        onClick = { condicionIva = "RESPONSABLE_INSCRIPTO" },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text("Resp. Inscripto (Fac A/B)", fontSize = 11.sp)
                    }
                    SegmentedButton(
                        selected = condicionIva == "MONOTRIBUTO",
                        onClick = { condicionIva = "MONOTRIBUTO" },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text("Monotributo (Fac C)", fontSize = 11.sp)
                    }
                }

                Text(
                    text = "Tipo de Concepto por defecto:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = defaultConcepto == 1,
                        onClick = { defaultConcepto = 1 },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                    ) {
                        Text("Productos", fontSize = 11.sp)
                    }
                    SegmentedButton(
                        selected = defaultConcepto == 2,
                        onClick = { defaultConcepto = 2 },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                    ) {
                        Text("Servicios", fontSize = 11.sp)
                    }
                    SegmentedButton(
                        selected = defaultConcepto == 3,
                        onClick = { defaultConcepto = 3 },
                        shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                    ) {
                        Text("Ambos", fontSize = 11.sp)
                    }
                }
            }
        }

        // CARD 5: Subscription & App Plan (Monthly Flat Rate)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(MpBlue.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Payment,
                                contentDescription = null,
                                tint = MpBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Abono Mensual de la App",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Facturación ilimitada sin límites ni comisiones",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .background(
                                if (state.config.isSubscribed) MpGreen.copy(alpha = 0.12f) else MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                                RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (state.config.isSubscribed) "Activo" else "Pausado",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (state.config.isSubscribed) MpGreen else MaterialTheme.colorScheme.error
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Plan contratado:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(state.config.subscriptionPlan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Tarifa mensual:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$14.999 ARS / mes", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ArcaBlue)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Medio de débito:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(if (state.config.subscriptionProvider == "MERCADO_PAGO") "Mercado Pago Débito Automático" else "Google Play", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val intent = android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse("https://www.mercadopago.com.ar/subscriptions/checkout?preapproval_plan_id=factuar_monthly")
                            )
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // Fallback
                                onToggleSubscription(true)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MpBlue),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Gestionar Suscripción MP", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { onToggleSubscription(!state.config.isSubscribed) },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = if (state.config.isSubscribed) "Pausar" else "Reactivar",
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // CARD 6: Webhook Relay & Crashlytics / Monitoring (Firebase Free Spark Tier)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(Color(0xFFFFA000).copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = Color(0xFFFFA000),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Webhook Relay & Crashlytics",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Firebase Cloud Functions (Plan Spark Gratuito - 2M req/mes)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .background(MpGreen.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "0$/mes",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MpGreen
                        )
                    }
                }

                OutlinedTextField(
                    value = webhookRelayUrl,
                    onValueChange = { webhookRelayUrl = it },
                    label = { Text("URL del Servidor Relay (Cloud Functions)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )

                OutlinedButton(
                    onClick = onTestWebhookRelay,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFA000))
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = Color(0xFFFFA000), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Probar Conexión Relay y Crashlytics", color = Color(0xFFFFA000), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Save Changes Button

        Button(
            onClick = {
                val newConfig = state.config.copy(
                    cuitEmisor = cuitText.toLongOrNull() ?: 20345678909L,
                    razonSocial = razonSocial,
                    domicilioFiscal = domicilioFiscal,
                    puntoVenta = puntoVentaText.toIntOrNull() ?: 1,
                    environment = environment,
                    condicionIvaEmisor = condicionIva,
                    mpAccessToken = mpAccessToken,
                    mpWebhookSecret = mpWebhookSecret,
                    webhookRelayUrl = webhookRelayUrl,
                    certCrtPem = certPem,
                    privateKeyPem = keyPem,
                    defaultConcepto = defaultConcepto
                )
                onSaveConfig(newConfig)
                Toast.makeText(context, "Ajustes guardados correctamente", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Guardar Parámetros Fiscales", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        // System Architecture & Technical Specifications Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountTree,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Arquitectura del Sistema & Flujos",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Diagramas, WSAA, WSFE v1 y Webhooks",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = "Consultá los esquemas técnicos de onboarding con Clave Fiscal (Opción B), flujo de homologación ARCA y vinculación OAuth 2.0 de Mercado Pago.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = { showArchitectureView = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountTree,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Ver Arquitectura y Flujos Técnicos",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Production Maintenance & Database Management Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Mantenimiento y Base de Datos Local",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onClearAll,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                    ) {
                        Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Limpiar Historial de Comprobantes", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Button(
                    onClick = {
                        val adminUrl = state.config.webhookRelayUrl.replace("/mpWebhookRelay", "/admin")
                        val intent = android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse(adminUrl)
                        )
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "No se pudo abrir el navegador", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(imageVector = Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Abrir Panel Web Admin (Suscripciones & Comercios)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DelegationStepRow(stepNumber: Int, instruction: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(18.dp)
                .background(ArcaBlue, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = stepNumber.toString(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = instruction, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}
