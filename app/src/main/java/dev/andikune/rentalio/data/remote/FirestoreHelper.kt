package dev.andikune.rentalio.data.remote

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import dev.andikune.rentalio.data.model.Admin
import dev.andikune.rentalio.data.model.DeviceSession
import kotlinx.coroutines.tasks.await

/**
 * Firestore bersifat opsional. Jika google-services.json tidak tersedia
 * (misalnya build offline), semua operasi menjadi no-op dan aplikasi tetap
 * berjalan penuh secara lokal.
 */
class FirestoreHelper(private val context: Context? = null) {
    private val available: Boolean by lazy {
        try {
            context != null && FirebaseApp.getApps(context).isNotEmpty()
        } catch (_: Exception) {
            false
        }
    }

    val isAvailable: Boolean get() = available

    private val firestore: FirebaseFirestore? by lazy {
        if (!available) null else runCatching { FirebaseFirestore.getInstance() }.getOrNull()
    }

    private var deviceListener: ListenerRegistration? = null

    suspend fun syncDeviceSession(session: DeviceSession): Boolean {
        val db = firestore ?: return false
        return try {
            db.collection("devices")
                .document(session.deviceId)
                .set(session)
                .await()
            true
        } catch (e: Exception) {
            Log.w("FirestoreHelper", "Firestore sync failed, offline fallback active: ${e.message}")
            false
        }
    }

    suspend fun fetchAdminStatus(adminId: String): Admin? {
        val db = firestore ?: return null
        return try {
            val doc = db.collection("admins").document(adminId).get().await()
            doc.toObject(Admin::class.java)
        } catch (e: Exception) {
            Log.w("FirestoreHelper", "Admin fetch failed: ${e.message}")
            null
        }
    }

    fun listenToDeviceUpdates(deviceId: String, onUpdate: (DeviceSession) -> Unit) {
        val db = firestore ?: return
        deviceListener?.remove()
        try {
            deviceListener = db.collection("devices")
                .document(deviceId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
                    val session = snapshot.toObject(DeviceSession::class.java)
                    if (session != null) {
                        onUpdate(session)
                    }
                }
        } catch (e: Exception) {
            Log.w("FirestoreHelper", "Snapshot listener error: ${e.message}")
        }
    }

    fun removeListener() {
        deviceListener?.remove()
        deviceListener = null
    }
}
