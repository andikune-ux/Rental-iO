package dev.andikune.rentalio.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.andikune.rentalio.data.local.RentalPreferences
import dev.andikune.rentalio.service.KioskForegroundService
import dev.andikune.rentalio.ui.MainActivity

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != "android.intent.action.QUICKBOOT_POWERON") {
            return
        }

        // Service foreground selalu dijalankan agar timer berbasis epoch langsung
        // dihitung ulang setelah reboot, walaupun activity tidak boleh dibuka.
        KioskForegroundService.start(context)

        val autoStart = RentalPreferences(context).getSettings().autoStartOnBoot
        if (autoStart) {
            try {
                val i = Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                context.startActivity(i)
            } catch (_: Exception) {
                // Android 10+ dapat memblokir background activity start.
                // Karena Rental.iO adalah Home launcher, sistem tetap akan
                // menampilkannya setelah boot.
            }
        }
    }
}
