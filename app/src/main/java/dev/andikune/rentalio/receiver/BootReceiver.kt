package dev.andikune.rentalio.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.andikune.rentalio.ui.MainActivity

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            val autoStart = dev.andikune.rentalio.data.local.RentalPreferences(context).getSettings().autoStartOnBoot
            if (autoStart) {
                val i = Intent(context, MainActivity::class.java)
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(i)
            }
        }
    }
}
