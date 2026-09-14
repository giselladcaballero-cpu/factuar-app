package com.vektorgo.app.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vektorgo.app.ui.components.AnimatedVektorMark
import com.vektorgo.app.ui.components.AutoProvisioningDialog
import com.vektorgo.app.ui.components.MercadoPagoOAuthDialog
import com.vektorgo.app.ui.theme.ArcaBlue
import com.vektorgo.app.ui.theme.MpBlue
import com.vektorgo.app.ui.theme.MpGreen
import com.vektorgo.app.ui.viewmodel.BillingUiState

/**
 * Mandatory single screen shown instead of the Dashboard/tabs until BOTH
 * ARCA and Mercado Pago are set up. Before this, a new merchant had to know
 * to go into Ajustes and find the right cards among several unrelated
 * sections (fiscal parameters, webhook relay, subscription...) — this
 * screen has nothing else on it, just what's actually required to start
 * billing.
 */
@Composable
fun OnboardingScreen(
    state: BillingUiState,
    onSaveConfig: (com.vektorgo.app.data.local.entity.ArcaConfigEntity) -> Unit,
    onOpenAutoProvisioning: () -> Unit,
    onCloseAutoProvisioning: () -> Unit,
    onGenerateCsr: (cuit: Long, razonSocial: String) -> Unit,
    onSaveArcaCertificate: (certPem: String) -> Unit,
    onOpenMpOAuth: () -> Unit,
    onCloseMpOAuth: () -> Unit,
    onConnectMp: (accessToken: String) -> Unit
) {
    var cuitText by remember(state.config) {
        mutableStateOf(if (state.config.cuitEmisor > 0) state.config.cuitEmisor.toString() else "")
    }
    var razonSocial by remember(state.config) { mutableStateOf(state.config.razonSocial) }
    var puntoVentaText by remember(state.config) {
        mutableStateOf(if (state.config.puntoVenta > 0) state.config.puntoVenta.toString() else "")
    }

    val isArcaActive = state.config.isArcaConnected &&
        (state.config.certCrtPem.isNotBlank() || state.config.cuitEmisor > 0)
    val isMpActive = state.config.isMpConnected

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
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AnimatedVektorMark(modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(text = "Antes de empezar", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    text = "Dos pasos, una sola vez. Después vas directo al Dashboard.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StepChip("1. ARCA", isArcaActive, ArcaBlue)
            StepChip("2. Mercado Pago", isMpActive, MpBlue)
        }

        // ---- Step 1: ARCA ----
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "1. Facturación electrónica (ARCA)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    if (isArcaActive) Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MpGreen, modifier = Modifier.size(18.dp))
                }

                if (!isArcaActive) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ArcaBlue.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                            .border(1.dp, ArcaBlue.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OnboardingStepRow(
                            1,
                            "Dar de alta tu Punto de Venta: arca.gob.ar con tu Clave Fiscal → " +
                                "\"Puntos de Venta y Domicilios\" → Nuevo → Sistema: \"WSFE - Web Services\" " +
                                "(no \"Facturador Móvil\" ni \"Controlador Fiscal\"). Anotá el número y cargalo abajo."
                        )
                        OnboardingStepRow(
                            2,
                            "Generar tu Certificado Digital con el botón de abajo, subirlo a ARCA y pegar acá lo que te entreguen."
                        )
                    }

                    OutlinedTextField(
                        value = cuitText,
                        onValueChange = { cuitText = it.filter { c -> c.isDigit() } },
                        label = { Text("CUIT (11 dígitos)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = razonSocial,
                        onValueChange = { razonSocial = it },
                        label = { Text("Razón Social") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = puntoVentaText,
                        onValueChange = { puntoVentaText = it.filter { c -> c.isDigit() } },
                        label = { Text("Punto de Venta (el número que te dio ARCA)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            onSaveConfig(
                                state.config.copy(
                                    cuitEmisor = cuitText.toLongOrNull() ?: 0L,
                                    razonSocial = razonSocial,
                                    puntoVenta = puntoVentaText.toIntOrNull() ?: 1
                                )
                            )
                            onOpenAutoProvisioning()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ArcaBlue),
                        enabled = cuitText.length == 11 && razonSocial.isNotBlank() && puntoVentaText.isNotBlank()
                    ) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Guardar y generar Certificado", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                } else {
                    Text(
                        text = "CUIT ${state.config.cuitEmisor} (${state.config.razonSocial}) — Punto de Venta #${state.config.puntoVenta}. Listo.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // ---- Step 2: Mercado Pago ----
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "2. Cobros (Mercado Pago)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    if (isMpActive) Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MpGreen, modifier = Modifier.size(18.dp))
                }

                if (!isMpActive) {
                    Text(
                        text = "Vinculá tu cuenta de Mercado Pago para que la app facture automáticamente cada cobro por QR o Point.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = onOpenMpOAuth,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MpBlue)
                    ) {
                        Icon(imageVector = Icons.Default.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Conectar con Mercado Pago", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                } else {
                    Text(
                        text = "Cuenta vinculada: ${state.config.mpUserName}. Listo.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun StepChip(label: String, done: Boolean, color: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .background(if (done) color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = (if (done) "✓ " else "") + label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (done) color else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun OnboardingStepRow(stepNumber: Int, instruction: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .height(18.dp)
                .width(18.dp)
                .background(ArcaBlue, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = stepNumber.toString(), color = androidx.compose.ui.graphics.Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = instruction, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}
