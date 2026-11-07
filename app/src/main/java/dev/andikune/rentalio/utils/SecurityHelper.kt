package dev.andikune.rentalio.utils

import java.security.MessageDigest

object SecurityHelper {
    fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(pin.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun formatDuration(seconds: Long): String {
        if (seconds <= 0) return "00j : 00m : 00s"
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return "%02dj : %02dm : %02ds".format(hours, minutes, secs)
    }

    fun formatTimeDigital(seconds: Long): String {
        if (seconds <= 0) return "00:00:00"
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return "%02d:%02d:%02d".format(hours, minutes, secs)
    }

    /**
     * Sesuai Bagian 6 (Sistem Harga):
     * 1 jam penuh (60 mnt) = Rp 5.000
     * Sisa menit = Rp 100 / menit
     * Bonus menit = Rp 0 (gratis)
     */
    fun calculateRegularCost(hours: Int, minutes: Int): Long {
        val fullHoursCost = hours.coerceAtLeast(0) * 5000L
        val minutesCost = minutes.coerceAtLeast(0) * 100L
        return fullHoursCost + minutesCost
    }

    /**
     * Sesuai Bagian 7 (Tagihan Unlimited):
     * 30 menit pertama flat Rp 3.000 (jika <= 30 menit)
     * Setelah 30 menit, bertambah Rp 100 / menit (misal 31 menit = Rp 3.100)
     */
    fun calculateUnlimitedExactCost(seconds: Long): Long {
        val totalMinutes = seconds.coerceAtLeast(0L) / 60L
        return if (totalMinutes <= 30L) {
            3000L
        } else {
            3000L + (totalMinutes - 30L) * 100L
        }
    }

    /**
     * Pembulatan ke bawah per kelipatan Rp 500:
     * Contoh: Rp 3.100 - Rp 3.400 dibulatkan ke Rp 3.000
     *         Rp 3.600 - Rp 3.900 dibulatkan ke Rp 3.500
     */
    fun roundToNearest500(amount: Long): Long {
        return (amount / 500L) * 500L
    }

    /**
     * Tagihan akhir setelah pembulatan
     */
    fun calculateUnlimitedCost(seconds: Long): Long {
        val exact = calculateUnlimitedExactCost(seconds)
        return roundToNearest500(exact)
    }

    fun formatRupiah(amount: Long): String {
        val formatter = java.text.NumberFormat.getNumberInstance(java.util.Locale.forLanguageTag("id-ID"))
        return "Rp ${formatter.format(amount)}"
    }
}

