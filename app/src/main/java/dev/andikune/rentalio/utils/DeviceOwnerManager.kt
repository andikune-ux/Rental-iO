package dev.andikune.rentalio.utils

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.UserManager
import android.util.Log
import dev.andikune.rentalio.receiver.AdminReceiver

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

            // Configure lock task features: izinkan tombol HOME agar penyewa bisa kembali ke launcher,
            // tetapi TETAP MATIKAN Overview/Recents (LOCK_TASK_FEATURE_OVERVIEW tidak diaktifkan)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val flags = DevicePolicyManager.LOCK_TASK_FEATURE_HOME
                dpm.setLockTaskFeatures(adminComponent, flags)
            }

            // Disable keyguard and status bar
            dpm.setKeyguardDisabled(adminComponent, true)
            dpm.setStatusBarDisabled(adminComponent, true)

            // Restrict user settings modifications and factory reset
            dpm.addUserRestriction(adminComponent, UserManager.DISALLOW_FACTORY_RESET)
            dpm.addUserRestriction(adminComponent, UserManager.DISALLOW_SAFE_BOOT)
            dpm.addUserRestriction(adminComponent, UserManager.DISALLOW_ADD_USER)
            dpm.addUserRestriction(adminComponent, UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA)
        } catch (e: Exception) {
            Log.e("DeviceOwnerManager", "Error configuring kiosk policies: ${e.message}", e)
        }
    }

    fun startLockTask(activity: Activity, whitelistedPackages: List<String> = emptyList()): Boolean {
        return try {
            if (isDeviceOwner()) {
                val allPackages = (listOf(context.packageName) + whitelistedPackages).distinct().toTypedArray()
                dpm.setLockTaskPackages(adminComponent, allPackages)
            }
            activity.startLockTask()
            true
        } catch (e: Exception) {
            Log.e("DeviceOwnerManager", "Failed to start lock task: ${e.message}")
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
        } catch (e: Exception) {
            Log.e("DeviceOwnerManager", "Error releasing kiosk policies: ${e.message}")
        }
    }
}
