package com.vektorgo.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class LogSeverity {
    INFO,
    SUCCESS,
    WARNING,
    ERROR
}

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val eventType: String, // WSAA_LOGIN, WSFE_CAE, MP_WEBHOOK, WORKER_SYNC, MANUAL_RETRY
    val title: String,
    val message: String,
    val severity: LogSeverity = LogSeverity.INFO,
    val payloadJson: String? = null
)
