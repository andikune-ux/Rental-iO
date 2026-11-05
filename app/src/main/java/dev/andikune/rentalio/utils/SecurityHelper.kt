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
}
