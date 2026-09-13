package com.vektorgo.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "arca_config")
data class ArcaConfigEntity(
    @PrimaryKey
    val id: Int = 1,
    val cuitEmisor: Long = 20345678909L,
    val razonSocial: String = "EMPRESA DEMO S.A.S.",
    val domicilioFiscal: String = "Av. Corrientes 1234, CABA, Argentina",
    val inicioActividades: String = "01/01/2022",
    val condicionIvaEmisor: String = "RESPONSABLE_INSCRIPTO", // RESPONSABLE_INSCRIPTO, MONOTRIBUTO
    val puntoVenta: Int = 1,
    val environment: String = "HOMOLOGACION", // HOMOLOGACION, PRODUCCION
    val certCrtPem: String = "",
    val privateKeyPem: String = "",
    val isArcaConnected: Boolean = true,
    val arcaAlias: String = "FactuAR-909",
    val onboardingMode: String = "AUTO_PROVISIONED", // AUTO_PROVISIONED, DELEGATION, MANUAL_KEYS

    // Mercado Pago OAuth & Webhooks
    val isMpConnected: Boolean = true,
    val mpUserName: String = "Rodrigo Timoner",
    val mpCollectorId: Long = 104928192L,
    val mpAccessToken: String = "APP_USR-7849302910394821-081313-demo-992384729104",
    val mpPublicKey: String = "APP_USR-pub-demo-123456",
    val mpWebhookSecret: String = "d41d8cd98f00b204e9800998ecf8427e",

    // Billing Automation Settings
    val autoInvoiceEnabled: Boolean = true,
    val defaultConcepto: Int = 1, // 1: Productos, 2: Servicios, 3: Ambos
    val defaultAlicuotaIva: Double = 21.0,
    val minAmountRequiresDoc: Double = 344488.0,
    val webhookRelayUrl: String = "https://us-central1-factuar-mp.cloudfunctions.net/mpWebhookRelay",
    val lastSyncTimestamp: Long = System.currentTimeMillis(),

    // Monthly Subscription (Flat Rate / Unlimited Invoices)
    val isSubscribed: Boolean = true,
    val subscriptionPlan: String = "Plan Mensual Ilimitado",
    val subscriptionPriceArs: Double = 14999.0,
    val subscriptionValidUntil: Long = System.currentTimeMillis() + (30L * 24 * 3600 * 1000),
    val subscriptionProvider: String = "MERCADO_PAGO" // MERCADO_PAGO, GOOGLE_PLAY
)
