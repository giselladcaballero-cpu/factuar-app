package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.AuditLogDao
import com.example.data.local.dao.ConfigDao
import com.example.data.local.dao.InvoiceDao
import com.example.data.local.dao.PaymentDao
import com.example.data.local.entity.ArcaConfigEntity
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.AuthTicketEntity
import com.example.data.local.entity.InvoiceEntity
import com.example.data.local.entity.PaymentEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        PaymentEntity::class,
        InvoiceEntity::class,
        ArcaConfigEntity::class,
        AuthTicketEntity::class,
        AuditLogEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun paymentDao(): PaymentDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun configDao(): ConfigDao
    abstract fun auditLogDao(): AuditLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Drop and recreate or add missing columns gracefully
                db.execSQL("DROP TABLE IF EXISTS arca_config")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS arca_config (
                        id INTEGER PRIMARY KEY NOT NULL,
                        cuitEmisor INTEGER NOT NULL,
                        razonSocial TEXT NOT NULL,
                        domicilioFiscal TEXT NOT NULL,
                        inicioActividades TEXT NOT NULL,
                        condicionIvaEmisor TEXT NOT NULL,
                        puntoVenta INTEGER NOT NULL,
                        environment TEXT NOT NULL,
                        certCrtPem TEXT NOT NULL,
                        privateKeyPem TEXT NOT NULL,
                        isArcaConnected INTEGER NOT NULL,
                        arcaAlias TEXT NOT NULL,
                        onboardingMode TEXT NOT NULL,
                        isMpConnected INTEGER NOT NULL,
                        mpUserName TEXT NOT NULL,
                        mpCollectorId INTEGER NOT NULL,
                        mpAccessToken TEXT NOT NULL,
                        mpPublicKey TEXT NOT NULL,
                        mpWebhookSecret TEXT NOT NULL,
                        autoInvoiceEnabled INTEGER NOT NULL,
                        defaultConcepto INTEGER NOT NULL,
                        defaultAlicuotaIva REAL NOT NULL,
                        minAmountRequiresDoc REAL NOT NULL,
                        webhookRelayUrl TEXT NOT NULL,
                        lastSyncTimestamp INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "factuar_v2.db"
                )
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigration(dropAllTables = true)
                .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Seed initial config
                        CoroutineScope(Dispatchers.IO).launch {
                            val configDao = getDatabase(context).configDao()
                            if (configDao.getConfig() == null) {
                                configDao.insertOrUpdateConfig(ArcaConfigEntity())
                            }
                        }
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
