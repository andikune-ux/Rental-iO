package dev.andikune.rentalio.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rental_state")
data class LocalRentalState(
    @PrimaryKey val id: Int = 1,
    val remainingTimeSeconds: Long = 0L,
    val isExpired: Boolean = false,
    val isRunning: Boolean = false,
    val isUnlimited: Boolean = false,
    val timerEndEpochMs: Long = 0L,
    val unlimitedStartEpochMs: Long = 0L,
    val localAdminPinHash: String = "",
    val whitelistPackages: String = "",
    val isSuspended: Boolean = false,
    val adminId: String = "admin_01",
    val ownerId: String = "owner_root",
    val lastUpdatedTimestamp: Long = System.currentTimeMillis()
)
