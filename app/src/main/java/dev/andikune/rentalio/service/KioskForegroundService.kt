package dev.andikune.rentalio.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
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
import androidx.core.app.ServiceCompat
import dev.andikune.rentalio.R
import dev.andikune.rentalio.data.local.RentalPreferences
import dev.andikune.rentalio.data.repository.RentalRepository
import dev.andikune.rentalio.data.timer.TimerBus
import dev.andikune.rentalio.data.timer.TimerSession
import dev.andikune.rentalio.data.timer.TimerSnapshot
import dev.andikune.rentalio.ui.MainActivity
import dev.andikune.rentalio.utils.SecurityHelper
import dev.andikune.rentalio.utils.SystemOverlayHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class KioskForegroundService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var timerJob: Job? = null
    private var alarmJob: Job? = null
    private lateinit var repository: RentalRepository
    private lateinit var preferences: RentalPreferences

    private var cachedSession: TimerSession? = null
    private var alarmExpired: Boolean = false
    private var ringtone: Ringtone? = null

    companion object {
        const val CHANNEL_ID = "rental_timer_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "dev.andikune.rentalio.action.START_TIMER"
        const val ACTION_STOP = "dev.andikune.rentalio.action.STOP_TIMER"
        const val ACTION_ADD_TIME = "dev.andikune.rentalio.action.ADD_TIME"
        const val ACTION_SET_TIME = "dev.andikune.rentalio.action.SET_TIME"
        const val ACTION_DISMISS_ALARM = "dev.andikune.rentalio.action.DISMISS_ALARM"
        const val ACTION_REFRESH = "dev.andikune.rentalio.action.REFRESH"
        const val EXTRA_SECONDS = "extra_seconds"
        private const val PERSIST_INTERVAL_MS = 10_000L
        private const val NOTIFY_INTERVAL_MS = 5_000L

        private fun send(context: Context, action: String, seconds: Long? = null) {
            val intent = Intent(context, KioskForegroundService::class.java).apply {
                this.action = action
                if (seconds != null) putExtra(EXTRA_SECONDS, seconds)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                // Android 12+ dapat menolak start FGS dari background. Service
                // yang sudah berjalan tetap hidup, jadi kegagalan ini aman.
                Log.w("KioskService", "Tidak bisa memulai service ($action): ${e.message}")
            }
        }

        fun start(context: Context) = send(context, ACTION_START)

        fun stop(context: Context) {
            context.stopService(Intent(context, KioskForegroundService::class.java))
        }

        fun addTime(context: Context, seconds: Long) = send(context, ACTION_ADD_TIME, seconds)

        fun setTime(context: Context, seconds: Long) = send(context, ACTION_SET_TIME, seconds)

        fun dismissAlarm(context: Context) = send(context, ACTION_DISMISS_ALARM)

        fun refresh(context: Context) = send(context, ACTION_REFRESH)
    }

    override fun onCreate() {
        super.onCreate()
        repository = RentalRepository(applicationContext)
        preferences = RentalPreferences(applicationContext)
        createNotificationChannel()
        initAlarmSound()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        if (action == ACTION_STOP) {
            shutdownService()
            return START_NOT_STICKY
        }

        // Wajib dipanggil untuk setiap startForegroundService (Android 8+),
        // termasuk ACTION_ADD_TIME / ACTION_SET_TIME / ACTION_REFRESH.
        val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification("Menyiapkan kiosk Rental.iO..."),
            serviceType
        )

        when (action) {
            ACTION_ADD_TIME -> {
                val seconds = intent.getLongExtra(EXTRA_SECONDS, 0L)
                serviceScope.launch {
                    if (seconds > 0L) {
                        repository.addRentalTime(seconds)
                        reloadSessionAndPublish()
                    }
                    startTimer()
                }
            }

            ACTION_SET_TIME -> {
                val seconds = intent.getLongExtra(EXTRA_SECONDS, 0L)
                serviceScope.launch {
                    val settings = preferences.getSettings()
                    if (settings.isUnlimitedMode) {
                        repository.startUnlimited()
                    } else {
                        repository.startCountdown(seconds)
                    }
                    alarmExpired = false
                    stopAlarm()
                    reloadSessionAndPublish()
                    startTimer()
                }
            }

            ACTION_REFRESH -> {
                serviceScope.launch {
                    reloadSessionAndPublish()
                    startTimer()
                }
            }

            ACTION_DISMISS_ALARM -> {
                alarmExpired = true
                stopAlarm()
                startTimer()
            }

            ACTION_START, null -> {
                startTimer()
            }
        }

        return START_STICKY
    }

    private fun startTimer() {
        if (timerJob?.isActive == true) {
            serviceScope.launch { reloadSessionAndPublish() }
            return
        }

        timerJob = serviceScope.launch {
            repository.initializeIfNeeded()
            cachedSession = repository.loadTimerSession()
            var lastPersistMs = System.currentTimeMillis()
            var lastNotifyMs = 0L

            while (isActive) {
                val session = cachedSession ?: repository.loadTimerSession().also { cachedSession = it }
                val snapshot = repository.snapshotOf(session)
                TimerBus.publish(snapshot)

                if (!snapshot.isExpired && alarmExpired) {
                    alarmExpired = false
                    stopAlarm()
                }

                if (snapshot.isExpired && !alarmExpired) {
                    alarmExpired = true
                    repository.markExpired()
                    cachedSession = repository.loadTimerSession()
                    TimerBus.publish(repository.snapshotOf(cachedSession!!))
                    triggerExpirationAlarm()
                } else if (!snapshot.isUnlimited) {
                    if (snapshot.isRunning) {
                        if (snapshot.remainingSeconds in 1L..30L) {
                            SystemOverlayHelper.showPassThrough30sCountdown(
                                applicationContext,
                                snapshot.remainingSeconds.toInt()
                            )
                        } else {
                            SystemOverlayHelper.hidePassThroughCountdown()
                        }

                        if (snapshot.remainingSeconds == 900L ||
                            snapshot.remainingSeconds == 600L ||
                            snapshot.remainingSeconds == 300L
                        ) {
                            SystemOverlayHelper.showPassThroughToastAlert(
                                applicationContext,
                                (snapshot.remainingSeconds / 60L).toInt()
                            )
                        }
                    } else if (!snapshot.isExpired) {
                        SystemOverlayHelper.hidePassThroughCountdown()
                    }
                } else {
                    SystemOverlayHelper.hidePassThroughCountdown()
                }

                val now = System.currentTimeMillis()
                if (lastNotifyMs == 0L || now - lastNotifyMs >= NOTIFY_INTERVAL_MS) {
                    updateNotificationForSnapshot(snapshot)
                    lastNotifyMs = now
                }

                if ((snapshot.isRunning || snapshot.isUnlimited) && now - lastPersistMs >= PERSIST_INTERVAL_MS) {
                    repository.persistSnapshot(snapshot, cachedSession!!)
                    repository.syncCloud(snapshot)
                    lastPersistMs = now
                }

                delay(if (snapshot.isRunning || snapshot.isUnlimited) 500L else 1000L)
            }
        }
    }

    private suspend fun reloadSessionAndPublish() {
        val session = repository.loadTimerSession()
        cachedSession = session
        val snapshot = repository.snapshotOf(session)
        TimerBus.publish(snapshot)
        if (!snapshot.isExpired) {
            alarmExpired = false
            stopAlarm()
        }
        updateNotificationForSnapshot(snapshot)
    }

    private fun triggerExpirationAlarm() {
        SystemOverlayHelper.hidePassThroughCountdown()

        // Full-screen intent + activity agar overlay Game Over tampil walau
        // pengguna sedang berada di dalam game.
        updateNotificationForSnapshot(
            TimerSnapshot(isRunning = false, isExpired = true, isUnlimited = false)
        )

        try {
            val launchIntent = Intent(applicationContext, MainActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                )
                putExtra("IS_GAME_OVER", true)
            }
            startActivity(launchIntent)
        } catch (e: Exception) {
            Log.e("KioskService", "Failed to launch main activity on expiration: ${e.message}")
        }

        if (alarmJob?.isActive == true) return
        alarmJob = serviceScope.launch {
            while (isActive && alarmExpired) {
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
        } catch (_: Exception) {
        }
    }

    private fun initAlarmSound() {
        try {
            val alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ringtone = RingtoneManager.getRingtone(applicationContext, alertUri)?.apply {
                audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            }
        } catch (e: Exception) {
            Log.w("KioskService", "Could not init ringtone: ${e.message}")
        }
    }

    private fun playAlarmSound() {
        try {
            val tone = ringtone
            if (tone != null && !tone.isPlaying) {
                tone.play()
            }
        } catch (_: Exception) {
            try {
                val generator = ToneGenerator(android.media.AudioManager.STREAM_ALARM, 100)
                generator.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 1200)
            } catch (_: Exception) {
            }
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

    private fun buildNotification(text: String, fullScreen: Boolean = false): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Rental.iO Gaming Kiosk")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(if (fullScreen) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(pendingIntent)

        if (fullScreen) {
            builder.setFullScreenIntent(pendingIntent, true)
        }

        return builder.build()
    }

    private fun updateNotificationForSnapshot(snapshot: TimerSnapshot) {
        val text = when {
            snapshot.isExpired ->
                "⚠️ MASA SEWA HABIS! Hubungi Admin Rental."
            snapshot.isUnlimited -> {
                val bill = SecurityHelper.calculateUnlimitedCost(snapshot.elapsedSeconds)
                "Unlimited: ${SecurityHelper.formatDuration(snapshot.elapsedSeconds)} | Biaya: ${SecurityHelper.formatRupiah(bill)}"
            }
            snapshot.isRunning ->
                "Sisa Waktu Sewa: ${SecurityHelper.formatDuration(snapshot.remainingSeconds)}"
            else ->
                "Kiosk aktif • belum ada sesi. Buka Admin untuk mulai."
        }
        val notification = buildNotification(text, fullScreen = snapshot.isExpired)
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
                enableVibration(false)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun shutdownService() {
        timerJob?.cancel()
        timerJob = null
        stopAlarm()
        SystemOverlayHelper.hidePassThroughCountdown()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        timerJob?.cancel()
        timerJob = null
        stopAlarm()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
