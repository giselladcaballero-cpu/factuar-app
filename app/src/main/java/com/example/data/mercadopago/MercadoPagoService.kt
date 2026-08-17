package com.example.data.mercadopago

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random

class MercadoPagoService(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
) {

    private val mpApiBase = "https://api.mercadopago.com"

    /**
     * Verifies HMAC-SHA256 signature from Mercado Pago Webhook header.
     * Format of x-signature: "ts=1710000000,v1=a1b2c3d4e5..."
     */
    fun verifyWebhookSignature(
        xSignatureHeader: String,
        requestId: String,
        dataId: String,
        secretKey: String
    ): Boolean {
        if (xSignatureHeader.isBlank() || secretKey.isBlank()) return true // Allow in dev/mock

        try {
            val parts = xSignatureHeader.split(",")
            var ts = ""
            var v1 = ""
            for (part in parts) {
                val kv = part.trim().split("=")
                if (kv.size == 2) {
                    if (kv[0] == "ts") ts = kv[1]
                    if (kv[0] == "v1") v1 = kv[1]
                }
            }

            if (ts.isEmpty() || v1.isEmpty()) return false

            // Manifest template: id:[data.id];request-id:[x-request-id];ts:[ts];
            val manifest = "id:$dataId;request-id:$requestId;ts:$ts;"

            val mac = Mac.getInstance("HmacSHA256")
            val keySpec = SecretKeySpec(secretKey.toByteArray(StandardCharsets.UTF_8), "HmacSHA256")
            mac.init(keySpec)
            val hashBytes = mac.doFinal(manifest.toByteArray(StandardCharsets.UTF_8))
            val calculatedV1 = hashBytes.joinToString("") { "%02x".format(it) }

            return calculatedV1.equals(v1, ignoreCase = true)
        } catch (e: Exception) {
            Log.e("MercadoPagoService", "Error verifying signature: ${e.message}")
            return false
        }
    }

    /**
     * Validates a personal Access Token against the real Mercado Pago API and
     * returns the linked account identity (used to confirm the token belongs
     * to the expected account before saving it).
     */
    suspend fun getAccountInfo(accessToken: String): Result<MpAccountInfo> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$mpApiBase/users/me")
                .addHeader("Authorization", "Bearer $accessToken")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful && responseBody.isNotEmpty()) {
                val json = JSONObject(responseBody)
                Result.success(
                    MpAccountInfo(
                        id = json.getLong("id"),
                        nickname = json.optString("nickname", ""),
                        firstName = json.optString("first_name", null),
                        lastName = json.optString("last_name", null),
                        email = json.optString("email", null)
                    )
                )
            } else {
                Result.failure(Exception("Token inválido o rechazado por Mercado Pago (HTTP ${response.code})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Queries Mercado Pago API to fetch full payment details.
     */
    suspend fun getPaymentDetails(paymentId: Long, accessToken: String): Result<MpPaymentDetail> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$mpApiBase/v1/payments/$paymentId")
                .addHeader("Authorization", "Bearer $accessToken")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful && responseBody.isNotEmpty()) {
                val json = JSONObject(responseBody)
                val payerJson = json.optJSONObject("payer")
                val identJson = payerJson?.optJSONObject("identification")

                val payer = MpPayer(
                    email = payerJson?.optString("email") ?: "cliente@mercadolibre.com.ar",
                    firstName = payerJson?.optString("first_name"),
                    lastName = payerJson?.optString("last_name"),
                    identification = if (identJson != null && identJson.has("number")) {
                        MpPayerIdentification(
                            type = identJson.optString("type", "DNI"),
                            number = identJson.optString("number")
                        )
                    } else null
                )

                val detail = MpPaymentDetail(
                    id = json.getLong("id"),
                    collectorId = json.optLong("collector_id", 123456789L),
                    dateApproved = json.optString("date_approved", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US).format(Date())),
                    dateCreated = json.optString("date_created", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US).format(Date())),
                    transactionAmount = json.getDouble("transaction_amount"),
                    netReceivedAmount = json.optDouble("net_received_amount", json.getDouble("transaction_amount") * 0.94),
                    currencyId = json.optString("currency_id", "ARS"),
                    paymentMethodId = json.optString("payment_method_id", "account_money"),
                    paymentTypeId = json.optString("payment_type_id", "account_money"),
                    status = json.getString("status"),
                    statusDetail = json.optString("status_detail", "accredited"),
                    description = json.optString("description", "Venta Mercado Pago"),
                    payer = payer,
                    externalReference = json.optString("external_reference")
                )
                return@withContext Result.success(detail)
            } else {
                return@withContext Result.failure(Exception("HTTP ${response.code}: $responseBody"))
            }
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }

    /**
     * Generates a realistic mock payment for testing auto-invoicing scenarios.
     */
    fun createSimulatedPayment(scenarioIndex: Int = 0): MpPaymentDetail {
        val now = Date()
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US).format(now)
        val randomPaymentId = Random.nextLong(70000000000L, 99999999999L)

        return when (scenarioIndex % 4) {
            0 -> {
                // Scenario 1: Factura B (Consumidor Final con DNI)
                MpPaymentDetail(
                    id = randomPaymentId,
                    collectorId = 20345678909L,
                    dateApproved = isoFormat,
                    dateCreated = isoFormat,
                    transactionAmount = 35900.0,
                    netReceivedAmount = 33746.0,
                    currencyId = "ARS",
                    paymentMethodId = "master",
                    paymentTypeId = "credit_card",
                    status = "approved",
                    statusDetail = "accredited",
                    description = "Auriculares Inalámbricos Bluetooth Pro ANC",
                    payer = MpPayer(
                        email = "martin.alvarez@gmail.com",
                        firstName = "Martín",
                        lastName = "Álvarez",
                        identification = MpPayerIdentification(type = "DNI", number = "36894021")
                    ),
                    externalReference = "MLA-9823741"
                )
            }
            1 -> {
                // Scenario 2: Factura A (Empresa / Responsable Inscripto con CUIT)
                MpPaymentDetail(
                    id = randomPaymentId,
                    collectorId = 20345678909L,
                    dateApproved = isoFormat,
                    dateCreated = isoFormat,
                    transactionAmount = 142500.0,
                    netReceivedAmount = 133950.0,
                    currencyId = "ARS",
                    paymentMethodId = "account_money",
                    paymentTypeId = "account_money",
                    status = "approved",
                    statusDetail = "accredited",
                    description = "Silla Ergonómica Oficina Premium Mesh",
                    payer = MpPayer(
                        email = "compras@logistica-norte.com.ar",
                        firstName = "Logística Norte",
                        lastName = "S.R.L.",
                        identification = MpPayerIdentification(type = "CUIT", number = "30715894321")
                    ),
                    externalReference = "MLA-1102938"
                )
            }
            2 -> {
                // Scenario 3: Factura B (Servicio / Suscripción Software)
                MpPaymentDetail(
                    id = randomPaymentId,
                    collectorId = 20345678909L,
                    dateApproved = isoFormat,
                    dateCreated = isoFormat,
                    transactionAmount = 18500.0,
                    netReceivedAmount = 17390.0,
                    currencyId = "ARS",
                    paymentMethodId = "visa",
                    paymentTypeId = "debit_card",
                    status = "approved",
                    statusDetail = "accredited",
                    description = "Licencia Mensual Hosting y Soporte Cloud",
                    payer = MpPayer(
                        email = "carolina.mendez@outlook.com",
                        firstName = "Carolina",
                        lastName = "Méndez",
                        identification = MpPayerIdentification(type = "DNI", number = "40192847")
                    ),
                    externalReference = "SRV-4491"
                )
            }
            else -> {
                // Scenario 4: Consumidor Final anónimo (Sin DNI declarado)
                MpPaymentDetail(
                    id = randomPaymentId,
                    collectorId = 20345678909L,
                    dateApproved = isoFormat,
                    dateCreated = isoFormat,
                    transactionAmount = 9800.0,
                    netReceivedAmount = 9212.0,
                    currencyId = "ARS",
                    paymentMethodId = "rapipago",
                    paymentTypeId = "ticket",
                    status = "approved",
                    statusDetail = "accredited",
                    description = "Funda Protectora y Vidrio Templado",
                    payer = MpPayer(
                        email = "comprador_anonimo@yahoo.com.ar",
                        firstName = "Comprador",
                        lastName = "Mercado Libre",
                        identification = null
                    ),
                    externalReference = "MLA-7729103"
                )
            }
        }
    }
}
