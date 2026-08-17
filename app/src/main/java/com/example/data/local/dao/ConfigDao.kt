package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.ArcaConfigEntity
import com.example.data.local.entity.AuthTicketEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConfigDao {
    @Query("SELECT * FROM arca_config WHERE id = 1 LIMIT 1")
    fun getConfigFlow(): Flow<ArcaConfigEntity?>

    @Query("SELECT * FROM arca_config WHERE id = 1 LIMIT 1")
    suspend fun getConfig(): ArcaConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateConfig(config: ArcaConfigEntity)

    @Update
    suspend fun updateConfig(config: ArcaConfigEntity)

    // Auth Tickets (WSAA)
    @Query("SELECT * FROM auth_tickets WHERE service = :service LIMIT 1")
    suspend fun getAuthTicket(service: String = "wsfe"): AuthTicketEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAuthTicket(ticket: AuthTicketEntity)

    @Query("DELETE FROM auth_tickets WHERE service = :service")
    suspend fun deleteAuthTicket(service: String = "wsfe")
}
