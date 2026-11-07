package dev.andikune.rentalio.data.repository

import android.content.Context
import android.os.Build
import android.provider.Settings
import dev.andikune.rentalio.data.local.LocalRentalDao
import dev.andikune.rentalio.data.local.LocalRentalState
import dev.andikune.rentalio.data.local.RentalDatabase
import dev.andikune.rentalio.data.local.RentalPreferences
import dev.andikune.rentalio.data.model.DeviceSession
import dev.andikune.rentalio.data.remote.FirestoreHelper
import dev.andikune.rentalio.data.timer.TimerBus
import dev.andikune.rentalio.data.timer.TimerSession
import dev.andikune.rentalio.data.timer.TimerSnapshot
import dev.andikune.rentalio.utils.SecurityHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class RentalRepository(
    private val context: Context,
    private val dao: LocalRentalDao = RentalDatabase.getInstance(context).rentalDao(),
    private val firestoreHelper: FirestoreHelper = FirestoreHelper(context)
) {
    private val preferences = RentalPreferences(context)

    private val deviceId: String by lazy {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?: "device_${Build.MODEL.replace(" ", "_")}"
    }

    @Volatile
    private var lastCloudSyncMs: Long = 0L

    val rentalStateFlow: Flow<LocalRentalState?> = dao.getRentalStateFlow()

    suspend fun initializeIfNeeded() = withContext(Dispatchers.IO) {
        val existing = dao.getRentalState()
        if (existing == null) {
            val settings = preferences.getSettings()
            dao.insertOrUpdate(
                LocalRentalState(
                    id = 1,
                    remainingTimeSeconds = 0L,
                    isExpired = false,
                    isRunning = false,
                    isUnlimited = false,
                    timerEndEpochMs = 0L,
                    unlimitedStartEpochMs = 0L,
                    localAdminPinHash = SecurityHelper.hashPin(settings.adminPin),
                    whitelistPackages = settings.whitelistPackages.joinToString(",")
                )
            )
        }
        TimerBus.publish(snapshotOf(loadSessionInternal()))
    }

    suspend fun loadTimerSession(): TimerSession = withContext(Dispatchers.IO) {
        loadSessionInternal()
    }

    private suspend fun loadSessionInternal(): TimerSession {
        val state = dao.getRentalState() ?: LocalRentalState()
        return TimerSession(
            isRunning = state.isRunning,
            isUnlimited = state.isUnlimited,
            endEpochMs = state.timerEndEpochMs,
            startEpochMs = state.unlimitedStartEpochMs,
            storedRemainingSeconds = state.remainingTimeSeconds,
            isExpired = state.isExpired
        )
    }

    /**
     * Menghitung snapshot dari sesi + jam dinding. Tidak menyentuh database.
     */
    fun snapshotOf(session: TimerSession, now: Long = System.currentTimeMillis()): TimerSnapshot {
        return when {
            !session.isRunning -> TimerSnapshot(
                remainingSeconds = session.storedRemainingSeconds.coerceAtLeast(0L),
                elapsedSeconds = 0L,
                isRunning = false,
                isExpired = session.isExpired,
                isUnlimited = false
            )

            session.isUnlimited -> {
                val elapsed = ((now - session.startEpochMs).coerceAtLeast(0L)) / 1000L
                TimerSnapshot(
                    remainingSeconds = 0L,
                    elapsedSeconds = elapsed,
                    isRunning = true,
                    isExpired = false,
                    isUnlimited = true
                )
            }

            else -> {
                val remaining = ((session.endEpochMs - now + 999L) / 1000L).coerceAtLeast(0L)
                TimerSnapshot(
                    remainingSeconds = remaining,
                    elapsedSeconds = 0L,
                    isRunning = remaining > 0L,
                    isExpired = remaining <= 0L,
                    isUnlimited = false
                )
            }
        }
    }

    suspend fun getTimerSnapshot(): TimerSnapshot = withContext(Dispatchers.IO) {
        snapshotOf(loadSessionInternal())
    }

    suspend fun publishTimerState() = withContext(Dispatchers.IO) {
        TimerBus.publish(snapshotOf(loadSessionInternal()))
    }

    suspend fun startCountdown(seconds: Long): TimerSnapshot = withContext(Dispatchers.IO) {
        val safe = seconds.coerceAtLeast(0L)
        val now = System.currentTimeMillis()
        val running = safe > 0L
        val session = TimerSession(
            isRunning = running,
            isUnlimited = false,
            endEpochMs = if (running) now + (safe * 1000L) else 0L,
            startEpochMs = 0L,
            storedRemainingSeconds = safe,
            isExpired = !running
        )
        updateSessionRecord(session, safe, !running)
        preferences.saveUnlimitedMode(false)
        snapshotOf(session).also { TimerBus.publish(it) }
    }

    suspend fun startUnlimited(): TimerSnapshot = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val session = TimerSession(
            isRunning = true,
            isUnlimited = true,
            endEpochMs = 0L,
            startEpochMs = now,
            storedRemainingSeconds = 0L,
            isExpired = false
        )
        updateSessionRecord(session, 0L, false)
        preferences.saveUnlimitedMode(true)
        snapshotOf(session).also { TimerBus.publish(it) }
    }

    suspend fun addRentalTime(additionalSeconds: Long): TimerSnapshot = withContext(Dispatchers.IO) {
        val current = loadSessionInternal()
        if (additionalSeconds <= 0L) {
            return@withContext snapshotOf(current)
        }
        if (current.isUnlimited && current.isRunning) {
            // Unlimited sudah menghitung maju; tambah waktu tidak bermakna.
            return@withContext snapshotOf(current)
        }

        val now = System.currentTimeMillis()
        val base = if (current.isRunning && current.endEpochMs > now) {
            current.endEpochMs
        } else {
            now
        }
        val newEnd = base + (additionalSeconds * 1000L)
        val remaining = ((newEnd - now + 999L) / 1000L).coerceAtLeast(1L)
        val session = TimerSession(
            isRunning = true,
            isUnlimited = false,
            endEpochMs = newEnd,
            startEpochMs = 0L,
            storedRemainingSeconds = remaining,
            isExpired = false
        )
        updateSessionRecord(session, remaining, false)
        preferences.saveUnlimitedMode(false)
        snapshotOf(session).also { TimerBus.publish(it) }
    }

    suspend fun stopSession(expired: Boolean = false): TimerSnapshot = withContext(Dispatchers.IO) {
        val session = TimerSession(
            isRunning = false,
            isUnlimited = false,
            endEpochMs = 0L,
            startEpochMs = 0L,
            storedRemainingSeconds = 0L,
            isExpired = expired
        )
        updateSessionRecord(session, 0L, expired)
        if (!expired) {
            preferences.saveUnlimitedMode(false)
        }
        snapshotOf(session).also { TimerBus.publish(it) }
    }

    suspend fun markExpired(): TimerSnapshot = withContext(Dispatchers.IO) {
        val current = loadSessionInternal()
        if (current.isUnlimited) {
            return@withContext snapshotOf(current)
        }
        val session = current.copy(
            isRunning = false,
            endEpochMs = 0L,
            startEpochMs = 0L,
            storedRemainingSeconds = 0L,
            isExpired = true
        )
        updateSessionRecord(session, 0L, true)
        snapshotOf(session).also { TimerBus.publish(it) }
    }

    suspend fun persistSnapshot(snapshot: TimerSnapshot, session: TimerSession) = withContext(Dispatchers.IO) {
        if (snapshot.isExpired) return@withContext
        updateSessionRecord(
            session = session,
            remaining = snapshot.remainingSeconds.coerceAtLeast(0L),
            expired = false
        )
    }

    private suspend fun updateSessionRecord(session: TimerSession, remaining: Long, expired: Boolean) {
        dao.updateSession(
            remaining = remaining,
            expired = expired,
            running = session.isRunning,
            unlimited = session.isUnlimited,
            endEpochMs = session.endEpochMs,
            unlimitedStartEpochMs = session.startEpochMs,
            timestamp = System.currentTimeMillis()
        )
    }

    suspend fun getStoredPinHash(): String? = withContext(Dispatchers.IO) {
        dao.getRentalState()?.localAdminPinHash
    }

    suspend fun verifyAdminPin(pin: String): Boolean = withContext(Dispatchers.IO) {
        val storedHash = dao.getRentalState()?.localAdminPinHash
        val prefsPin = preferences.getSettings().adminPin
        pin == prefsPin || SecurityHelper.verifyPin(pin, storedHash)
    }

    suspend fun updateAdminPin(newPin: String) = withContext(Dispatchers.IO) {
        dao.updatePin(SecurityHelper.hashPin(newPin))
        preferences.saveAdminPin(newPin)
    }

    suspend fun updateWhitelistPackages(packages: List<String>) = withContext(Dispatchers.IO) {
        dao.updateWhitelist(packages.joinToString(","))
    }

    suspend fun getWhitelistPackagesList(): List<String> = withContext(Dispatchers.IO) {
        val raw = dao.getRentalState()?.whitelistPackages ?: ""
        raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    suspend fun setSuspended(suspended: Boolean) = withContext(Dispatchers.IO) {
        dao.updateSuspended(suspended)
    }

    /**
     * Sinkronisasi cloud dibatasi agar tidak menulis ke Firestore setiap detik.
     * Dipanggil oleh service paling cepat setiap 30 detik.
     */
    suspend fun syncCloud(snapshot: TimerSnapshot) = withContext(Dispatchers.IO) {
        if (!firestoreHelper.isAvailable) return@withContext
        val now = System.currentTimeMillis()
        if (now - lastCloudSyncMs < CLOUD_SYNC_INTERVAL_MS) return@withContext
        lastCloudSyncMs = now
        try {
            firestoreHelper.syncDeviceSession(
                DeviceSession(
                    deviceId = deviceId,
                    adminId = "admin_01",
                    isLocked = snapshot.isExpired,
                    remainingSeconds = if (snapshot.isUnlimited) snapshot.elapsedSeconds else snapshot.remainingSeconds,
                    whitelistApps = getWhitelistPackagesListInternal(),
                    mode = if (firestoreHelper.isAvailable) "CLOUD_FIRESTORE" else "OFFLINE_FALLBACK",
                    lastUpdatedBy = "DEVICE_TIMER",
                    lastTimestamp = now
                )
            )
        } catch (_: Exception) {
            // Offline fallback tetap berjalan.
        }
    }

    private suspend fun getWhitelistPackagesListInternal(): List<String> {
        val raw = dao.getRentalState()?.whitelistPackages ?: ""
        return raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun startCloudSyncListener(onCloudUpdate: (DeviceSession) -> Unit) {
        if (!firestoreHelper.isAvailable) return
        firestoreHelper.listenToDeviceUpdates(deviceId, onCloudUpdate)
    }

    fun stopCloudSyncListener() {
        firestoreHelper.removeListener()
    }

    /**
     * Menerapkan perubahan dari dashboard admin/owner. Perubahan dari timer
     * perangkat sendiri diabaikan agar tidak terjadi loop.
     */
    suspend fun applyCloudUpdate(session: DeviceSession) = withContext(Dispatchers.IO) {
        if (session.lastUpdatedBy == "DEVICE_TIMER") return@withContext
        if (session.isLocked) {
            stopSession(expired = true)
        } else if (session.remainingSeconds > 0L) {
            startCountdown(session.remainingSeconds)
        }
        if (session.whitelistApps.isNotEmpty()) {
            updateWhitelistPackages(session.whitelistApps)
            preferences.saveWhitelistPackages(session.whitelistApps.toSet())
        }
    }

    companion object {
        private const val CLOUD_SYNC_INTERVAL_MS = 30_000L
    }
}
