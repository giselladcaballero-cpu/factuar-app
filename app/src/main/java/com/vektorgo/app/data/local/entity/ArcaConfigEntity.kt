package com.vektorgo.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "arca_config")
data class ArcaConfigEntity(
    @PrimaryKey
    val id: Int = 1,
    val cuitEmisor: Long = 0L,
    val razonSocial: String = "",
    val domicilioFiscal: String = "",
    val inicioActividades: String = "",
    val condicionIvaEmisor: String = "RESPONSABLE_INSCRIPTO", // RESPONSABLE_INSCRIPTO, MONOTRIBUTO
    val puntoVenta: Int = 1,
    val environment: String = "HOMOLOGACION", // HOMOLOGACION, PRODUCCION
    val certCrtPem: String = "",
    val privateKeyPem: String = "",
    val isArcaConnected: Boolean = false,
    val arcaAlias: String = "",
    val onboardingMode: String = "NONE", // NONE, CSR_MANUAL, MANUAL_KEYS

    // Mercado Pago Access Token & Webhooks
    val isMpConnected: Boolean = false,
    val mpUserName: String = "",
    val mpCollectorId: Long = 0L,
    val mpAccessToken: String = "",
    val mpPublicKey: String = "",
    val mpWebhookSecret: String = "",

    // Billing Automation Settings
    val autoInvoiceEnabled: Boolean = false,
    val defaultConcepto: Int = 1, // 1: Productos, 2: Servicios, 3: Ambos
    val defaultAlicuotaIva: Double = 21.0,
    val minAmountRequiresDoc: Double = 344488.0,
    val webhookRelayUrl: String = "",
    val lastSyncTimestamp: Long = System.currentTimeMillis(),

    // Vektor Go's own subscription (what the merchant pays US, not what
    // their customers pay them). No free trial: isSubscribed only ever
    // becomes true once Mercado Pago confirms the preapproval is
    // "authorized" — see subscription-callback in supabase/functions/.
    val isSubscribed: Boolean = false,
    val subscriptionId: String = "", // Mercado Pago preapproval id
    val subscriptionStatus: String = "NONE" // NONE, PENDING, AUTHORIZED, CANCELLED
)
