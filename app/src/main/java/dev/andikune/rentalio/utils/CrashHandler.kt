package dev.andikune.rentalio.utils

import android.content.Context
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

object CrashHandler {
    private const val CRASH_FILE_NAME = "crash_log.txt"

    fun initialize(context: Context) {
        val appContext = context.applicationContext
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = StringWriter()
                val pw = PrintWriter(sw)
                throwable.printStackTrace(pw)
                val stackTrace = sw.toString()
                
                // Simpan ke file internal aplikasi
                val file = File(appContext.filesDir, CRASH_FILE_NAME)
                file.writeText(stackTrace)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            // Lanjutkan crash normal agar sistem tahu aplikasi berhenti
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    fun getAndClearCrashLog(context: Context): String? {
        val file = File(context.filesDir, CRASH_FILE_NAME)
        return if (file.exists()) {
            val log = file.readText()
            file.delete() // Hapus agar tidak muncul berulang
            log
        } else {
            null
        }
    }
}
