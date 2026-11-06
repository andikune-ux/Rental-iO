package dev.andikune.rentalio.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object UpdateManager {
    
    fun checkAndRequestStoragePermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.addCategory("android.intent.category.DEFAULT")
                    intent.data = Uri.parse(String.format("package:%s", context.packageName))
                    context.startActivity(intent)
                } catch (e: Exception) {
                    val intent = Intent()
                    intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                    context.startActivity(intent)
                }
            }
        }
    }

    suspend fun downloadApk(context: Context, apkUrl: String, versionName: String) {
        withContext(Dispatchers.IO) {
            val fileName = "RentalIO-v$versionName.apk"
            val folder = File(Environment.getExternalStorageDirectory(), "Rental-iO/Pembaruan Aplikasi")
            
            if (!folder.exists()) {
                folder.mkdirs()
            }

            val file = File(folder, fileName)

            try {
                val url = URL(apkUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connect()

                FileOutputStream(file).use { outputStream ->
                    connection.inputStream.copyTo(outputStream)
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Berhasil diunduh ke:\n${file.absolutePath}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Gagal mengunduh: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
