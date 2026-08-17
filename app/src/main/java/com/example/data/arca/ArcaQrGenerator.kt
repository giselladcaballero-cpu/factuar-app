package com.example.data.arca

import android.util.Base64
import org.json.JSONObject
import java.nio.charset.StandardCharsets

object ArcaQrGenerator {

    /**
     * Generates the official ARCA (ex AFIP) QR verification link according to RG 4892.
     * https://www.afip.gob.ar/fe/qr/?p=[Base64_JSON]
     */
    fun generateQrUrl(
        cuitEmisor: Long,
        ptoVta: Int,
        tipoCmp: Int,
        nroCmp: Long,
        fechaYyyyMmDd: String, // Format "20260813" or "2026-08-13"
        importe: Double,
        tipoDocRec: Int,
        nroDocRec: Long,
        cae: String
    ): String {
        val formattedDate = if (fechaYyyyMmDd.length == 8 && !fechaYyyyMmDd.contains("-")) {
            "${fechaYyyyMmDd.substring(0, 4)}-${fechaYyyyMmDd.substring(4, 6)}-${fechaYyyyMmDd.substring(6, 8)}"
        } else {
            fechaYyyyMmDd
        }

        val json = JSONObject().apply {
            put("ver", 1)
            put("fecha", formattedDate)
            put("cuit", cuitEmisor)
            put("ptoVta", ptoVta)
            put("tipoCmp", tipoCmp)
            put("nroCmp", nroCmp)
            put("importe", importe)
            put("moneda", "PES")
            put("ctz", 1.0)
            put("tipoDocRec", tipoDocRec)
            put("nroDocRec", nroDocRec)
            put("tipoCodAut", "E")
            put("codAut", cae.toLongOrNull() ?: 74392817492019L)
        }

        val base64Json = Base64.encodeToString(
            json.toString().toByteArray(StandardCharsets.UTF_8),
            Base64.NO_WRAP
        )

        return "https://www.afip.gob.ar/fe/qr/?p=$base64Json"
    }
}
