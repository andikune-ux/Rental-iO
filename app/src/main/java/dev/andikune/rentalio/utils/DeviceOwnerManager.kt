package dev.andikune.rentalio.utils

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.UserManager
import android.util.Log
import dev.andikune.rentalio.receiver.AdminReceiver
import dev.andikune.rentalio.ui.MainActivity

class DeviceOwnerManager(private val context: Context) {
    private val dpm: DevicePolicyManager =
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    val adminComponent: ComponentName = ComponentName(context, AdminReceiver::class.java)

    fun isDeviceOwner(): Boolean {
        return dpm.isDeviceOwnerApp(context.packageName)
    }

    fun isAdminActive(): Boolean {
        return dpm.isAdminActive(adminComponent)
    }

    fun configureKioskPolicies(whitelistedPackages: List<String>) {
        if (!isDeviceOwner()) {
            Log.w("DeviceOwnerManager", "App is not Device Owner. Run ADB command to grant owner permissions.")
            return
        }

        try {
            // Whitelist Rental.iO package plus all approved gaming packages for LockTask
            val allPackages = (listOf(context.packageName) + whitelistedPackages).distinct().toTypedArray()
            dpm.setLockTaskPackages(adminComponent, allPackages)

            // Configure lock task features: disable home, notifications, keyguard, system info
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val flags = DevicePolicyManager.LOCK_TASK_FEATURE_NONE
                dpm.setLockTaskFeatures(adminComponent, flags)
            }

            // Disable keyguard and status bar
            dpm.setKeyguardDisabled(adminComponent, true)
            dpm.setStatusBarDisabled(adminComponent, true)

            // Jadikan Rental.iO sebagai Home Launcher permanen (tombol Home selalu
            // kembali ke kiosk, tanpa dialog pemilihan launcher).
            setPersistentHome()

            // Restrict user settings modifications and factory reset
            dpm.addUserRestriction(adminComponent, UserManager.DISALLOW_FACTORY_RESET)
            dpm.addUserRestriction(adminComponent, UserManager.DISALLOW_SAFE_BOOT)
            dpm.addUserRestriction(adminComponent, UserManager.DISALLOW_ADD_USER)
            dpm.addUserRestriction(adminComponent, UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA)
        } catch (e: Exception) {
            Log.e("DeviceOwnerManager", "Error configuring kiosk policies: ${e.message}", e)
        }
    }

    fun startLockTask(activity: Activity): Boolean {
        return try {
            if (isDeviceOwner()) {
                val packages = dpm.getLockTaskPackages(adminComponent)
                if (!packages.contains(context.packageName)) {
                    dpm.setLockTaskPackages(adminComponent, arrayOf(context.packageName))
                }
                // Pastikan tombol Home selalu diarahkan ke Rental.iO.
                setPersistentHome()
            }
            activity.startLockTask()
            true
        } catch (e: Exception) {
            Log.e("DeviceOwnerManager", "Failed to start lock task: ${e.message}")
            false
        }
    }

    /**
     * Pasang Rental.iO sebagai Home Launcher default secara permanen.
     * Hanya berfungsi jika aplikasi berstatus Device Owner.
     */
    fun setPersistentHome(): Boolean {
        if (!isDeviceOwner()) return false
        return try {
            val homeFilter = IntentFilter(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                addCategory(Intent.CATEGORY_DEFAULT)
            }
            val homeComponent = ComponentName(context, MainActivity::class.java)
            dpm.clearPackagePersistentPreferredActivities(adminComponent, context.packageName)
            dpm.addPersistentPreferredActivity(adminComponent, homeFilter, homeComponent)
            Log.i("DeviceOwnerManager", "Persistent Home activity set to ${homeComponent.flattenToShortString()}")
            true
        } catch (e: Exception) {
            Log.e("DeviceOwnerManager", "Failed to set persistent home: ${e.message}", e)
            false
        }
    }

    fun stopLockTask(activity: Activity): Boolean {
        return try {
            activity.stopLockTask()
            true
        } catch (e: Exception) {
            Log.e("DeviceOwnerManager", "Failed to stop lock task: ${e.message}")
            false
        }
    }

    fun releaseKioskPolicies() {
        if (!isDeviceOwner()) return
        try {
            dpm.setKeyguardDisabled(adminComponent, false)
            dpm.setStatusBarDisabled(adminComponent, false)
            dpm.clearUserRestriction(adminComponent, UserManager.DISALLOW_FACTORY_RESET)
            dpm.clearUserRestriction(adminComponent, UserManager.DISALLOW_SAFE_BOOT)
            // Lepas paksaan Home app agar pengguna bisa memilih launcher lain.
            dpm.clearPackagePersistentPreferredActivities(adminComponent, context.packageName)
        } catch (e: Exception) {
            Log.e("DeviceOwnerManager", "Error releasing kiosk policies: ${e.message}")
        }
    }
}
