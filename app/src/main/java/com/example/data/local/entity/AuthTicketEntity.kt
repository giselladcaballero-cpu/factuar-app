package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "auth_tickets")
data class AuthTicketEntity(
    @PrimaryKey
    val service: String = "wsfe",
    val token: String,
    val sign: String,
    val cuit: Long,
    val generationTimeMillis: Long,
    val expirationTimeMillis: Long,
    val environment: String = "HOMOLOGACION"
) {
    fun isValid(): Boolean {
        // Ticket is valid if current time is less than expiration minus 5 minutes safety buffer
        val bufferMillis = 5 * 60 * 1000L
        return System.currentTimeMillis() < (expirationTimeMillis - bufferMillis) && token.isNotBlank() && sign.isNotBlank()
    }
}
