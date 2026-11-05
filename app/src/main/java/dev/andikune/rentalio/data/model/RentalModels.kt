package dev.andikune.rentalio.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

enum class UserRole {
    OWNER,
    ADMIN,
    USER
}

data class Owner(
    val ownerId: String = "",
    val name: String = "Master Owner",
    val email: String = "owner@rental.io",
    val createdAt: Long = System.currentTimeMillis()
)

data class Admin(
    val adminId: String = "",
    val ownerId: String = "",
    val name: String = "Admin Rental",
    val status: String = "ACTIVE", // ACTIVE | SUSPENDED
    val licenseExpiry: Long = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000)
)

data class DeviceSession(
    val deviceId: String = "",
    val adminId: String = "",
    val isLocked: Boolean = false,
    val remainingSeconds: Long = 3600L,
    val whitelistApps: List<String> = emptyList(),
    val mode: String = "OFFLINE_FALLBACK", // CLOUD_FIRESTORE | OFFLINE_FALLBACK
    val lastUpdatedBy: String = "SYSTEM",
    val lastTimestamp: Long = System.currentTimeMillis()
)

data class WhitelistApp(
    val packageName: String,
    val label: String,
    val category: String = "Game",
    val isInstalled: Boolean = true,
    val launchIntentAvailable: Boolean = true
)

@JsonClass(generateAdapter = true)
data class GitHubRelease(
    @Json(name = "tag_name") val tagName: String = "",
    @Json(name = "name") val name: String = "",
    @Json(name = "body") val body: String = "",
    @Json(name = "html_url") val htmlUrl: String = "",
    @Json(name = "published_at") val publishedAt: String = "",
    @Json(name = "assets") val assets: List<GitHubReleaseAsset> = emptyList()
)

@JsonClass(generateAdapter = true)
data class GitHubReleaseAsset(
    @Json(name = "name") val name: String = "",
    @Json(name = "browser_download_url") val browserDownloadUrl: String = "",
    @Json(name = "size") val size: Long = 0L,
    @Json(name = "content_type") val contentType: String = ""
)

data class RentalUiState(
    val remainingSeconds: Long = 3600L,
    val isExpired: Boolean = false,
    val isLocked: Boolean = false,
    val isDeviceOwnerActive: Boolean = false,
    val isLockTaskModeActive: Boolean = false,
    val isSuspendedByOwner: Boolean = false,
    val selectedApp: WhitelistApp? = null,
    val whitelistApps: List<WhitelistApp> = emptyList(),
    val isCloudConnected: Boolean = false,
    val showAdminDialog: Boolean = false,
    val showUpdateDialog: Boolean = false,
    val updateRelease: GitHubRelease? = null,
    val downloadProgress: Int = -1,
    val errorMessage: String? = null
)
