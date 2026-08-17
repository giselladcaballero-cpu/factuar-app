package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class PaymentBillingStatus {
    PENDING_BILLING,
    INVOICED,
    ERROR,
    IGNORED
}

@Entity(tableName = "payment_transactions")
data class PaymentEntity(
    @PrimaryKey
    val id: Long, // Mercado Pago Payment ID
    val collectorId: Long,
    val dateApproved: String,
    val dateCreated: String,
    val transactionAmount: Double,
    val netReceivedAmount: Double,
    val currencyId: String = "ARS",
    val paymentMethodId: String, // account_money, visa, master, etc.
    val paymentTypeId: String,   // credit_card, debit_card, etc.
    val status: String,          // approved, pending, rejected
    val statusDetail: String,    // accredited, etc.
    val description: String,
    val payerEmail: String,
    val payerDniCuit: String,
    val payerDocType: String,    // DNI, CUIT, CUIL
    val payerFirstName: String,
    val payerLastName: String,
    val externalReference: String? = null,
    val billingStatus: PaymentBillingStatus = PaymentBillingStatus.PENDING_BILLING,
    val associatedInvoiceId: Long? = null,
    val lastError: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
