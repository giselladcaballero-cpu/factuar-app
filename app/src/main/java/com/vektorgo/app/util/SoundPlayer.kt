package com.vektorgo.app.util

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Short, synthesized chime sounds for app-open and invoice-emitted
 * feedback. Built as sine-wave "bell" notes (quick attack, exponential
 * decay) on the fly instead of a bundled audio asset or the user's own
 * unpredictable system notification tone. Fails silently — a missing
 * sound must never interrupt billing.
 */
object SoundPlayer {

    private const val SAMPLE_RATE = 44100

    /** Quick two-note "ding-ding" on app open. */
    fun playAppOpen() = playMelody(listOf(Note(1046.5, 90), Note(1568.0, 140)))

    /** Ascending "cha-ching" arpeggio when a comprobante gets its CAE. */
    fun playInvoiceEmitted() = playMelody(
        listOf(
            Note(523.25, 100),  // C5
            Note(659.25, 100),  // E5
            Note(783.99, 100),  // G5
            Note(1046.5, 260)   // C6, held
        )
    )

    private data class Note(val hz: Double, val durationMs: Int)

    private fun playMelody(notes: List<Note>) {
        try {
            val samples = buildMelodyPcm(notes)
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(samples.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            track.write(samples, 0, samples.size)
            track.setNotificationMarkerPosition(samples.size)
            track.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
                override fun onMarkerReached(t: AudioTrack) {
                    try {
                        t.release()
                    } catch (e: Exception) {
                        Log.w("SoundPlayer", "Could not release AudioTrack: ${e.message}")
                    }
                }
                override fun onPeriodicNotification(t: AudioTrack) {}
            })
            track.play()
        } catch (e: Exception) {
            Log.w("SoundPlayer", "Could not play melody: ${e.message}")
        }
    }

    /**
     * Renders each note as a sine wave with a fast attack and exponential
     * decay envelope, so it reads as a bell/chime "ding" rather than a flat
     * telephony beep.
     */
    private fun buildMelodyPcm(notes: List<Note>): ShortArray {
        val totalSamples = notes.sumOf { (SAMPLE_RATE * it.durationMs / 1000.0).toInt() }
        val out = ShortArray(totalSamples)
        var offset = 0
        for (note in notes) {
            val n = (SAMPLE_RATE * note.durationMs / 1000.0).toInt()
            val attackSamples = (SAMPLE_RATE * 0.005).toInt().coerceAtLeast(1)
            for (i in 0 until n) {
                val t = i / SAMPLE_RATE.toDouble()
                val attack = (i.toDouble() / attackSamples).coerceIn(0.0, 1.0)
                val decay = exp(-3.5 * i / n)
                val envelope = attack * decay
                val sample = sin(2.0 * PI * note.hz * t) * envelope
                out[offset + i] = (sample * Short.MAX_VALUE * 0.7).toInt().toShort()
            }
            offset += n
        }
        return out
    }
}
