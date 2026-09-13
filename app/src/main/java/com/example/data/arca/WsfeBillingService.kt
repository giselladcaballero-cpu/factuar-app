package com.example.data.arca

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Real SOAP client for ARCA (ex AFIP) WSFE v1. There is no simulated/offline
 * fallback: network or SOAP failures are thrown, and explicit rejections from
 * ARCA (Err nodes) are returned as a rejected WsfeVoucherResponse so the caller
 * can show the real reason instead of a fabricated CAE.
 */
class WsfeBillingService(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
) {

    private val wsfeHomoUrl = "https://wswhomo.afip.gov.ar/wsfev1/service.asmx"
    private val wsfeProdUrl = "https://servicios1.afip.gov.ar/wsfev1/service.asmx"

    /**
     * Queries FECompUltimoAutorizado to get the real last authorized voucher
     * number for a given PtoVta/CbteTipo. This is the source of truth ARCA
     * expects the next CbteDesde to follow — using only the local DB can drift
     * out of sync and cause every subsequent request to be rejected.
     */
    suspend fun getLastAuthorizedNumber(
        auth: WsfeAuth,
        ptoVta: Int,
        cbteTipo: Int,
        isProduction: Boolean
    ): Long = withContext(Dispatchers.IO) {
        val endpoint = if (isProduction) wsfeProdUrl else wsfeHomoUrl
        val soapPayload = """
            <?xml version="1.0" encoding="utf-8"?>
            <soap:Envelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
              <soap:Body>
                <FECompUltimoAutorizado xmlns="http://ar.gov.afip.dif.FEV1/">
                  <Auth>
                    <Token>${auth.token}</Token>
                    <Sign>${auth.sign}</Sign>
                    <Cuit>${auth.cuit}</Cuit>
                  </Auth>
                  <PtoVta>$ptoVta</PtoVta>
                  <CbteTipo>$cbteTipo</CbteTipo>
                </FECompUltimoAutorizado>
              </soap:Body>
            </soap:Envelope>
        """.trimIndent()

        val requestBody = soapPayload.toRequestBody("text/xml; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(endpoint)
            .post(requestBody)
            .addHeader("SOAPAction", "http://ar.gov.afip.dif.FEV1/FECompUltimoAutorizado")
            .build()

        val response = httpClient.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            throw IOException("ARCA WSFE FECompUltimoAutorizado HTTP ${response.code}: ${extractSoapFault(responseBody).ifBlank { responseBody.take(500) }}")
        }

        val errores = extractErrores(responseBody)
        if (errores.isNotEmpty()) {
            throw IOException("ARCA rechazó FECompUltimoAutorizado: ${errores.joinToString(" | ")}")
        }

        val cbteNroStr = responseBody.substringAfter("<CbteNro>", "").substringBefore("</CbteNro>")
        return@withContext cbteNroStr.toLongOrNull() ?: 0L
    }

    /**
     * Builds and transmits FECAESolicitar to ARCA (ex AFIP) WSFE v1.
     */
    suspend fun solicitarCae(
        auth: WsfeAuth,
        voucherReq: WsfeVoucherRequest,
        isProduction: Boolean = false
    ): WsfeVoucherResponse = withContext(Dispatchers.IO) {
        val endpoint = if (isProduction) wsfeProdUrl else wsfeHomoUrl
        val soapPayload = buildFecaeSolicitarXml(auth, voucherReq)

        val requestBody = soapPayload.toRequestBody("text/xml; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(endpoint)
            .post(requestBody)
            .addHeader("SOAPAction", "http://ar.gov.afip.dif.FEV1/FECAESolicitar")
            .build()

        val response = httpClient.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            val faultDetail = extractSoapFault(responseBody).ifBlank { responseBody.take(500) }
            val diagnostic = if (faultDetail.isBlank()) {
                // ARCA returned an empty body — likely a raw parser-level
                // rejection before it could even build a SOAP Fault. Include
                // the request we actually sent (credentials redacted) so the
                // malformed XML can be spotted directly.
                "(sin contenido) | Request enviado: ${redactAuthForLog(soapPayload).take(1500)}"
            } else {
                faultDetail
            }
            throw IOException("ARCA WSFE FECAESolicitar HTTP ${response.code}: $diagnostic")
        }

        parseFecaeResponse(responseBody, voucherReq.cbteDesde)
    }

    private fun redactAuthForLog(xml: String): String {
        return xml
            .replace(Regex("<Token>.*?</Token>"), "<Token>[redacted]</Token>")
            .replace(Regex("<Sign>.*?</Sign>"), "<Sign>[redacted]</Sign>")
    }

    /**
     * Constructs full XML SOAP Envelope for FECAESolicitar.
     */
    fun buildFecaeSolicitarXml(auth: WsfeAuth, req: WsfeVoucherRequest): String {
        val ivaXml = if (req.ivaItems.isNotEmpty()) {
            val items = req.ivaItems.joinToString("\n") { iva ->
                """
                <AlicIva>
                    <Id>${iva.id}</Id>
                    <BaseImp>${String.format(Locale.US, "%.2f", iva.baseImp)}</BaseImp>
                    <Importe>${String.format(Locale.US, "%.2f", iva.importe)}</Importe>
                </AlicIva>
                """.trimIndent()
            }
            "<Iva>\n$items\n</Iva>"
        } else {
            ""
        }

        val serviciosFechas = if (req.concepto == 2 || req.concepto == 3) {
            """
            <FchServDesde>${req.fchServDesde ?: req.cbteFch}</FchServDesde>
            <FchServHasta>${req.fchServHasta ?: req.cbteFch}</FchServHasta>
            <FchVtoPago>${req.fchVtoPago ?: req.cbteFch}</FchVtoPago>
            """.trimIndent()
        } else {
            ""
        }

        // Links a Nota de Crédito/Débito to the original comprobante it
        // corrects — required by ARCA, and must appear after MonCotiz and
        // before Iva per the WSFEv1 XSD element order.
        val cbtesAsocXml = if (req.cbtesAsociados.isNotEmpty()) {
            val items = req.cbtesAsociados.joinToString("\n") { asoc ->
                """
                <CbteAsoc>
                    <Tipo>${asoc.tipo}</Tipo>
                    <PtoVta>${asoc.ptoVta}</PtoVta>
                    <Nro>${asoc.nro}</Nro>
                    <Cuit>${asoc.cuit}</Cuit>
                    ${asoc.cbteFch?.let { "<CbteFch>$it</CbteFch>" } ?: ""}
                </CbteAsoc>
                """.trimIndent()
            }
            "<CbtesAsoc>\n$items\n</CbtesAsoc>"
        } else {
            ""
        }

        return """
            <?xml version="1.0" encoding="utf-8"?>
            <soap:Envelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
              <soap:Body>
                <FECAESolicitar xmlns="http://ar.gov.afip.dif.FEV1/">
                  <Auth>
                    <Token>${auth.token}</Token>
                    <Sign>${auth.sign}</Sign>
                    <Cuit>${auth.cuit}</Cuit>
                  </Auth>
                  <FeCAEReq>
                    <FeCabReq>
                      <CantReg>1</CantReg>
                      <PtoVta>${req.ptoVta}</PtoVta>
                      <CbteTipo>${req.cbteTipo}</CbteTipo>
                    </FeCabReq>
                    <FeDetReq>
                      <FECAEDetRequest>
                        <Concepto>${req.concepto}</Concepto>
                        <DocTipo>${req.docTipo}</DocTipo>
                        <DocNro>${req.docNro}</DocNro>
                        <CbteDesde>${req.cbteDesde}</CbteDesde>
                        <CbteHasta>${req.cbteHasta}</CbteHasta>
                        <CbteFch>${req.cbteFch}</CbteFch>
                        <ImpTotal>${String.format(Locale.US, "%.2f", req.impTotal)}</ImpTotal>
                        <ImpTotConc>${String.format(Locale.US, "%.2f", req.impTotConc)}</ImpTotConc>
                        <ImpNeto>${String.format(Locale.US, "%.2f", req.impNeto)}</ImpNeto>
                        <ImpOpEx>${String.format(Locale.US, "%.2f", req.impOpEx)}</ImpOpEx>
                        <ImpTrib>${String.format(Locale.US, "%.2f", req.impTrib)}</ImpTrib>
                        <ImpIVA>${String.format(Locale.US, "%.2f", req.impIVA)}</ImpIVA>
                        $serviciosFechas
                        <MonId>${req.monId}</MonId>
                        <MonCotiz>${String.format(Locale.US, "%.1f", req.monCotiz)}</MonCotiz>
                        $cbtesAsocXml
                        $ivaXml
                      </FECAEDetRequest>
                    </FeDetReq>
                  </FeCAEReq>
                </FECAESolicitar>
              </soap:Body>
            </soap:Envelope>
        """.trimIndent()
    }

    private fun parseFecaeResponse(xml: String, cbteNro: Long): WsfeVoucherResponse {
        val resultado = xml.substringAfter("<Resultado>", "").substringBefore("</Resultado>")
        val cae = xml.substringAfter("<CAE>", "").substringBefore("</CAE>")
        val caeFchVto = xml.substringAfter("<CAEFchVto>", "").substringBefore("</CAEFchVto>")

        val observaciones = extractMsgBlocks(xml, "Obs")
        val errores = extractMsgBlocks(xml, "Err")

        return WsfeVoucherResponse(
            resultado = resultado.ifBlank { if (cae.isNotBlank()) "A" else "R" },
            cae = cae.ifBlank { null },
            caeFchVto = caeFchVto.ifBlank { null },
            cbteNro = cbteNro,
            observaciones = observaciones,
            errores = errores,
            rawSoapResponse = xml
        )
    }

    private fun extractErrores(xml: String): List<String> = extractMsgBlocks(xml, "Err")

    private fun extractMsgBlocks(xml: String, tag: String): List<String> {
        val results = mutableListOf<String>()
        if (!xml.contains("<$tag>")) return results
        val blocks = xml.split("<$tag>")
        for (i in 1 until blocks.size) {
            val msg = blocks[i].substringAfter("<Msg>", "").substringBefore("</Msg>")
            if (msg.isNotBlank()) results.add(msg)
        }
        return results
    }

    private fun extractSoapFault(xml: String): String {
        if (!xml.contains("faultstring")) return ""
        val faultCode = xml.substringAfter("<faultcode>", "").substringBefore("</faultcode>")
        val faultString = xml.substringAfter("<faultstring>").substringBefore("</faultstring>")
        val detail = xml.substringAfter("<detail>", "").substringBefore("</detail>").trim()
        return buildString {
            if (faultCode.isNotBlank()) append("[$faultCode] ")
            append(faultString)
            if (detail.isNotBlank()) append(" | detail: $detail")
        }
    }
}
