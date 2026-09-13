package com.vektorgo.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fiscal_invoices")
data class InvoiceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val paymentId: Long,
    val cbteTipo: Int,          // 1: Factura A, 6: Factura B, 11: Factura C
    val cbteTipoNombre: String,  // "Factura B", etc.
    val ptoVta: Int,
    val cbteNro: Long,
    val concepto: Int,          // 1: Prod, 2: Serv, 3: Ambos
    val docTipo: Int,           // 80: CUIT, 96: DNI, 99: Final
    val docTipoNombre: String,  // "DNI", "CUIT", "Consumidor Final"
    val docNro: Long,
    val receptorNombre: String,
    val receptorCondicionIva: String,
    val receptorEmail: String? = null,
    val cbteFch: String,        // YYYYMMDD
    val impTotal: Double,
    val impTotConc: Double = 0.0,
    val impNeto: Double,
    val impOpEx: Double = 0.0,
    val impTrib: Double = 0.0,
    val impIVA: Double,
    val ivaAlicuota: Double = 21.0,
    val cae: String,            // Código de Autorización Electrónico
    val caeFchVto: String,      // YYYYMMDD
    val resultado: String,      // A (Aprobado), R (Rechazado), E (Error)
    val observaciones: String? = null,
    val qrCodeData: String = "",
    val environment: String = "HOMOLOGACION",
    val itemsDescription: String = "Venta por Mercado Pago / Mercado Libre",
    val createdAt: Long = System.currentTimeMillis()
)
