package com.vektorgo.app.data.mercadopago

data class MpWebhookNotification(
    val id: String,
    val liveMode: Boolean,
    val type: String, // "payment"
    val dateCreated: String,
    val userId: Long,
    val apiVersion: String,
    val action: String, // "payment.created", "payment.updated"
    val dataId: Long
)

data class MpPayerIdentification(
    val type: String, // "DNI", "CUIT", "CUIL", "OTHER"
    val number: String
)

data class MpPayer(
    val email: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val identification: MpPayerIdentification? = null
)

data class MpPaymentDetail(
    val id: Long,
    val collectorId: Long,
    val dateApproved: String,
    val dateCreated: String,
    val transactionAmount: Double,
    val netReceivedAmount: Double,
    val currencyId: String = "ARS",
    val paymentMethodId: String,
    val paymentTypeId: String,
    val status: String, // "approved", "pending", "rejected"
    val statusDetail: String,
    val description: String,
    val payer: MpPayer,
    val externalReference: String? = null
)
