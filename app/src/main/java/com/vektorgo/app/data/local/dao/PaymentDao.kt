package com.vektorgo.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.vektorgo.app.data.local.entity.PaymentBillingStatus
import com.vektorgo.app.data.local.entity.PaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payment_transactions ORDER BY timestamp DESC")
    fun getAllPayments(): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payment_transactions WHERE id = :id LIMIT 1")
    suspend fun getPaymentById(id: Long): PaymentEntity?

    @Query("SELECT * FROM payment_transactions WHERE billingStatus = :status ORDER BY timestamp DESC")
    fun getPaymentsByBillingStatus(status: PaymentBillingStatus): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payment_transactions WHERE billingStatus = 'PENDING_BILLING' OR billingStatus = 'ERROR'")
    suspend fun getPendingOrErrorPayments(): List<PaymentEntity>

    @Query("SELECT COUNT(*) FROM payment_transactions")
    fun getTotalPaymentsCount(): Flow<Int>

    @Query("SELECT SUM(transactionAmount) FROM payment_transactions WHERE status = 'approved'")
    fun getTotalApprovedRevenue(): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayments(payments: List<PaymentEntity>)

    @Update
    suspend fun updatePayment(payment: PaymentEntity)

    @Query("DELETE FROM payment_transactions WHERE id = :id")
    suspend fun deletePaymentById(id: Long)

    @Query("DELETE FROM payment_transactions")
    suspend fun clearAll()
}
