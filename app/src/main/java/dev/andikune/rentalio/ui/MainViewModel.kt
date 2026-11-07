package dev.andikune.rentalio.ui

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.andikune.rentalio.data.local.RentalPreferences
import dev.andikune.rentalio.data.model.RentalUiState
import dev.andikune.rentalio.data.model.WhitelistApp
import dev.andikune.rentalio.data.repository.RentalRepository
import dev.andikune.rentalio.service.KioskForegroundService
import dev.andikune.rentalio.service.UpdateManager
import dev.andikune.rentalio.utils.DeviceOwnerManager
import dev.andikune.rentalio.utils.SecurityHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RentalRepository(application)
    private val updateManager = UpdateManager(application)
    val deviceOwnerManager = DeviceOwnerManager(application)
    val preferences = RentalPreferences(application)

    private val _uiState = MutableStateFlow(RentalUiState())
    val uiState: StateFlow<RentalUiState> = _uiState.asStateFlow()

    private val _allDeviceApps = MutableStateFlow<List<WhitelistApp>>(emptyList())
    val allDeviceApps: StateFlow<List<WhitelistApp>> = _allDeviceApps.asStateFlow()

    init {
        initData()
        observeSettings()
        observeRentalState()
        observeUpdateProgress()
    }

    private fun initData() {
        viewModelScope.launch {
            repository.initializeIfNeeded()
            loadAllInstalledApps()
            checkDeviceOwnerStatus()
            checkForUpdates(silent = true)
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            preferences.settingsFlow.collect { s ->
                _uiState.update { current ->
                    current.copy(
                        rentalName = s.rentalName,
                        phoneLabel = s.phoneLabel,
                        isUnlimitedMode = s.isUnlimitedMode,
                        packageHours = s.packageHours,
                        packageMinutes = s.packageMinutes,
                        bonusMinutes = s.bonusMinutes,
                        bannerTexts = listOf(s.bannerText1, s.bannerText2, s.bannerText3)
                    )
                }
                filterWhitelistApps(s.whitelistPackages)
            }
        }
    }

    private fun observeRentalState() {
        viewModelScope.launch {
            repository.rentalStateFlow.collect { localState ->
                if (localState != null) {
                    val isUnlimited = preferences.getSettings().isUnlimitedMode
                    val isExpired = !isUnlimited && (localState.remainingTimeSeconds <= 0L || localState.isExpired)

                    _uiState.update { current ->
                        if (isUnlimited) {
                            current.copy(
                                unlimitedElapsedSeconds = localState.remainingTimeSeconds,
                                isExpired = false,
                                isSuspendedByOwner = localState.isSuspended
                            )
                        } else {
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

    fun loadAllInstalledApps() {
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
        for (game in defaultGames) {
            val isInstalled = try {
                packageManager.getPackageInfo(game.packageName, 0)
                true
            } catch (_: Exception) {
                false
            }
            installedList.add(game.copy(isInstalled = isInstalled))
        }

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
                            category = "Aplikasi",
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

        _allDeviceApps.value = sortedList
        filterWhitelistApps(preferences.getSettings().whitelistPackages)
    }

    private fun filterWhitelistApps(whitelistPackages: Set<String>) {
        val allApps = _allDeviceApps.value
        val filtered = allApps.filter { whitelistPackages.contains(it.packageName) }
        val effectiveList = if (filtered.isNotEmpty()) filtered else allApps

        _uiState.update {
            it.copy(
                whitelistApps = effectiveList,
                selectedApp = effectiveList.firstOrNull { a -> a.isInstalled } ?: effectiveList.firstOrNull()
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

        val isWhitelisted = preferences.getSettings().whitelistPackages.contains(app.packageName)
        if (!isWhitelisted && _allDeviceApps.value.any { preferences.getSettings().whitelistPackages.contains(it.packageName) }) {
            Toast.makeText(context, "Aplikasi ini diblokir oleh Admin Kiosk!", Toast.LENGTH_SHORT).show()
            return
        }

        val packageManager = context.packageManager
        val launchIntent = packageManager.getLaunchIntentForPackage(app.packageName)
        
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
        } else {
            // FIX BUG KIOSK: Jangan pernah buka Play Store. Blokir total!
            Toast.makeText(context, "Aplikasi tidak terpasang atau diblokir oleh sistem Kiosk!", Toast.LENGTH_SHORT).show()
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
            KioskForegroundService.setTime(getApplication(), seconds)
            _uiState.update { it.copy(errorMessage = null) }
        }
    }

    fun savePackageDuration(hours: Int, minutes: Int, seconds: Int, bonusMinutes: Int, isUnlimited: Boolean) {
        viewModelScope.launch {
            preferences.savePackageTime(hours, minutes, bonusMinutes)
            preferences.saveUnlimitedMode(isUnlimited)

            if (isUnlimited) {
                repository.setRemainingSeconds(0L)
                KioskForegroundService.setTime(getApplication(), 0L)
            } else {
                val totalSeconds = (hours * 3600L) + (minutes * 60L) + seconds + (bonusMinutes * 60L)
                repository.setRemainingSeconds(totalSeconds)
                KioskForegroundService.setTime(getApplication(), totalSeconds)
            }
            _uiState.update { it.copy(showAdminDialog = false) }
        }
    }

    fun saveIdentityAndBanners(rentalName: String, phoneLabel: String, b1: String, b2: String, b3: String, autoStart: Boolean) {
        preferences.saveRentalName(rentalName)
        preferences.savePhoneLabel(phoneLabel)
        preferences.saveBannerTexts(b1, b2, b3)
        preferences.saveAutoStart(autoStart)
        _uiState.update {
            it.copy(
                rentalName = rentalName,
                phoneLabel = phoneLabel,
                bannerTexts = listOf(b1, b2, b3),
                showAdminDialog = false
            )
        }
    }

    fun saveWhitelist(packages: Set<String>) {
        viewModelScope.launch {
            preferences.saveWhitelistPackages(packages)
            repository.updateWhitelistPackages(packages.toList())
            filterWhitelistApps(packages)
            if (deviceOwnerManager.isDeviceOwner()) {
                deviceOwnerManager.configureKioskPolicies(packages.toList())
            }
            _uiState.update { it.copy(showAdminDialog = false) }
        }
    }

    fun verifyAdminPin(pin: String): Boolean {
        val s = preferences.getSettings()
        return pin == s.adminPin
    }

    fun unlockWithPin(pin: String) {
        viewModelScope.launch {
            val isValid = verifyAdminPin(pin)
            if (isValid) {
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
            preferences.saveAdminPin(newPin)
            repository.updateAdminPin(newPin)
        }
    }

    fun updateBatteryTelemetry(level: Int, isCharging: Boolean, tempCelsius: Float, networkStatus: String) {
        val isOverheated = tempCelsius >= 45.0f
        val isLow = level <= 10 && !isCharging

        _uiState.update {
            it.copy(
                batteryLevel = level,
                isCharging = isCharging,
                batteryTemperature = tempCelsius,
                isOverheated = isOverheated,
                isBatteryLow = isLow,
                networkStatus = networkStatus
            )
        }
    }

    fun finishUnlimitedSession() {
        val elapsed = _uiState.value.unlimitedElapsedSeconds
        val cost = SecurityHelper.calculateUnlimitedCost(elapsed)
        _uiState.update {
            it.copy(
                showUnlimitedSummaryDialog = true,
                unlimitedSummarySeconds = elapsed,
                unlimitedSummaryCost = cost
            )
        }
    }

    fun dismissUnlimitedSummary() {
        viewModelScope.launch {
            repository.setRemainingSeconds(0L)
            KioskForegroundService.setTime(getApplication(), 0L)
            _uiState.update {
                it.copy(
                    showUnlimitedSummaryDialog = false,
                    unlimitedElapsedSeconds = 0L
                )
            }
        }
    }

    fun toggleLockTask(enable: Boolean, activity: Activity) {
        if (enable) {
            val whitelisted = preferences.getSettings().whitelistPackages.toList()
            deviceOwnerManager.configureKioskPolicies(whitelisted)
            val success = deviceOwnerManager.startLockTask(activity, whitelisted)
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
