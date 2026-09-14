package com.vektorgo.app

import android.app.Application
import android.content.Context

/**
 * Catches crashes that happen before any UI can render (DB init, native
 * library loading, etc.) and saves the stack trace so MainActivity can show
 * it on the next launch instead of the app just silently closing with
 * nothing to debug from.
 */
class VektorGoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val trace = android.util.Log.getStackTraceString(throwable)
                getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putString(KEY_LAST_CRASH, "${throwable.message}\n\n$trace")
                    .apply()
            } catch (e: Exception) {
                // If we can't even save the crash, fall through to the
                // default handler below -- never swallow the original crash.
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    companion object {
        private const val PREFS_NAME = "vektor_go_crash_prefs"
        private const val KEY_LAST_CRASH = "last_crash"

        fun getLastCrash(context: Context): String? {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_LAST_CRASH, null)
        }

        fun clearLastCrash(context: Context) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_LAST_CRASH)
                .apply()
        }
    }
}
