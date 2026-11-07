package dev.andikune.rentalio.utils

import java.security.MessageDigest

object SecurityHelper {
    private val HEX_PIN_REGEX = Regex("^[0-9a-fA-F]{64}$")

    fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(pin.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Mendukung dua format: PIN lama yang masih plaintext (kompatibilitas
     * upgrade) dan PIN baru yang sudah SHA-256.
     */
    fun verifyPin(pin: String, stored: String?): Boolean {
        if (stored.isNullOrBlank()) return false
        val normalized = stored.trim()
        return if (HEX_PIN_REGEX.matches(normalized)) {
            hashPin(pin).equals(normalized, ignoreCase = true)
        } else {
            normalized == pin
        }
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
     *
     * Total menit dihitung lebih dulu supaya input seperti 90 menit tetap
     * dihargai 1 jam + 30 menit (Rp 8.000), bukan 90 x Rp 100.
     */
    fun calculateRegularCost(hours: Int, minutes: Int): Long {
        val totalMinutes = (hours.coerceAtLeast(0) * 60) + minutes.coerceAtLeast(0)
        val fullHoursCost = (totalMinutes / 60) * 5000L
        val minutesCost = (totalMinutes % 60) * 100L
        return fullHoursCost + minutesCost
    }

    /**
     * Sesuai Bagian 7 (Tagihan Unlimited):
     * Total Menit * Rp 100
     */
    fun calculateUnlimitedCost(seconds: Long): Long {
        val totalMinutes = seconds.coerceAtLeast(0L) / 60L
        return totalMinutes * 100L
    }

    fun formatRupiah(amount: Long): String {
        val formatter = java.text.NumberFormat.getNumberInstance(java.util.Locale.forLanguageTag("id-ID"))
        return "Rp ${formatter.format(amount)}"
    }
}
