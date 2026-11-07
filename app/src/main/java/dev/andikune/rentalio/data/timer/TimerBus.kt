package dev.andikune.rentalio.data.timer

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Sesi timer yang dipersist ke Room. Waktu disimpan sebagai epoch (wall clock)
 * supaya sisa waktu tetap benar walaupun proses aplikasi mati, perangkat reboot,
 * atau service di-kill oleh sistem.
 */
data class TimerSession(
    val isRunning: Boolean = false,
    val isUnlimited: Boolean = false,
    val endEpochMs: Long = 0L,
    val startEpochMs: Long = 0L,
    val storedRemainingSeconds: Long = 0L,
    val isExpired: Boolean = false
)

/**
 * Snapshot timer yang dipakai UI. Dihitung ulang dari [TimerSession] + jam dinding.
 */
data class TimerSnapshot(
    val remainingSeconds: Long = 0L,
    val elapsedSeconds: Long = 0L,
    val isRunning: Boolean = false,
    val isExpired: Boolean = false,
    val isUnlimited: Boolean = false
)

/**
 * Bus timer process-wide. Service menulis, UI membaca. Tidak perlu menulis ke
 * Room setiap detik hanya untuk memperbarui tampilan.
 */
object TimerBus {
    private val _state = MutableStateFlow(TimerSnapshot())
    val state: StateFlow<TimerSnapshot> = _state.asStateFlow()

    fun publish(snapshot: TimerSnapshot) {
        _state.value = snapshot
    }
}
