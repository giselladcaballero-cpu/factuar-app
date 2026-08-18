package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.ArcaBlue
import com.example.ui.theme.MpGreen
import com.example.ui.viewmodel.CsrUiState

/**
 * Guides the user through obtaining a real ARCA certificate:
 * 1) The app generates an RSA key pair + PKCS#10 CSR locally.
 * 2) The user uploads that CSR themselves at afip.gob.ar (their own browser,
 *    their own Clave Fiscal — this app never asks for it) and downloads the
 *    certificate ARCA issues.
 * 3) The user pastes that certificate back here to finish the link.
 */
@Composable
fun AutoProvisioningDialog(
    csr: CsrUiState,
    onDismiss: () -> Unit,
    onGenerateCsr: (cuit: Long, razonSocial: String) -> Unit,
    onSaveCertificate: (certPem: String) -> Unit
) {
    var cuitInput by remember { mutableStateOf(if (csr.cuit > 0) csr.cuit.toString() else "") }
    var razonSocialInput by remember { mutableStateOf(csr.razonSocial) }
    var certInput by remember { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current

    Dialog(onDismissRequest = {
        if (!csr.isGenerating) onDismiss()
    }) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
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
                                text = "Certificado Digital ARCA",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Generación de CSR + carga manual del certificado",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (!csr.isGenerating) {
                        IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar")
                        }
                    }
                }

                when {
                    csr.isGenerating -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                            Text(
                                text = "Paso ${csr.stepNumber} de ${csr.totalSteps}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            LinearProgressIndicator(
                                progress = { (csr.stepNumber.toFloat() / csr.totalSteps.toFloat()).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().height(6.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Text(text = csr.stepTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                text = csr.stepDesc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    csr.isSaved -> {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(56.dp).background(MpGreen.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = MpGreen, modifier = Modifier.size(36.dp))
                            }
                            Text(
                                text = "¡Certificado Vinculado!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "El certificado y la clave privada quedaron guardados en este dispositivo. Ya podés probar la conexión WSAA.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = onDismiss,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MpGreen)
                            ) {
                                Text("Listo", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    csr.isReady -> {
                        // Homologación shortcut: ARCA's testing WSAA accepts a
                        // self-signed certificate, no trip to the ARCA portal needed.
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MpGreen.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                .border(1.dp, MpGreen.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "¿Solo vas a probar en Homologación?",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "ARCA Homologación acepta un certificado autofirmado para testing — no hace falta pasar por el portal de ARCA.",
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(
                                    onClick = { onSaveCertificate(csr.selfSignedCertPem) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MpGreen)
                                ) {
                                    Text("Usar Certificado Autofirmado (Homologación)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }

                        Text(
                            text = "O, para Producción: 1. Copiá este CSR y subilo en ARCA",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 120.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = csr.csrPem,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = { clipboard.setText(AnnotatedString(csr.csrPem)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Copiar CSR", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(ArcaBlue.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                                .border(1.dp, ArcaBlue.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, tint = ArcaBlue, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Pasos en afip.gob.ar (con tu Clave Fiscal, en tu navegador):", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            DialogStepRow(1, "Entrá a 'Administración de Certificados Digitales'.")
                            DialogStepRow(2, "Elegí 'Agregar Alias' y pegá el CSR de arriba.")
                            DialogStepRow(3, "Descargá el certificado .crt que ARCA te entrega.")
                            DialogStepRow(4, "En 'Administrador de Relaciones', asociá ese alias al servicio 'WSFE'.")
                            DialogStepRow(5, "Abrí el .crt, copiá su contenido y pegalo abajo.")
                        }

                        Text(
                            text = "2. Pegá acá el certificado que te dio ARCA",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        OutlinedTextField(
                            value = certInput,
                            onValueChange = { certInput = it },
                            placeholder = { Text("-----BEGIN CERTIFICATE----- ...") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 4,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Button(
                            onClick = { onSaveCertificate(certInput.trim()) },
                            enabled = certInput.contains("BEGIN CERTIFICATE"),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Guardar Certificado", fontWeight = FontWeight.Bold)
                        }
                    }

                    else -> {
                        // Step 1: collect CUIT + razón social, generate the CSR.
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = ArcaBlue, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "La app genera la clave privada en este teléfono y arma la Solicitud de Certificado (CSR). Vos la subís en ARCA con tu propia Clave Fiscal — la app nunca te la pide.",
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        OutlinedTextField(
                            value = cuitInput,
                            onValueChange = { cuitInput = it.filter { c -> c.isDigit() } },
                            label = { Text("CUIT del Contribuyente (11 dígitos)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                focusedBorderColor = MaterialTheme.colorScheme.primary
                            )
                        )

                        OutlinedTextField(
                            value = razonSocialInput,
                            onValueChange = { razonSocialInput = it },
                            label = { Text("Razón Social o Nombre Fantasía") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                focusedBorderColor = MaterialTheme.colorScheme.primary
                            )
                        )

                        if (csr.error != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(10.dp))
                                    .padding(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = csr.error, color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 12.sp)
                                }
                            }
                        }

                        Button(
                            onClick = {
                                val cuitNum = cuitInput.toLongOrNull() ?: 0L
                                onGenerateCsr(cuitNum, razonSocialInput)
                            },
                            enabled = cuitInput.length == 11,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Generar CSR", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogStepRow(stepNumber: Int, instruction: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(18.dp)
                .background(ArcaBlue, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = stepNumber.toString(), color = androidx.compose.ui.graphics.Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = instruction, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}
