package com.vektorgo.app.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom

/**
 * Generates (once) and retrieves the passphrase used to encrypt the local
 * SQLCipher database. The passphrase itself is stored in
 * EncryptedSharedPreferences, whose key is held in the Android Keystore —
 * it never leaves the device and is not extractable even with root, unlike
 * the plaintext SQLite file it used to protect nothing before this.
 */
object DatabasePassphrase {
    private const val PREFS_NAME = "vektor_go_secure_prefs"
    private const val KEY_DB_PASSPHRASE = "db_passphrase"
    private const val PASSPHRASE_BYTES = 32

    fun getOrCreate(context: Context): ByteArray {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val prefs = EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        val existing = prefs.getString(KEY_DB_PASSPHRASE, null)
        if (existing != null) {
            return android.util.Base64.decode(existing, android.util.Base64.NO_WRAP)
        }

        val newPassphrase = ByteArray(PASSPHRASE_BYTES).also { SecureRandom().nextBytes(it) }
        prefs.edit()
            .putString(KEY_DB_PASSPHRASE, android.util.Base64.encodeToString(newPassphrase, android.util.Base64.NO_WRAP))
            .apply()
        return newPassphrase
    }
}
