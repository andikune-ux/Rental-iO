package dev.andikune.rentalio.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import dev.andikune.rentalio.R
import dev.andikune.rentalio.data.repository.RentalRepository
import dev.andikune.rentalio.ui.MainActivity
import dev.andikune.rentalio.utils.SecurityHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class KioskForegroundService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var timerJob: Job? = null
    private var alarmJob: Job? = null
    private lateinit var repository: RentalRepository
    private var currentSeconds: Long = 3600L
    private var isExpired: Boolean = false
    private var ringtone: Ringtone? = null

    companion object {
        const val CHANNEL_ID = "rental_timer_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "dev.andikune.rentalio.action.START_TIMER"
        const val ACTION_STOP = "dev.andikune.rentalio.action.STOP_TIMER"
        const val ACTION_ADD_TIME = "dev.andikune.rentalio.action.ADD_TIME"
        const val ACTION_DISMISS_ALARM = "dev.andikune.rentalio.action.DISMISS_ALARM"
        const val EXTRA_SECONDS = "extra_seconds"

        fun start(context: Context) {
            val intent = Intent(context, KioskForegroundService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, KioskForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun addTime(context: Context, seconds: Long) {
            val intent = Intent(context, KioskForegroundService::class.java).apply {
                action = ACTION_ADD_TIME
                putExtra(EXTRA_SECONDS, seconds)
            }
            context.startService(intent)
        }

        fun dismissAlarm(context: Context) {
            val intent = Intent(context, KioskForegroundService::class.java).apply {
                action = ACTION_DISMISS_ALARM
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        repository = RentalRepository(applicationContext)
        createNotificationChannel()
        initAlarmSound()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopTimer()
                stopAlarm()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_ADD_TIME -> {
                val added = intent.getLongExtra(EXTRA_SECONDS, 0L)
                if (added > 0) {
                    currentSeconds += added
                    isExpired = false
                    stopAlarm()
                    serviceScope.launch {
                        repository.updateTimer(currentSeconds, isExpired = false)
                    }
                    updateNotification(currentSeconds)
                }
            }
            ACTION_DISMISS_ALARM -> {
                stopAlarm()
            }
            ACTION_START, null -> {
                startForeground(NOTIFICATION_ID, buildNotification("Memulai sesi rental..."))
                startTimer()
            }
        }
        return START_STICKY
    }

    private fun startTimer() {
        if (timerJob?.isActive == true) return

        timerJob = serviceScope.launch {
            repository.initializeIfNeeded()
            currentSeconds = repository.getRemainingSeconds()
            val prefs = dev.andikune.rentalio.data.local.RentalPreferences(applicationContext)

            while (isActive) {
                val currentPrefs = prefs.getSettings()
                if (currentPrefs.isUnlimitedMode) {
                    // Unlimited Mode (Count Up)
                    currentSeconds++
                    repository.updateTimer(currentSeconds, isExpired = false)
                    val bill = dev.andikune.rentalio.utils.SecurityHelper.calculateUnlimitedCost(currentSeconds)
                    val text = "Unlimited: ${dev.andikune.rentalio.utils.SecurityHelper.formatDuration(currentSeconds)} | Biaya: ${dev.andikune.rentalio.utils.SecurityHelper.formatRupiah(bill)}"
                    updateNotificationCustom(text)
                } else {
                    // Regular Countdown Mode
                    if (currentSeconds > 0) {
                        currentSeconds--
                        repository.updateTimer(currentSeconds, isExpired = false)
                        updateNotification(currentSeconds)

                        // Bagian 3 Butir 13: Toast 15, 10, 5 Menit
                        if (currentSeconds == 900L || currentSeconds == 600L || currentSeconds == 300L) {
                            dev.andikune.rentalio.utils.SystemOverlayHelper.showPassThroughToastAlert(
                                applicationContext,
                                (currentSeconds / 60).toInt()
                            )
                        }

                        // Bagian 3 Butir 13: 30 Detik Countdown Transparent Overlay
                        if (currentSeconds in 1..30) {
                            dev.andikune.rentalio.utils.SystemOverlayHelper.showPassThrough30sCountdown(
                                applicationContext,
                                currentSeconds.toInt()
                            )
                        } else if (currentSeconds > 30) {
                            dev.andikune.rentalio.utils.SystemOverlayHelper.hidePassThroughCountdown()
                        }

                        if (isExpired) {
                            isExpired = false
                            stopAlarm()
                        }
                    } else {
                        dev.andikune.rentalio.utils.SystemOverlayHelper.hidePassThroughCountdown()
                        if (!isExpired) {
                            isExpired = true
                            repository.updateTimer(0, isExpired = true)
                            triggerExpirationAlarm()
                        }
                        updateNotification(0)
                    }
                }
                delay(1000L)
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun triggerExpirationAlarm() {
        // Bring main activity to foreground immediately to show Blocking Overlay
        try {
            val launchIntent = Intent(applicationContext, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                putExtra("IS_GAME_OVER", true)
            }
            startActivity(launchIntent)
        } catch (e: Exception) {
            Log.e("KioskService", "Failed to launch main activity on expiration: ${e.message}")
        }

        // Start repeating vibration and tone
        if (alarmJob?.isActive == true) return
        alarmJob = serviceScope.launch {
            while (isActive && isExpired) {
                playAlarmSound()
                triggerVibration()
                delay(2000L)
            }
        }
    }

    private fun stopAlarm() {
        alarmJob?.cancel()
        alarmJob = null
        try {
            ringtone?.stop()
        } catch (_: Exception) {}
    }

    private fun initAlarmSound() {
        try {
            val alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ringtone = RingtoneManager.getRingtone(applicationContext, alertUri)
        } catch (e: Exception) {
            Log.w("KioskService", "Could not init ringtone: ${e.message}")
        }
    }

    private fun playAlarmSound() {
        try {
            if (ringtone != null && !ringtone!!.isPlaying) {
                ringtone?.play()
            }
        } catch (_: Exception) {
            try {
                val tone = ToneGenerator(android.media.AudioManager.STREAM_ALARM, 100)
                tone.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 1200)
            } catch (_: Exception) {}
        }
    }

    private fun triggerVibration() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0, 500, 200, 500), -1)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 200, 500), -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(longArrayOf(0, 500, 200, 500), -1)
                }
            }
        } catch (e: Exception) {
            Log.w("KioskService", "Vibration failed: ${e.message}")
        }
    }

    private fun buildNotification(text: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Rental.iO Gaming Kiosk")
            .setContentText(text)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun updateNotification(seconds: Long) {
        val text = if (seconds > 0) {
            "Sisa Waktu Sewa: ${SecurityHelper.formatDuration(seconds)}"
        } else {
            "⚠️ MASA SEWA HABIS! Hubungi Admin Rental."
        }
        val notification = buildNotification(text)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun updateNotificationCustom(text: String) {
        val notification = buildNotification(text)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Rental.iO Timer & Lock",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Persistent active timer and expiration alerts for gaming kiosk rental"
                setSound(null, null)
                enableVibration(true)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        stopTimer()
        stopAlarm()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
