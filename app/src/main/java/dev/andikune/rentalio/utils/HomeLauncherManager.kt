package dev.andikune.rentalio.utils

import android.app.Activity
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log

/**
 * Helper untuk menjadikan Rental.iO sebagai Home Launcher utama.
 *
 * - Jika aplikasi berstatus Device Owner, peran Home dipasang permanen secara
 *   diam-diam lewat [DeviceOwnerManager.setPersistentHome] (tanpa dialog).
 * - Jika bukan Device Owner, Android mewajibkan persetujuan pengguna, jadi
 *   ditampilkan dialog sistem "Home app" (RoleManager di Android 10+).
 */
object HomeLauncherManager {

    /** Request code untuk dialog RoleManager (ROLE_HOME). */
    const val REQUEST_CODE_SET_HOME = 4201

    /**
     * Cek apakah Rental.iO sudah menjadi Home Launcher default saat ini.
     */
    fun isDefaultHome(context: Context): Boolean {
        return try {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
            }
            val resolved = context.packageManager.resolveActivity(
                homeIntent,
                PackageManager.MATCH_DEFAULT_ONLY
            )
            resolved?.activityInfo?.packageName == context.packageName
        } catch (e: Exception) {
            Log.w("HomeLauncherManager", "Gagal cek default home: ${e.message}")
            false
        }
    }

    /**
     * Minta Rental.iO menjadi Home Launcher default.
     *
     * @return true jika permintaan berhasil dikirim / sudah menjadi default.
     */
    fun requestDefaultHome(activity: Activity): Boolean {
        if (isDefaultHome(activity)) return true

        // 1. Device Owner -> pasang permanen tanpa dialog.
        val deviceOwnerManager = DeviceOwnerManager(activity)
        if (deviceOwnerManager.isDeviceOwner() && deviceOwnerManager.setPersistentHome()) {
            Log.i("HomeLauncherManager", "Home launcher dipasang permanen via Device Owner.")
            return true
        }

        // 2. Android 10+ -> dialog resmi RoleManager ROLE_HOME.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val roleManager = activity.getSystemService(RoleManager::class.java)
                if (roleManager != null &&
                    roleManager.isRoleAvailable(RoleManager.ROLE_HOME) &&
                    !roleManager.isRoleHeld(RoleManager.ROLE_HOME)
                ) {
                    activity.startActivityForResult(
                        roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME),
                        REQUEST_CODE_SET_HOME
                    )
                    return true
                }
            } catch (e: Exception) {
                Log.w("HomeLauncherManager", "RoleManager gagal: ${e.message}")
            }
        }

        // 3. Fallback Android 7-9 / perangkat tanpa RoleManager:
        //    buka halaman pemilihan Home app di Settings.
        return try {
            activity.startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
            true
        } catch (e: Exception) {
            Log.w("HomeLauncherManager", "Settings Home gagal: ${e.message}")
            // 4. Fallback terakhir: tampilkan chooser Home.
            try {
                val chooser = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                }
                activity.startActivity(chooser)
                true
            } catch (e2: Exception) {
                Log.e("HomeLauncherManager", "Tidak bisa membuka chooser Home: ${e2.message}")
                false
            }
        }
    }
}
