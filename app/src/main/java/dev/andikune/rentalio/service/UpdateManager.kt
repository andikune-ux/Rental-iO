package dev.andikune.rentalio.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import dev.andikune.rentalio.BuildConfig
import dev.andikune.rentalio.data.model.GitHubRelease
import dev.andikune.rentalio.data.remote.GitHubReleaseService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class UpdateManager(
    private val context: Context,
    private val releaseService: GitHubReleaseService = GitHubReleaseService.create()
) {
    private val _downloadProgress = MutableStateFlow<Int>(-1) // -1: idle, 0-100: downloading, 100: finished
    val downloadProgress: StateFlow<Int> = _downloadProgress.asStateFlow()

    private val _statusMessage = MutableStateFlow<String>("")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    suspend fun checkLatestRelease(): GitHubRelease? = withContext(Dispatchers.IO) {
        try {
            _statusMessage.value = "Memeriksa rilis terbaru..."
            val release = releaseService.getLatestRelease()
            val currentVersion = BuildConfig.VERSION_NAME
            val remoteTag = release.tagName.trimStart('v', 'V')
            val localTag = currentVersion.trimStart('v', 'V')

            Log.d("UpdateManager", "Local version: $localTag, Remote version: $remoteTag")
            if (isNewerVersion(remoteTag, localTag)) {
                _statusMessage.value = "Versi baru tersedia: ${release.tagName}"
                release
            } else {
                _statusMessage.value = "Aplikasi sudah menggunakan versi terbaru ($currentVersion)."
                null
            }
        } catch (e: Exception) {
            Log.w("UpdateManager", "Gagal memeriksa rilis GitHub: ${e.message}")
            _statusMessage.value = "Gagal terhubung ke GitHub Releases: ${e.localizedMessage}"
            null
        }
    }

    private fun isNewerVersion(remote: String, local: String): Boolean {
        if (remote == local) return false
        val remoteParts = remote.split(".").mapNotNull { it.toIntOrNull() }
        val localParts = local.split(".").mapNotNull { it.toIntOrNull() }

        val length = maxOf(remoteParts.size, localParts.size)
        for (i in 0 until length) {
            val r = remoteParts.getOrElse(i) { 0 }
            val l = localParts.getOrElse(i) { 0 }
            if (r > l) return true
            if (r < l) return false
        }
        return false
    }

    suspend fun downloadAndInstallApk(release: GitHubRelease): Boolean = withContext(Dispatchers.IO) {
        val apkAsset = release.assets.firstOrNull {
            it.name.endsWith(".apk", ignoreCase = true)
        } ?: release.assets.firstOrNull { it.contentType.contains("android.package", ignoreCase = true) }

        if (apkAsset == null || apkAsset.browserDownloadUrl.isBlank()) {
            _statusMessage.value = "Tidak ditemukan file APK pada rilis ${release.tagName}."
            _downloadProgress.value = -1
            return@withContext false
        }

        try {
            _statusMessage.value = "Mengunduh ${apkAsset.name}..."
            _downloadProgress.value = 0

            val response = releaseService.downloadFile(apkAsset.browserDownloadUrl)
            val body = response.body()
            if (!response.isSuccessful || body == null) {
                _statusMessage.value = "Gagal mengunduh file update (HTTP ${response.code()})."
                _downloadProgress.value = -1
                return@withContext false
            }

            val baseDir = File(context.getExternalFilesDir(null), "rentalio/pembaruan aplikasi/apk")
            if (!baseDir.exists() && !baseDir.mkdirs()) {
                _statusMessage.value = "Tidak bisa membuat folder unduhan."
                _downloadProgress.value = -1
                return@withContext false
            }
            val destinationFile = File(baseDir, "app-release.apk")
            if (destinationFile.exists()) {
                destinationFile.delete()
            }

            val totalBytes = body.contentLength()
            var downloadedBytes = 0L
            var lastProgress = -1

            body.byteStream().use { inputStream ->
                FileOutputStream(destinationFile).use { outputStream ->
                    val buffer = ByteArray(16 * 1024)
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        if (totalBytes > 0) {
                            val progress = ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100)
                            if (progress != lastProgress) {
                                lastProgress = progress
                                _downloadProgress.value = progress
                            }
                        }
                    }
                    outputStream.flush()
                }
            }

            if (!destinationFile.exists() || destinationFile.length() <= 0L) {
                _statusMessage.value = "File APK kosong. Unduhan dibatalkan."
                _downloadProgress.value = -1
                return@withContext false
            }

            val archiveInfo = context.packageManager.getPackageArchiveInfo(destinationFile.absolutePath, 0)
            if (archiveInfo == null) {
                _statusMessage.value = "File yang diunduh bukan APK valid."
                _downloadProgress.value = -1
                destinationFile.delete()
                return@withContext false
            }

            _downloadProgress.value = 100
            _statusMessage.value = "Unduhan selesai. Memulai instalasi..."
            launchInstallIntent(destinationFile)
            true
        } catch (e: Exception) {
            Log.e("UpdateManager", "Download error: ${e.message}", e)
            _statusMessage.value = "Error saat mengunduh: ${e.localizedMessage}"
            _downloadProgress.value = -1
            false
        }
    }

    private fun canInstallPackages(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    private fun openUnknownSourcesSettings() {
        try {
            val intent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            _statusMessage.value = "Izinkan \"Pasang aplikasi tidak dikenal\" lalu tekan Update lagi."
        } catch (e: Exception) {
            Log.w("UpdateManager", "Tidak bisa membuka pengaturan sumber tidak dikenal: ${e.message}")
            _statusMessage.value = "Aktifkan izin pemasangan APK di Pengaturan > Keamanan."
        }
    }

    private fun launchInstallIntent(apkFile: File) {
        if (!canInstallPackages()) {
            openUnknownSourcesSettings()
            return
        }
        try {
            val authority = "${context.packageName}.fileprovider"
            val contentUri: Uri = FileProvider.getUriForFile(context, authority, apkFile)

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(installIntent)
        } catch (e: Exception) {
            Log.e("UpdateManager", "Gagal membuka FileProvider installer: ${e.message}", e)
            _statusMessage.value = "Gagal memasang APK: ${e.message}"
        }
    }
}
