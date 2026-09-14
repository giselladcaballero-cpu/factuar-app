package com.vektorgo.app.data.admin

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Reports this merchant's subscription state to the admin panel's backend
 * (register-subscriber Edge Function). Best-effort: if it fails, the
 * merchant's own subscription still works locally — this only feeds the
 * panel used to see who's subscribed, active or not, and how much they
 * pay. Never blocks or reverts activateSubscription() on failure.
 */
class AdminSyncService(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
) {
    private val registerUrl = "https://aaalabfxyrdpunhcanbu.supabase.co/functions/v1/register-subscriber"

    suspend fun registerSubscriber(
        collectorId: Long,
        businessName: String,
        cuit: Long,
        email: String,
        subscriptionId: String,
        subscriptionStatus: String,
        subscriptionAmount: Double,
        environment: String,
        appVersion: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("mp_collector_id", collectorId)
                put("business_name", businessName)
                put("cuit", cuit)
                put("email", email)
                put("subscription_id", subscriptionId)
                put("subscription_status", subscriptionStatus)
                put("subscription_amount", subscriptionAmount)
                put("environment", environment)
                put("app_version", appVersion)
            }

            val request = Request.Builder()
                .url(registerUrl)
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("HTTP ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
