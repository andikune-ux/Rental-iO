package dev.andikune.rentalio.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RentalPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("rentalio_prefs", Context.MODE_PRIVATE)

    data class SettingsData(
        val rentalName: String = "UNE RENTAL HP",
        val phoneLabel: String = "HP NO. 1",
        val adminPin: String = "123456",
        val isUnlimitedMode: Boolean = false,
        val autoStartOnBoot: Boolean = true,
        val packageHours: Int = 1,
        val packageMinutes: Int = 0,
        val bonusMinutes: Int = 0,
        val bannerText1: String = "Rental HP murah 30 menit 3ribu aja 😆",
        val bannerText2: String = "Rental HP murah 60 Menit 5ribu aja 😁",
        val bannerText3: String = "Mohon menggunakan HP rental dengan hati-hati...",
        val whitelistPackages: Set<String> = defaultWhitelist,
        val isKioskActive: Boolean = true
    )

    private val _settingsFlow = MutableStateFlow(loadSettings())
    val settingsFlow: StateFlow<SettingsData> = _settingsFlow.asStateFlow()

    fun getSettings(): SettingsData = loadSettings()

    private fun loadSettings(): SettingsData {
        val rawWhitelist = prefs.getStringSet(KEY_WHITELIST, defaultWhitelist) ?: defaultWhitelist
        return SettingsData(
            rentalName = prefs.getString(KEY_RENTAL_NAME, "UNE RENTAL HP") ?: "UNE RENTAL HP",
            phoneLabel = prefs.getString(KEY_PHONE_LABEL, "HP NO. 1") ?: "HP NO. 1",
            adminPin = prefs.getString(KEY_ADMIN_PIN, "123456") ?: "123456",
            isUnlimitedMode = prefs.getBoolean(KEY_UNLIMITED_MODE, false),
            autoStartOnBoot = prefs.getBoolean(KEY_AUTO_START, true),
            packageHours = prefs.getInt(KEY_PACKAGE_HOURS, 1),
            packageMinutes = prefs.getInt(KEY_PACKAGE_MINUTES, 0),
            bonusMinutes = prefs.getInt(KEY_BONUS_MINUTES, 0),
            bannerText1 = prefs.getString(KEY_BANNER_1, "Rental HP murah 30 menit 3ribu aja 😆") ?: "Rental HP murah 30 menit 3ribu aja 😆",
            bannerText2 = prefs.getString(KEY_BANNER_2, "Rental HP murah 60 Menit 5ribu aja 😁") ?: "Rental HP murah 60 Menit 5ribu aja 😁",
            bannerText3 = prefs.getString(KEY_BANNER_3, "Mohon menggunakan HP rental dengan hati-hati...") ?: "Mohon menggunakan HP rental dengan hati-hati...",
            whitelistPackages = rawWhitelist,
            isKioskActive = prefs.getBoolean(KEY_KIOSK_ACTIVE, true)
        )
    }

    fun saveRentalName(name: String) {
        prefs.edit().putString(KEY_RENTAL_NAME, name).apply()
        _settingsFlow.value = loadSettings()
    }

    fun savePhoneLabel(label: String) {
        prefs.edit().putString(KEY_PHONE_LABEL, label).apply()
        _settingsFlow.value = loadSettings()
    }

    fun saveAdminPin(pin: String) {
        prefs.edit().putString(KEY_ADMIN_PIN, pin).apply()
        _settingsFlow.value = loadSettings()
    }

    fun saveUnlimitedMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_UNLIMITED_MODE, enabled).apply()
        _settingsFlow.value = loadSettings()
    }

    fun saveAutoStart(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_START, enabled).apply()
        _settingsFlow.value = loadSettings()
    }

    fun savePackageTime(hours: Int, minutes: Int, bonusMinutes: Int) {
        prefs.edit()
            .putInt(KEY_PACKAGE_HOURS, hours)
            .putInt(KEY_PACKAGE_MINUTES, minutes)
            .putInt(KEY_BONUS_MINUTES, bonusMinutes)
            .apply()
        _settingsFlow.value = loadSettings()
    }

    fun saveBannerTexts(t1: String, t2: String, t3: String) {
        prefs.edit()
            .putString(KEY_BANNER_1, t1)
            .putString(KEY_BANNER_2, t2)
            .putString(KEY_BANNER_3, t3)
            .apply()
        _settingsFlow.value = loadSettings()
    }

    fun saveWhitelistPackages(packages: Set<String>) {
        prefs.edit().putStringSet(KEY_WHITELIST, packages).apply()
        _settingsFlow.value = loadSettings()
    }

    fun saveKioskActive(active: Boolean) {
        prefs.edit().putBoolean(KEY_KIOSK_ACTIVE, active).apply()
        _settingsFlow.value = loadSettings()
    }

    companion object {
        private const val KEY_RENTAL_NAME = "rental_name"
        private const val KEY_PHONE_LABEL = "phone_label"
        private const val KEY_ADMIN_PIN = "admin_pin"
        private const val KEY_UNLIMITED_MODE = "unlimited_mode"
        private const val KEY_AUTO_START = "auto_start_on_boot"
        private const val KEY_PACKAGE_HOURS = "package_hours"
        private const val KEY_PACKAGE_MINUTES = "package_minutes"
        private const val KEY_BONUS_MINUTES = "bonus_minutes"
        private const val KEY_BANNER_1 = "banner_1"
        private const val KEY_BANNER_2 = "banner_2"
        private const val KEY_BANNER_3 = "banner_3"
        private const val KEY_WHITELIST = "whitelist_packages"
        private const val KEY_KIOSK_ACTIVE = "kiosk_active"

        val defaultWhitelist = setOf(
            "com.dts.freefireth",
            "com.mobile.legends",
            "com.tencent.ig",
            "com.miHoYo.GenshinImpact",
            "com.activision.callofduty.shooter",
            "com.ea.gp.apexlegendsmobilefps",
            "com.roblox.client",
            "com.gameloft.android.ANMP.GloftA9HM"
        )
    }
}
