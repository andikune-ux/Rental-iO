package dev.andikune.rentalio.utils

import android.content.Context
import java.io.PrintWriter
import java.io.StringWriter

object CrashHandler {
    private const val PREFS_NAME = "crash_prefs"
    private const val KEY_CRASH_LOG = "last_crash_log"

    fun initialize(context: Context) {
        val appContext = context.applicationContext
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = StringWriter()
                val pw = PrintWriter(sw)
                throwable.printStackTrace(pw)
                val stackTrace = sw.toString()

                // Simpan secara instan ke SharedPreferences (synchronous commit)
                appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putString(KEY_CRASH_LOG, stackTrace)
                    .commit()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            // Lanjutkan crash normal agar aplikasi tertutup
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    fun getAndClearCrashLog(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val log = prefs.getString(KEY_CRASH_LOG, null)
        if (log != null) {
            // Hapus log setelah dibaca agar tidak muncul berulang kali
            prefs.edit().remove(KEY_CRASH_LOG).apply()
        }
        return log
    }
}
