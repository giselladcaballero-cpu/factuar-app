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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vektorgo.app.ui.theme.ArcaBlue
import com.vektorgo.app.ui.theme.MpBlue
import com.vektorgo.app.ui.theme.MpGreen

@Composable
fun ArchitectureGuideScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // Header Title Card with Geometric Balance
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountTree,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Arquitectura de Experiencia y Viabilidad",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Especificación de Fricción Cero: Onboarding con Clave Fiscal (Opción B) + OAuth 2.0 Mercado Pago y Emisión ARCA WSFE v1.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                )
            }
        }

        // Section 0: Onboarding & Viabilidad Masiva (Option B vs Option A)
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
                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "0. Onboarding Sin Fricción para Público General",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "El 95% de los usuarios abandona ante configuraciones técnicas (copiar tokens de Developers o generar CSR/CRT con OpenSSL). La arquitectura implementa dos modelos de alta adopción:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                ArchitectureStep(
                    number = "1",
                    title = "Opción B: Generación Automática de Certificados (Recomendada)",
                    description = "El usuario ingresa CUIT + Clave Fiscal una única vez. La app genera la clave privada RSA 2048 en Android KeyStore y un worker en segundo plano tramita el certificado X.509 en AFIP en 15s. La Clave Fiscal es efímera en memoria volátil y nunca se guarda.",
                    icon = Icons.Default.Lock,
                    color = MaterialTheme.colorScheme.primary
                )

                ArchitectureStep(
                    number = "2",
                    title = "Mercado Pago: OAuth 2.0 en 1 Clic",
                    description = "Se reemplaza el campo manual de Access Token por el botón nativo 'Conectar con Mercado Pago'. La app abre el Custom Tab de autorización y recibe el access_token y collector_id por deep linking de forma 100% segura.",
                    icon = Icons.Default.Payment,
                    color = MpBlue
                )

                ArchitectureStep(
                    number = "3",
                    title = "Opción A: Delegación Guiada en Administrador de Relaciones",
                    description = "Alternativa donde el usuario autoriza al CUIT de la plataforma dentro del Administrador de Relaciones de AFIP con un tutorial visual de 3 clics.",
                    icon = Icons.Default.CheckCircle,
                    color = MpGreen
                )
            }
        }

        // Section 1: Webhook & Real-time Reception Strategy
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
                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "1. Estrategia de Recepción de Webhooks",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Mercado Pago envía notificaciones HTTPS POST (Webhooks/IPN) a una URL pública con IP fija o dominio con certificado SSL válido. Dado que un dispositivo móvil Android no posee una IP pública fija receptora, la arquitectura óptima en producción es:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                ArchitectureStep(
                    number = "A",
                    title = "Cloud Function / Microservicio Intermediario (Serverless Relay)",
                    description = "Una función serverless en Firebase Cloud Functions / Cloud Run recibe el webhook de Mercado Pago, verifica la firma HMAC en la cabecera 'x-signature', consulta /v1/payments/{id} y retransmite a la app mediante Firebase Cloud Messaging (FCM Data Message) o Firestore stream.",
                    icon = Icons.Default.CloudDone,
                    color = MpGreen
                )

                ArchitectureStep(
                    number = "B",
                    title = "WorkManager Polling & Fallback Autónomo (Client-Side)",
                    description = "Para funcionamiento 100% autónomo o sin backend servidor, la app ejecuta un PeriodicWorkRequest de WorkManager cada 15 minutos que consulta los pagos recientes (/v1/payments/search?status=approved) y procesa las transacciones pendientes sin duplicar facturas.",
                    icon = Icons.Default.Sync,
                    color = MpBlue
                )
            }
        }

        // Section 2: Security & HMAC-SHA256
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
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "2. Validación Criptográfica de Notificaciones",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Mercado Pago adjunta la cabecera 'x-signature: ts=[timestamp],v1=[hash]'. La app valida la autenticidad con HMAC-SHA256:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                CodeSnippetBox(
                    code = """
                        // Manifest firmado: id:[data.id];request-id:[x-request-id];ts:[ts];
                        val manifest = "id:${'$'}dataId;request-id:${'$'}requestId;ts:${'$'}ts;"
                        val mac = Mac.getInstance("HmacSHA256")
                        mac.init(SecretKeySpec(secretKey.toByteArray(), "HmacSHA256"))
                        val calculatedV1 = mac.doFinal(manifest.toByteArray()).toHex()
                    """.trimIndent()
                )
            }
        }

        // Section 3: ARCA WSAA & Ticket Caching
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
                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "3. Autenticación WSAA y Caché de Ticket (TA)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "ARCA prohíbe generar un Ticket de Acceso por cada factura individual (política de rate limit). El Ticket de Acceso (Token + Sign) se genera una sola vez, se almacena en Room Database y se reutiliza durante sus 12 horas de validez oficial.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                ArchitectureStep(
                    number = "1",
                    title = "Generación de TRA (Ticket de Requerimiento de Acceso)",
                    description = "Se construye el XML con <uniqueId>, <generationTime>, <expirationTime> y <service>wsfe</service>.",
                    icon = Icons.Default.Code,
                    color = ArcaBlue
                )

                ArchitectureStep(
                    number = "2",
                    title = "Firma Criptográfica CMS / PKCS#7",
                    description = "El XML se firma con el certificado X.509 (.crt) y la clave privada RSA (.key) del contribuyente y se envía a LoginCms.",
                    icon = Icons.Default.Security,
                    color = MaterialTheme.colorScheme.primary
                )

                ArchitectureStep(
                    number = "3",
                    title = "Extracción y Almacenamiento en Room",
                    description = "Se extraen <token> y <sign> y se persisten en la tabla 'auth_tickets' con expiración a 12 horas.",
                    icon = Icons.Default.Storage,
                    color = MpGreen
                )
            }
        }

        // Section 4: ARCA WSFE v1 Payload & QR
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
                            .background(MaterialTheme.colorScheme.tertiaryContainer, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "4. Emisión FECAESolicitar y Código QR RG 4892",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Al autorizarse el comprobante y obtener el CAE, la app calcula y genera el enlace oficial de verificación según la RG 4892:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                CodeSnippetBox(
                    code = """
                        URL: https://www.afip.gob.ar/fe/qr/?p=[Base64(JSON)]
                        JSON: {
                          "ver": 1,
                          "fecha": "2026-08-13",
                          "cuit": 20345678909,
                          "ptoVta": 1,
                          "tipoCmp": 6,
                          "nroCmp": 42,
                          "importe": 35900.00,
                          "moneda": "PES",
                          "ctz": 1,
                          "tipoDocRec": 96,
                          "nroDocRec": 36894021,
                          "tipoCodAut": "E",
                          "codAut": 748392019284
                        }
                    """.trimIndent()
                )
            }
        }
    }
}

@Composable
private fun ArchitectureStep(
    number: String,
    title: String,
    description: String,
    icon: ImageVector,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(color, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun CodeSnippetBox(code: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text(
            text = code,
            color = Color(0xFF38BDF8),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            lineHeight = 15.sp
        )
    }
}
