package dev.andikune.rentalio.utils

import android.content.Context
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

object CrashHandler {
    private const val CRASH_FILE_NAME = "crash_log.txt"

    fun initialize(context: Context) {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = StringWriter()
                val pw = PrintWriter(sw)
                throwable.printStackTrace(pw)
                val stackTrace = sw.toString()
                
                val file = File(context.filesDir, CRASH_FILE_NAME)
                file.writeText(stackTrace)
            } catch (e: Exception) {
                // Abaikan jika gagal menyimpan
            }
            // Lanjutkan crash normal
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    fun getAndClearCrashLog(context: Context): String? {
        val file = File(context.filesDir, CRASH_FILE_NAME)
        if (file.exists()) {
            val log = file.readText()
            file.delete() // Hapus setelah dibaca agar tidak muncul lagi
            return log
        }
        return null
    }
}
