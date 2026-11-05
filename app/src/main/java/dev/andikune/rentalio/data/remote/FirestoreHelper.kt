package dev.andikune.rentalio.data.remote

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import dev.andikune.rentalio.data.model.Admin
import dev.andikune.rentalio.data.model.DeviceSession
import kotlinx.coroutines.tasks.await

class FirestoreHelper {
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private var deviceListener: ListenerRegistration? = null

    suspend fun syncDeviceSession(session: DeviceSession): Boolean {
        return try {
            firestore.collection("devices")
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
        return try {
            val doc = firestore.collection("admins").document(adminId).get().await()
            doc.toObject(Admin::class.java)
        } catch (e: Exception) {
            Log.w("FirestoreHelper", "Admin fetch failed: ${e.message}")
            null
        }
    }

    fun listenToDeviceUpdates(deviceId: String, onUpdate: (DeviceSession) -> Unit) {
        deviceListener?.remove()
        try {
            deviceListener = firestore.collection("devices")
                .document(deviceId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
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
