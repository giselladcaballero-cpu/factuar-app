package com.vektorgo.app.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.Constraints
import com.vektorgo.app.data.local.AppDatabase
import com.vektorgo.app.data.repository.BillingRepository
import java.util.concurrent.TimeUnit

/**
 * Background counterpart to BillingViewModel's in-process 10-minute sync
 * loop: this one runs even when the app isn't open. Android's WorkManager
 * enforces a 15-minute floor on PeriodicWorkRequest — there is no way to
 * schedule a true background job more often than that — so it can't match
 * the in-app loop's cadence exactly, but it covers the gap when the
 * merchant isn't actively looking at the screen.
 *
 * Both paths call the same BillingRepository.syncMercadoPagoMovements(),
 * which reconciles by payment id, so running from two places never
 * double-processes or double-invoices a payment.
 */
class MpSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repository = BillingRepository(AppDatabase.getDatabase(applicationContext))
        val config = repository.getConfig()
        if (!config.isMpConnected || config.mpAccessToken.isBlank()) {
            return Result.success()
        }

        val syncResult = repository.syncMercadoPagoMovements()
        return if (syncResult.isSuccess) {
            Result.success()
        } else {
            Result.retry()
        }
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "mp_movements_sync"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<MpSyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
