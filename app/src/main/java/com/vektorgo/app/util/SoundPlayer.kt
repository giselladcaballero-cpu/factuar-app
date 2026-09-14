package com.vektorgo.app.util

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * Short, synthesized confirmation tones for app-open and invoice-emitted
 * feedback — generated on the fly instead of playing the user's own
 * (unpredictable, possibly long/musical) system notification ringtone, so
 * it reads as app feedback rather than a phone notification. Fails
 * silently — a missing sound must never interrupt billing.
 */
object SoundPlayer {

    /** Single short beep on app open. */
    fun playAppOpen() = playTone(ToneGenerator.TONE_PROP_BEEP, durationMs = 150)

    /** Bright two-tone "confirm" chime when a comprobante gets its CAE. */
    fun playInvoiceEmitted() = playTone(ToneGenerator.TONE_CDMA_CONFIRM, durationMs = 400)

    private fun playTone(tone: Int, durationMs: Int) {
        try {
            val generator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
            generator.startTone(tone, durationMs)
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    generator.release()
                } catch (e: Exception) {
                    Log.w("SoundPlayer", "Could not release tone generator: ${e.message}")
                }
            }, durationMs + 100L)
        } catch (e: Exception) {
            Log.w("SoundPlayer", "Could not play tone: ${e.message}")
        }
    }
}
