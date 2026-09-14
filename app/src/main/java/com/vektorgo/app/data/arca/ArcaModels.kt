package com.vektorgo.app.data.arca

data class WsfeAuth(
    val token: String,
    val sign: String,
    val cuit: Long
)

data class ArcaIvaItem(
    val id: Int, // 3: 0%, 4: 10.5%, 5: 21%, 6: 27%, 8: 5%, 9: 2.5%
    val baseImp: Double,
    val importe: Double
)

/**
 * Reference to the original voucher a Nota de Crédito/Débito cancels or
 * adjusts. ARCA requires this so the correction is linked to the original
 * comprobante, not just a free-floating document with the same amount.
 */
data class ArcaCbteAsociado(
    val tipo: Int,
    val ptoVta: Int,
    val nro: Long,
    val cuit: Long,
    val cbteFch: String? = null
)

data class WsfeVoucherRequest(
    val ptoVta: Int,
    val cbteTipo: Int, // 1: Factura A, 6: Factura B, 11: Factura C, 3/8/13: Notas de Crédito A/B/C
    val concepto: Int, // 1: Productos, 2: Servicios, 3: Productos y Servicios
    val docTipo: Int,  // 80: CUIT, 96: DNI, 99: Consumidor Final
    val docNro: Long,
    val cbteDesde: Long,
    val cbteHasta: Long,
    val cbteFch: String, // YYYYMMDD
    val impTotal: Double,
    val impTotConc: Double = 0.0,
    val impNeto: Double,
    val impOpEx: Double = 0.0,
    val impTrib: Double = 0.0,
    val impIVA: Double,
    val fchServDesde: String? = null,
    val fchServHasta: String? = null,
    val fchVtoPago: String? = null,
    val monId: String = "PES",
    val monCotiz: Double = 1.0,
    // Mandatory since ARCA's RG 5616 ("Condición IVA del receptor"):
    // 1 Responsable Inscripto, 4 Exento, 5 Consumidor Final, 6 Monotributo.
    val condicionIvaReceptorId: Int,
    val ivaItems: List<ArcaIvaItem> = emptyList(),
    val cbtesAsociados: List<ArcaCbteAsociado> = emptyList()
)

data class WsfeVoucherResponse(
    val resultado: String, // "A" (Aprobado), "R" (Rechazado), "E" (Error)
    val cae: String?,
    val caeFchVto: String?,
    val cbteNro: Long,
    val observaciones: List<String> = emptyList(),
    val errores: List<String> = emptyList(),
    val rawSoapResponse: String = ""
)

data class WsaaTicketResult(
    val success: Boolean,
    val token: String = "",
    val sign: String = "",
    val generationTimeMillis: Long = 0L,
    val expirationTimeMillis: Long = 0L,
    val errorMessage: String? = null
)

/**
 * Result of checking a Punto de Venta against ARCA's own records via
 * FEParamGetPtosVenta, instead of trusting the user did the manual
 * "Puntos de Venta y Domicilios" step correctly.
 */
data class PuntoVentaCheckResult(
    val exists: Boolean,
    val emisionTipo: String? = null,
    val bloqueado: Boolean = false,
    val errorMessage: String? = null
)
