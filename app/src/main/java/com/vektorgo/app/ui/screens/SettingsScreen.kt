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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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
import androidx.compose.material.icons.filled.Sync
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
    onGenerateCsr: (cuit: Long, razonSocial: String) -> Unit,
    onSaveArcaCertificate: (certPem: String) -> Unit,
    onOpenMpOAuth: () -> Unit,
    onOpenMpManual: () -> Unit,
    onCloseMpOAuth: () -> Unit,
    onConnectMp: (accessToken: String) -> Unit,
    onSyncMpMovements: () -> Unit,
    onDisconnectMp: () -> Unit,
    onDisconnectArca: () -> Unit,
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
    var certPem by remember(state.config) { mutableStateOf(state.config.certCrtPem) }
    var keyPem by remember(state.config) { mutableStateOf(state.config.privateKeyPem) }
    var defaultConcepto by remember(state.config) { mutableStateOf(state.config.defaultConcepto) }

    var showManualArcaKeys by remember { mutableStateOf(false) }
    var showDelegationGuide by remember { mutableStateOf(false) }
    var showMpManualToken by remember { mutableStateOf(false) }

    // Dialogs
    if (state.csr.showDialog) {
        AutoProvisioningDialog(
            csr = state.csr,
            onDismiss = onCloseAutoProvisioning,
            onGenerateCsr = onGenerateCsr,
            onSaveCertificate = onSaveArcaCertificate
        )
    }

    if (state.mpConnect.showDialog) {
        MercadoPagoOAuthDialog(
            isConnecting = state.mpConnect.isConnecting,
            error = state.mpConnect.error,
            onDismiss = onCloseMpOAuth,
            onConnect = onConnectMp
        )
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

                // Always-visible 2-step guide for merchants who haven't set
                // up ARCA yet. Before this, the app only explained the
                // certificate — the Punto de Venta step (a separate, required,
                // one-time setup on ARCA's own site) was never mentioned in
                // the guided flow, only shown after the fact as a summary
                // line once already connected. That silence is exactly what
                // produces "El punto de venta no se encuentra habilitado a
                // usar en el presente WS" from ARCA later on.
                if (!isArcaActive) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ArcaBlue.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                            .border(1.dp, ArcaBlue.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Antes de facturar, hacé esto UNA sola vez en ARCA:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        DelegationStepRow(
                            1,
                            "Dar de alta tu Punto de Venta: entrá a arca.gob.ar con tu Clave Fiscal → " +
                                "\"Puntos de Venta y Domicilios\" → Nuevo → Sistema: \"WSFE - Web Services\". " +
                                "No elijas \"Facturador Móvil\" ni \"Controlador Fiscal\", ARCA rechaza las facturas si el sistema no es este. " +
                                "Anotá el número que te asigna y cargalo en \"Punto Venta\", en Parámetros Fiscales más abajo."
                        )
                        DelegationStepRow(
                            2,
                            "Generar tu Certificado Digital: tocá el botón de abajo. La app arma la clave " +
                                "y el pedido de certificado (CSR); vos subís ese archivo a ARCA con tu Clave Fiscal y " +
                                "pegás acá el certificado que te entregan."
                        )
                    }
                }

                // Option B: Automated Certificate Generation Button (Prominent)
                Button(
                    onClick = onOpenAutoProvisioning,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (state.config.isArcaConnected) "Generar Nuevo Certificado (CSR)" else "Generar Certificado Digital (CSR)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
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
                                text = "Vinculación por Access Token",
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
                                text = "Access Token verificado contra la API de Mercado Pago.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 1-Click OAuth Button — opens Mercado Pago's own consent screen
                Button(
                    onClick = onOpenMpOAuth,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MpBlue)
                ) {
                    Icon(imageVector = Icons.Default.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (state.config.isMpConnected) "Reconectar con Mercado Pago" else "Conectar con Mercado Pago",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                if (state.config.isMpConnected) {
                    OutlinedButton(
                        onClick = onSyncMpMovements,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Sincronizar Movimientos (pagos y transferencias)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Manual fallback (accountants / troubleshooting)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showMpManualToken = !showMpManualToken }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Modo Manual (pegar Access Token)",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = if (showMpManualToken) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                AnimatedVisibility(visible = showMpManualToken) {
                    OutlinedButton(
                        onClick = onOpenMpManual,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Pegar Access Token manualmente", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
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

        // Demo Data & Maintenance Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Mantenimiento y Simulación",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onSeedDemo,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Cargar Demo", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = onClearAll,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                    ) {
                        Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Limpiar BD", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
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
