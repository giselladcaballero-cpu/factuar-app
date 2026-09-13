package com.vektorgo.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vektorgo.app.data.local.entity.InvoiceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InvoiceDao {
    @Query("SELECT * FROM fiscal_invoices ORDER BY createdAt DESC")
    fun getAllInvoices(): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM fiscal_invoices WHERE id = :id LIMIT 1")
    suspend fun getInvoiceById(id: Long): InvoiceEntity?

    @Query("SELECT * FROM fiscal_invoices WHERE paymentId = :paymentId LIMIT 1")
    suspend fun getInvoiceByPaymentId(paymentId: Long): InvoiceEntity?

    @Query("SELECT COUNT(*) FROM fiscal_invoices WHERE resultado = 'A'")
    fun getApprovedInvoicesCount(): Flow<Int>

    @Query("SELECT SUM(impTotal) FROM fiscal_invoices WHERE resultado = 'A'")
    fun getTotalInvoicedAmount(): Flow<Double?>

    @Query("SELECT MAX(cbteNro) FROM fiscal_invoices WHERE ptoVta = :ptoVta AND cbteTipo = :cbteTipo")
    suspend fun getLastVoucherNumber(ptoVta: Int, cbteTipo: Int): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: InvoiceEntity): Long

    @Query("DELETE FROM fiscal_invoices WHERE id = :id")
    suspend fun deleteInvoiceById(id: Long)

    @Query("DELETE FROM fiscal_invoices")
    suspend fun clearAll()
}
