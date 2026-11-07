package dev.andikune.rentalio.ui

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.andikune.rentalio.data.local.RentalPreferences
import dev.andikune.rentalio.data.model.RentalUiState
import dev.andikune.rentalio.data.model.WhitelistApp
import dev.andikune.rentalio.data.repository.RentalRepository
import dev.andikune.rentalio.data.timer.TimerBus
import dev.andikune.rentalio.service.KioskForegroundService
import dev.andikune.rentalio.service.UpdateManager
import dev.andikune.rentalio.utils.DeviceOwnerManager
import dev.andikune.rentalio.utils.HomeLauncherManager
import dev.andikune.rentalio.utils.SecurityHelper
import dev.andikune.rentalio.utils.SystemOverlayHelper
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

    @Volatile
    private var localPinHash: String? = null

    @Volatile
    private var appliedWhitelist: Set<String> = emptySet()

    private var hasAutoStartedKiosk: Boolean = false

    init {
        initData()
        observeSettings()
        observeTimerBus()
        observeSuspension()
        observeUpdateProgress()
    }

    private fun initData() {
        viewModelScope.launch {
            repository.initializeIfNeeded()
            localPinHash = repository.getStoredPinHash()
            loadAllInstalledApps()
            refreshPermissionStatuses()
            startCloudSyncListener()
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

    /**
     * Sumber utama timer adalah TimerBus (dihitung dari epoch oleh service).
     * Room hanya dipakai untuk suspensi dan data pengaturan, sehingga UI tidak
     * bergantung pada tulisan database setiap detik.
     */
    private fun observeTimerBus() {
        viewModelScope.launch {
            TimerBus.state.collect { timer ->
                _uiState.update {
                    it.copy(
                        remainingSeconds = timer.remainingSeconds,
                        unlimitedElapsedSeconds = timer.elapsedSeconds,
                        isExpired = timer.isExpired,
                        isUnlimitedMode = timer.isUnlimited,
                        isSessionRunning = timer.isRunning
                    )
                }
            }
        }
    }

    private fun observeSuspension() {
        viewModelScope.launch {
            repository.rentalStateFlow.collect { localState ->
                if (localState != null) {
                    _uiState.update { it.copy(isSuspendedByOwner = localState.isSuspended) }
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

    private fun startCloudSyncListener() {
        repository.startCloudSyncListener { cloudSession ->
            viewModelScope.launch {
                repository.applyCloudUpdate(cloudSession)
                KioskForegroundService.refresh(getApplication())
                _uiState.update { it.copy(isCloudConnected = true) }
            }
        }
    }

    fun checkDeviceOwnerStatus() {
        refreshPermissionStatuses()
    }

    fun refreshPermissionStatuses() {
        val app = getApplication<Application>()
        _uiState.update {
            it.copy(
                isDeviceOwnerActive = deviceOwnerManager.isDeviceOwner(),
                isDefaultLauncher = HomeLauncherManager.isDefaultHome(app),
                isOverlayGranted = SystemOverlayHelper.canDrawOverlays(app)
            )
        }
    }

    fun loadAllInstalledApps() {
        val app = getApplication<Application>()
        val packageManager = app.packageManager
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
        val seenPackages = mutableSetOf<String>()

        for (game in defaultGames) {
            val isInstalled = try {
                packageManager.getPackageInfo(game.packageName, 0)
                true
            } catch (_: Exception) {
                false
            }
            installedList.add(game.copy(isInstalled = isInstalled))
            seenPackages.add(game.packageName)
        }

        try {
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolvedApps = packageManager.queryIntentActivities(mainIntent, 0)
            for (resolveInfo in resolvedApps) {
                val pkgName = resolveInfo.activityInfo.packageName
                if (pkgName == app.packageName || pkgName in seenPackages) continue
                val label = resolveInfo.loadLabel(packageManager).toString()
                installedList.add(
                    WhitelistApp(
                        packageName = pkgName,
                        label = label,
                        category = "Aplikasi",
                        isInstalled = true
                    )
                )
                seenPackages.add(pkgName)
            }
        } catch (e: Exception) {
            Log.w("MainViewModel", "Failed to query launchable apps: ${e.message}")
        }

        val sortedList = installedList.sortedWith(
            compareByDescending<WhitelistApp> { it.isInstalled }
                .thenBy { it.label.lowercase() }
        )

        _allDeviceApps.value = sortedList
        appliedWhitelist = emptySet()
        filterWhitelistApps(preferences.getSettings().whitelistPackages)
    }

    private fun filterWhitelistApps(whitelistPackages: Set<String>) {
        if (whitelistPackages == appliedWhitelist && _uiState.value.whitelistApps.isNotEmpty()) {
            return
        }
        appliedWhitelist = whitelistPackages

        val activeApps = _allDeviceApps.value.filter {
            it.isInstalled && whitelistPackages.contains(it.packageName)
        }

        _uiState.update { current ->
            val stillSelected = activeApps.firstOrNull { it.packageName == current.selectedApp?.packageName }
            current.copy(
                whitelistApps = activeApps,
                selectedApp = stillSelected ?: activeApps.firstOrNull()
            )
        }
    }

    fun selectApp(app: WhitelistApp) {
        _uiState.update { it.copy(selectedApp = app) }
    }

    fun launchSelectedApp(context: Context) {
        val state = _uiState.value
        val app = state.selectedApp
        if (app == null) {
            Toast.makeText(context, "Belum ada aplikasi yang diizinkan Admin.", Toast.LENGTH_SHORT).show()
            return
        }

        if (!state.isSessionRunning) {
            Toast.makeText(context, "Sesi sewa belum aktif. Hubungi Admin Rental.", Toast.LENGTH_SHORT).show()
            return
        }

        if (state.isExpired && !state.isUnlimitedMode) {
            Toast.makeText(context, "Masa sewa telah habis! Hubungi Admin.", Toast.LENGTH_SHORT).show()
            return
        }

        if (state.isSuspendedByOwner) {
            Toast.makeText(context, "Perangkat sedang disuspend oleh Owner.", Toast.LENGTH_SHORT).show()
            return
        }

        val whitelist = preferences.getSettings().whitelistPackages
        if (!whitelist.contains(app.packageName)) {
            Toast.makeText(context, "Aplikasi ini diblokir oleh Admin Kiosk!", Toast.LENGTH_SHORT).show()
            return
        }

        if (!app.isInstalled) {
            Toast.makeText(context, "Aplikasi belum terpasang di perangkat ini.", Toast.LENGTH_SHORT).show()
            return
        }

        val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
        } else {
            Toast.makeText(context, "Aplikasi tidak bisa dibuka (launcher tidak tersedia).", Toast.LENGTH_SHORT).show()
        }
    }

    fun addRentalTime(seconds: Long) {
        if (seconds <= 0L) return
        viewModelScope.launch {
            repository.addRentalTime(seconds)
            KioskForegroundService.refresh(getApplication())
            _uiState.update { it.copy(errorMessage = null) }
        }
    }

    fun setRentalTime(seconds: Long) {
        viewModelScope.launch {
            repository.startCountdown(seconds)
            KioskForegroundService.refresh(getApplication())
            _uiState.update { it.copy(errorMessage = null) }
        }
    }

    fun savePackageDuration(
        hours: Int,
        minutes: Int,
        seconds: Int,
        bonusMinutes: Int,
        isUnlimited: Boolean
    ) {
        val totalSeconds = (hours * 3600L) + (minutes * 60L) + seconds +
            (bonusMinutes * 60L)

        if (!isUnlimited && totalSeconds <= 0L) {
            _uiState.update { it.copy(errorMessage = "Durasi sewa harus lebih dari 0 detik.") }
            return
        }

        viewModelScope.launch {
            preferences.savePackageTime(hours, minutes, bonusMinutes)
            preferences.saveUnlimitedMode(isUnlimited)
            if (isUnlimited) {
                repository.startUnlimited()
            } else {
                repository.startCountdown(totalSeconds)
            }
            KioskForegroundService.refresh(getApplication())
            _uiState.update { it.copy(showAdminDialog = false, isAdminUnlocked = false, errorMessage = null) }
        }
    }

    fun endRentalSession() {
        viewModelScope.launch {
            repository.stopSession(expired = false)
            KioskForegroundService.refresh(getApplication())
            KioskForegroundService.dismissAlarm(getApplication())
            _uiState.update {
                it.copy(showAdminDialog = false, isAdminUnlocked = false, errorMessage = null)
            }
        }
    }

    fun saveIdentityAndBanners(
        rentalName: String,
        phoneLabel: String,
        b1: String,
        b2: String,
        b3: String,
        autoStart: Boolean
    ) {
        viewModelScope.launch {
            preferences.saveRentalName(rentalName)
            preferences.savePhoneLabel(phoneLabel)
            preferences.saveBannerTexts(b1, b2, b3)
            preferences.saveAutoStart(autoStart)
            _uiState.update {
                it.copy(
                    rentalName = rentalName,
                    phoneLabel = phoneLabel,
                    bannerTexts = listOf(b1, b2, b3),
                    showAdminDialog = false,
                    isAdminUnlocked = false
                )
            }
        }
    }

    fun saveWhitelist(packages: Set<String>) {
        viewModelScope.launch {
            preferences.saveWhitelistPackages(packages)
            repository.updateWhitelistPackages(packages.toList())
            appliedWhitelist = emptySet()
            filterWhitelistApps(packages)
            if (deviceOwnerManager.isDeviceOwner()) {
                deviceOwnerManager.configureKioskPolicies(packages.toList())
            }
            _uiState.update { it.copy(showAdminDialog = false, isAdminUnlocked = false) }
        }
    }

    fun verifyAdminPin(pin: String): Boolean {
        val prefsPin = preferences.getSettings().adminPin
        return pin == prefsPin || SecurityHelper.verifyPin(pin, localPinHash)
    }

    fun unlockWithPin(pin: String) {
        if (verifyAdminPin(pin)) {
            KioskForegroundService.dismissAlarm(getApplication())
            _uiState.update {
                it.copy(
                    showAdminDialog = true,
                    isAdminUnlocked = true,
                    errorMessage = null
                )
            }
        } else {
            _uiState.update {
                it.copy(errorMessage = "PIN Admin Salah. Coba lagi!")
            }
        }
    }

    fun updateAdminPin(newPin: String) {
        if (newPin.length != 6 || !newPin.all { it.isDigit() }) {
            _uiState.update { it.copy(errorMessage = "PIN harus 6 angka.") }
            return
        }
        preferences.saveAdminPin(newPin)
        localPinHash = SecurityHelper.hashPin(newPin)
        viewModelScope.launch {
            repository.updateAdminPin(newPin)
            _uiState.update { it.copy(errorMessage = null) }
        }
    }

    fun updateBatteryTelemetry(
        level: Int,
        isCharging: Boolean,
        tempCelsius: Float,
        networkStatus: String
    ) {
        // 45°C terlalu umum saat gaming; 48°C baru dianggap benar-benar panas.
        val isOverheated = tempCelsius >= 48.0f
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
            preferences.saveUnlimitedMode(false)
            repository.stopSession(expired = false)
            KioskForegroundService.refresh(getApplication())
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
            if (deviceOwnerManager.isDeviceOwner()) {
                deviceOwnerManager.configureKioskPolicies(
                    preferences.getSettings().whitelistPackages.toList()
                )
            }
            val success = deviceOwnerManager.startLockTask(activity)
            preferences.saveKioskActive(success)
            _uiState.update { it.copy(isLockTaskModeActive = success) }
        } else {
            val success = deviceOwnerManager.stopLockTask(activity)
            preferences.saveKioskActive(false)
            _uiState.update { it.copy(isLockTaskModeActive = !success) }
        }
    }

    /**
     * Dipanggil dari onResume. Jika perangkat adalah Device Owner dan kiosk
     * diaktifkan, langsung pin aplikasi tanpa perlu menekan tombol.
     */
    fun maybeAutoStartKiosk(activity: Activity) {
        if (hasAutoStartedKiosk) return
        if (!deviceOwnerManager.isDeviceOwner()) return
        if (!preferences.getSettings().isKioskActive) return

        hasAutoStartedKiosk = true
        deviceOwnerManager.configureKioskPolicies(
            preferences.getSettings().whitelistPackages.toList()
        )
        val success = deviceOwnerManager.startLockTask(activity)
        _uiState.update { it.copy(isLockTaskModeActive = success) }
    }

    fun checkForUpdates(silent: Boolean = false) {
        if (silent && !preferences.shouldCheckForUpdates()) return
        viewModelScope.launch {
            val release = updateManager.checkLatestRelease()
            if (silent) preferences.markUpdateChecked()

            val skippedVersion = preferences.getSkippedUpdateVersion()
            if (release != null && release.tagName != skippedVersion) {
                _uiState.update {
                    it.copy(
                        updateRelease = release,
                        showUpdateDialog = true
                    )
                }
            } else if (!silent) {
                Toast.makeText(
                    getApplication(),
                    updateManager.statusMessage.value.ifBlank {
                        "Aplikasi Rental.iO sudah versi terbaru."
                    },
                    Toast.LENGTH_SHORT
                ).show()
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

    fun skipUpdateVersion() {
        preferences.saveSkippedUpdateVersion(_uiState.value.updateRelease?.tagName.orEmpty())
        _uiState.update { it.copy(showUpdateDialog = false) }
    }

    fun openAdminDialog() {
        _uiState.update { it.copy(showAdminDialog = true, isAdminUnlocked = false) }
    }

    fun dismissAdminDialog() {
        _uiState.update { it.copy(showAdminDialog = false, isAdminUnlocked = false) }
    }

    override fun onCleared() {
        repository.stopCloudSyncListener()
        super.onCleared()
    }
}
