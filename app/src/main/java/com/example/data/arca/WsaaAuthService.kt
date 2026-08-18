package com.example.data.arca

import android.util.Base64
import com.example.data.local.entity.ArcaConfigEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.bouncycastle.cert.jcajce.JcaCertStore
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder
import org.bouncycastle.cms.CMSProcessableByteArray
import org.bouncycastle.cms.CMSSignedDataGenerator
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder
import java.io.ByteArrayInputStream
import java.security.PrivateKey
import java.security.Security
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec
import java.security.KeyFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * Authenticates against ARCA (ex AFIP) WSAA by signing a Ticket de Requerimiento
 * de Acceso (TRA) into a real CMS/PKCS#7 SignedData envelope, as required by
 * https://www.afip.gob.ar/ws/WSAA/Especificacion_Tecnica_WSAA_1.2.2.pdf
 *
 * There is no simulated/mock fallback here: if signing or the SOAP call fails,
 * the failure is returned as-is so the caller can surface a real error instead
 * of a fabricated ticket.
 */
class WsaaAuthService(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
) {

    init {
        // Android ships its own stripped-down "BC" provider (from Conscrypt),
        // which doesn't support the signature algorithms we need. It must be
        // removed before installing the full BouncyCastle provider under the
        // same name, otherwise Security.addProvider() is a silent no-op.
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
        Security.insertProviderAt(BouncyCastleProvider(), 1)
    }

    private val wsaaHomoUrl = "https://wsaahomo.afip.gov.ar/ws/services/LoginCms"
    private val wsaaProdUrl = "https://wsaa.afip.gov.ar/ws/services/LoginCms"

    /**
     * Obtains a real Ticket de Acceso (Token + Sign) from ARCA WSAA.
     * Requires a valid certificate + private key to be configured; there is no
     * offline/simulated result.
     */
    suspend fun obtainAccessTicket(config: ArcaConfigEntity): WsaaTicketResult = withContext(Dispatchers.IO) {
        if (config.certCrtPem.isBlank() || config.privateKeyPem.isBlank()) {
            return@withContext WsaaTicketResult(
                success = false,
                errorMessage = "Falta cargar el certificado y la clave privada de ARCA en Configuración."
            )
        }

        try {
            val isProd = config.environment.equals("PRODUCCION", ignoreCase = true)
            val wsaaEndpoint = if (isProd) wsaaProdUrl else wsaaHomoUrl

            val traXml = createTraXml(service = "wsfe")
            val cmsBase64 = signTraCms(traXml, config.certCrtPem, config.privateKeyPem)
            val soapRequest = buildSoapEnvelope(cmsBase64)

            val requestBody = soapRequest.toRequestBody("text/xml; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(wsaaEndpoint)
                .post(requestBody)
                .addHeader("SOAPAction", "")
                .build()

            httpClient.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                if (response.isSuccessful && responseBody.contains("<token>")) {
                    return@withContext parseLoginTicketResponse(responseBody)
                }
                return@withContext WsaaTicketResult(
                    success = false,
                    errorMessage = "WSAA respondió HTTP ${response.code}: ${extractSoapFault(responseBody).ifBlank { responseBody.take(500) }}"
                )
            }
        } catch (e: Exception) {
            WsaaTicketResult(
                success = false,
                errorMessage = "Error autenticando con WSAA: ${e.message}"
            )
        }
    }

    /**
     * Generates Ticket de Requerimiento de Acceso (TRA) XML according to ARCA specification.
     */
    fun createTraXml(service: String = "wsfe"): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("GMT-3")
        }
        val now = System.currentTimeMillis()
        val genTime = sdf.format(Date(now - 120_000L)) // 2 minutes ago
        val expTime = sdf.format(Date(now + 1200_000L)) // +20 minutes

        val uniqueId = (System.currentTimeMillis() / 1000).toString()

        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <loginTicketRequest version="1.0">
              <header>
                <uniqueId>$uniqueId</uniqueId>
                <generationTime>$genTime</generationTime>
                <expirationTime>$expTime</expirationTime>
              </header>
              <service>$service</service>
            </loginTicketRequest>
        """.trimIndent()
    }

    /**
     * Signs the TRA XML into a real CMS/PKCS#7 SignedData envelope (encapsulated
     * content, SHA256withRSA), the format ARCA's WSAA loginCms operation expects.
     */
    private fun signTraCms(traXml: String, certPem: String, keyPem: String): String {
        val privateKey = parsePrivateKeyPem(keyPem)
        val certificate = parseCertificatePem(certPem)

        val contentSigner = JcaContentSignerBuilder("SHA256withRSA")
            .setProvider(BouncyCastleProvider.PROVIDER_NAME)
            .build(privateKey)

        val digestCalculatorProvider = JcaDigestCalculatorProviderBuilder()
            .setProvider(BouncyCastleProvider.PROVIDER_NAME)
            .build()

        val generator = CMSSignedDataGenerator()
        generator.addSignerInfoGenerator(
            JcaSignerInfoGeneratorBuilder(digestCalculatorProvider)
                .build(contentSigner, certificate)
        )
        generator.addCertificates(JcaCertStore(listOf(certificate)))

        val content = CMSProcessableByteArray(traXml.toByteArray(Charsets.UTF_8))
        val signedData = generator.generate(content, true) // encapsulate = true

        return Base64.encodeToString(signedData.encoded, Base64.NO_WRAP)
    }

    private fun buildSoapEnvelope(cmsBase64: String): String {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:wsaa="http://wsaa.view.sua.dvadac.desein.afip.gov">
              <soapenv:Header/>
              <soapenv:Body>
                <wsaa:loginCms>
                  <wsaa:in0>$cmsBase64</wsaa:in0>
                </wsaa:loginCms>
              </soapenv:Body>
            </soapenv:Envelope>
        """.trimIndent()
    }

    private fun parseLoginTicketResponse(xmlString: String): WsaaTicketResult {
        return try {
            val token = xmlString.substringAfter("<token>").substringBefore("</token>")
            val sign = xmlString.substringAfter("<sign>").substringBefore("</sign>")
            val expString = xmlString.substringAfter("<expirationTime>").substringBefore("</expirationTime>")

            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("GMT-3")
            }
            val expMillis = try {
                sdf.parse(expString.substringBefore("."))?.time ?: (System.currentTimeMillis() + 12 * 3600 * 1000L)
            } catch (e: Exception) {
                System.currentTimeMillis() + 12 * 3600 * 1000L
            }

            if (token.isBlank() || sign.isBlank()) {
                return WsaaTicketResult(
                    success = false,
                    errorMessage = "Respuesta de WSAA sin token/sign: ${xmlString.take(500)}"
                )
            }

            WsaaTicketResult(
                success = true,
                token = token,
                sign = sign,
                generationTimeMillis = System.currentTimeMillis(),
                expirationTimeMillis = expMillis
            )
        } catch (e: Exception) {
            WsaaTicketResult(
                success = false,
                errorMessage = "Error parseando respuesta de WSAA: ${e.message}"
            )
        }
    }

    private fun extractSoapFault(xml: String): String {
        if (!xml.contains("faultstring")) return ""
        return xml.substringAfter("<faultstring>").substringBefore("</faultstring>")
    }

    private fun parsePrivateKeyPem(pem: String): PrivateKey {
        val cleanPem = pem
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("-----BEGIN RSA PRIVATE KEY-----", "")
            .replace("-----END RSA PRIVATE KEY-----", "")
            .replace("\\s".toRegex(), "")
        val keyBytes = Base64.decode(cleanPem, Base64.DEFAULT)
        val spec = PKCS8EncodedKeySpec(keyBytes)
        return KeyFactory.getInstance("RSA").generatePrivate(spec)
    }

    private fun parseCertificatePem(pem: String): X509Certificate {
        val cleanPem = pem
            .replace("-----BEGIN CERTIFICATE-----", "")
            .replace("-----END CERTIFICATE-----", "")
            .replace("\\s".toRegex(), "")
        val certBytes = Base64.decode(cleanPem, Base64.DEFAULT)
        val cf = CertificateFactory.getInstance("X.509")
        return cf.generateCertificate(ByteArrayInputStream(certBytes)) as X509Certificate
    }
}
