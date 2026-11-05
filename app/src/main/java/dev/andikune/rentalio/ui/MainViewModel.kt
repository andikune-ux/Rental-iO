package dev.andikune.rentalio.ui

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.andikune.rentalio.data.model.RentalUiState
import dev.andikune.rentalio.data.model.WhitelistApp
import dev.andikune.rentalio.data.repository.RentalRepository
import dev.andikune.rentalio.service.KioskForegroundService
import dev.andikune.rentalio.service.UpdateManager
import dev.andikune.rentalio.utils.DeviceOwnerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RentalRepository(application)
    private val updateManager = UpdateManager(application)
    val deviceOwnerManager = DeviceOwnerManager(application)

    private val _uiState = MutableStateFlow(RentalUiState())
    val uiState: StateFlow<RentalUiState> = _uiState.asStateFlow()

    init {
        initData()
        observeRentalState()
        observeUpdateProgress()
    }

    private fun initData() {
        viewModelScope.launch {
            repository.initializeIfNeeded()
            loadWhitelistedApps()
            checkDeviceOwnerStatus()
            // Cold start update check
            checkForUpdates(silent = true)
        }
    }

    private fun observeRentalState() {
        viewModelScope.launch {
            repository.rentalStateFlow.collect { localState ->
                if (localState != null) {
                    val isExpired = localState.remainingTimeSeconds <= 0L || localState.isExpired
                    _uiState.update { current ->
                        current.copy(
                            remainingSeconds = localState.remainingTimeSeconds,
                            isExpired = isExpired,
                            isSuspendedByOwner = localState.isSuspended
                        )
                    }
                }
            }
        }
    }

    private fun observeUpdateProgress() {
        viewModelScope.launch {
            updateManager.downloadProgress.collect { progress ->
                _uiState.update { it.copy(downloadProgress = progress) }
            }
        }
    }

    fun checkDeviceOwnerStatus() {
        val isOwner = deviceOwnerManager.isDeviceOwner()
        _uiState.update { it.copy(isDeviceOwnerActive = isOwner) }
    }

    fun loadWhitelistedApps() {
        val packageManager = getApplication<Application>().packageManager
        val defaultGames = listOf(
            WhitelistApp("com.dts.freefireth", "Free Fire MAX", "Battle Royale"),
            WhitelistApp("com.mobile.legends", "Mobile Legends: Bang Bang", "MOBA"),
            WhitelistApp("com.tencent.ig", "PUBG Mobile", "Battle Royale"),
            WhitelistApp("com.miHoYo.GenshinImpact", "Genshin Impact", "Open World RPG"),
            WhitelistApp("com.activision.callofduty.shooter", "Call of Duty: Mobile", "FPS Action"),
            WhitelistApp("com.ea.gp.apexlegendsmobilefps", "Apex Legends Mobile", "Hero Shooter"),
            WhitelistApp("com.roblox.client", "Roblox", "Sandbox Universe"),
            WhitelistApp("com.gameloft.android.ANMP.GloftA9HM", "Asphalt 9: Legends", "Racing")
        )

        val installedList = mutableListOf<WhitelistApp>()
        // First check our popular predefined games
        for (game in defaultGames) {
            val isInstalled = try {
                packageManager.getPackageInfo(game.packageName, 0)
                true
            } catch (_: Exception) {
                false
            }
            installedList.add(game.copy(isInstalled = isInstalled))
        }

        // Also query launchable apps on the device (excluding ourselves)
        try {
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolvedApps = packageManager.queryIntentActivities(mainIntent, 0)
            for (resolveInfo in resolvedApps) {
                val pkgName = resolveInfo.activityInfo.packageName
                if (pkgName != getApplication<Application>().packageName &&
                    defaultGames.none { it.packageName == pkgName }) {
                    val label = resolveInfo.loadLabel(packageManager).toString()
                    installedList.add(
                        WhitelistApp(
                            packageName = pkgName,
                            label = label,
                            category = "App / Game",
                            isInstalled = true
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.w("MainViewModel", "Failed to query launchable apps: ${e.message}")
        }

        val sortedList = installedList.sortedWith(
            compareByDescending<WhitelistApp> { it.isInstalled }
                .thenBy { it.label }
        )

        _uiState.update {
            it.copy(
                whitelistApps = sortedList,
                selectedApp = sortedList.firstOrNull { app -> app.isInstalled } ?: sortedList.firstOrNull()
            )
        }
    }

    fun selectApp(app: WhitelistApp) {
        _uiState.update { it.copy(selectedApp = app) }
    }

    fun launchSelectedApp(context: Context) {
        val app = _uiState.value.selectedApp ?: return
        if (_uiState.value.isExpired) {
            Toast.makeText(context, "Masa sewa telah habis! Hubungi admin.", Toast.LENGTH_SHORT).show()
            return
        }

        val packageManager = context.packageManager
        val launchIntent = packageManager.getLaunchIntentForPackage(app.packageName)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
        } else {
            // If game is not installed on physical device, offer to install via Play Store
            try {
                val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${app.packageName}")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(marketIntent)
            } catch (_: Exception) {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${app.packageName}")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
            }
        }
    }

    fun addRentalTime(seconds: Long) {
        viewModelScope.launch {
            repository.addRentalTime(seconds)
            KioskForegroundService.addTime(getApplication(), seconds)
            _uiState.update { it.copy(errorMessage = null) }
        }
    }

    fun setRentalTime(seconds: Long) {
        viewModelScope.launch {
            repository.setRemainingSeconds(seconds)
            _uiState.update { it.copy(errorMessage = null) }
        }
    }

    fun unlockWithPin(pin: String) {
        viewModelScope.launch {
            val isValid = repository.verifyAdminPin(pin)
            if (isValid) {
                // Add 1 hour default on successful unlock from expired screen
                repository.addRentalTime(3600L)
                KioskForegroundService.dismissAlarm(getApplication())
                KioskForegroundService.addTime(getApplication(), 3600L)
                _uiState.update {
                    it.copy(
                        isExpired = false,
                        errorMessage = null
                    )
                }
            } else {
                _uiState.update {
                    it.copy(errorMessage = "PIN Admin Salah. Coba lagi!")
                }
            }
        }
    }

    fun updateAdminPin(newPin: String) {
        viewModelScope.launch {
            repository.updateAdminPin(newPin)
        }
    }

    fun toggleLockTask(enable: Boolean, activity: Activity) {
        if (enable) {
            val success = deviceOwnerManager.startLockTask(activity)
            _uiState.update { it.copy(isLockTaskModeActive = success) }
        } else {
            val success = deviceOwnerManager.stopLockTask(activity)
            _uiState.update { it.copy(isLockTaskModeActive = !success) }
        }
    }

    fun checkForUpdates(silent: Boolean = false) {
        viewModelScope.launch {
            val release = updateManager.checkLatestRelease()
            if (release != null) {
                _uiState.update {
                    it.copy(
                        updateRelease = release,
                        showUpdateDialog = true
                    )
                }
            } else if (!silent) {
                Toast.makeText(getApplication(), "Aplikasi Rental.iO sudah versi terbaru.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun downloadAndInstallUpdate() {
        val release = _uiState.value.updateRelease ?: return
        viewModelScope.launch {
            updateManager.downloadAndInstallApk(release)
        }
    }

    fun dismissUpdateDialog() {
        _uiState.update { it.copy(showUpdateDialog = false) }
    }

    fun openAdminDialog() {
        _uiState.update { it.copy(showAdminDialog = true) }
    }

    fun dismissAdminDialog() {
        _uiState.update { it.copy(showAdminDialog = false) }
    }
}
