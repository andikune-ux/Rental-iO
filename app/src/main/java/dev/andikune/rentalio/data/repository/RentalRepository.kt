package dev.andikune.rentalio.data.repository

import android.content.Context
import android.os.Build
import android.provider.Settings
import dev.andikune.rentalio.data.local.LocalRentalDao
import dev.andikune.rentalio.data.local.LocalRentalState
import dev.andikune.rentalio.data.local.RentalDatabase
import dev.andikune.rentalio.data.model.DeviceSession
import dev.andikune.rentalio.data.remote.FirestoreHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RentalRepository(
    private val context: Context,
    private val dao: LocalRentalDao = RentalDatabase.getInstance(context).rentalDao(),
    private val firestoreHelper: FirestoreHelper = FirestoreHelper()
) {
    private val deviceId: String by lazy {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?: "device_${Build.MODEL.replace(" ", "_")}"
    }

    val rentalStateFlow: Flow<LocalRentalState?> = dao.getRentalStateFlow()

    suspend fun initializeIfNeeded() = withContext(Dispatchers.IO) {
        val existing = dao.getRentalState()
        if (existing == null) {
            val defaultState = LocalRentalState(
                id = 1,
                remainingTimeSeconds = 3600L,
                isExpired = false,
                localAdminPinHash = "123456",
                whitelistPackages = "com.dts.freefireth,com.mobile.legends,com.tencent.ig,com.miHoYo.GenshinImpact,com.activision.callofduty.shooter"
            )
            dao.insertOrUpdate(defaultState)
        }
    }

    suspend fun getRemainingSeconds(): Long = withContext(Dispatchers.IO) {
        dao.getRentalState()?.remainingTimeSeconds ?: 3600L
    }

    suspend fun updateTimer(seconds: Long, isExpired: Boolean) = withContext(Dispatchers.IO) {
        dao.updateTimer(seconds, isExpired)
        // Attempt cloud update asynchronously without blocking local operations
        CoroutineScope(Dispatchers.IO).launch {
            try {
                firestoreHelper.syncDeviceSession(
                    DeviceSession(
                        deviceId = deviceId,
                        adminId = "admin_01",
                        isLocked = isExpired,
                        remainingSeconds = seconds,
                        whitelistApps = getWhitelistPackagesList(),
                        mode = "HYBRID_OFFLINE_READY",
                        lastUpdatedBy = "DEVICE_TIMER"
                    )
                )
            } catch (_: Exception) {
                // Offline fallback remains unaffected
            }
        }
    }

    suspend fun addRentalTime(additionalSeconds: Long) = withContext(Dispatchers.IO) {
        val current = dao.getRentalState() ?: LocalRentalState()
        val newSeconds = maxOf(0L, current.remainingTimeSeconds) + additionalSeconds
        dao.updateTimer(newSeconds, isExpired = false)
    }

    suspend fun setRemainingSeconds(seconds: Long) = withContext(Dispatchers.IO) {
        dao.updateTimer(seconds, isExpired = seconds <= 0L)
    }

    suspend fun verifyAdminPin(pin: String): Boolean = withContext(Dispatchers.IO) {
        val current = dao.getRentalState() ?: LocalRentalState()
        val prefs = dev.andikune.rentalio.data.local.RentalPreferences(context).getSettings()
        pin == current.localAdminPinHash || pin == prefs.adminPin
    }

    suspend fun updateAdminPin(newPin: String) = withContext(Dispatchers.IO) {
        dao.updatePin(newPin)
        dev.andikune.rentalio.data.local.RentalPreferences(context).saveAdminPin(newPin)
    }

    suspend fun getWhitelistPackagesList(): List<String> = withContext(Dispatchers.IO) {
        val raw = dao.getRentalState()?.whitelistPackages ?: ""
        raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    suspend fun updateWhitelistPackages(packages: List<String>) = withContext(Dispatchers.IO) {
        dao.updateWhitelist(packages.joinToString(","))
    }

    fun startCloudSyncListener(onCloudUpdate: (Long, Boolean) -> Unit) {
        firestoreHelper.listenToDeviceUpdates(deviceId) { cloudSession ->
            onCloudUpdate(cloudSession.remainingSeconds, cloudSession.isLocked)
        }
    }

    fun stopCloudSyncListener() {
        firestoreHelper.removeListener()
    }
}
