package dev.andikune.rentalio

import android.app.Application
import dev.andikune.rentalio.utils.CrashHandler

class RentalIOApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Inisialisasi CrashHandler paling awal sebelum apapun dimuat
        CrashHandler.initialize(this)
    }
}
