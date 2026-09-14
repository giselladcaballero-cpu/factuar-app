package com.vektorgo.app.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.util.Log

/**
 * Plays short system sounds for app-open and invoice-emitted feedback.
 * Uses the device's default notification/event tones instead of bundling
 * audio assets, and fails silently — a missing sound must never interrupt
 * billing.
 */
object SoundPlayer {

    fun playAppOpen(context: Context) = playSystemSound(context, RingtoneManager.TYPE_NOTIFICATION)

    fun playInvoiceEmitted(context: Context) = playSystemSound(context, RingtoneManager.TYPE_NOTIFICATION)

    private fun playSystemSound(context: Context, type: Int) {
        try {
            val uri = RingtoneManager.getActualDefaultRingtoneUri(context, type)
                ?: RingtoneManager.getDefaultUri(type)
                ?: return
            val player = MediaPlayer()
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            player.setDataSource(context, uri)
            player.setOnCompletionListener { it.release() }
            player.setOnErrorListener { mp, _, _ -> mp.release(); true }
            player.prepare()
            player.start()
        } catch (e: Exception) {
            Log.w("SoundPlayer", "Could not play sound: ${e.message}")
        }
    }
}
