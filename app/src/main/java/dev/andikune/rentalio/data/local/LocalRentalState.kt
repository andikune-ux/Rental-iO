package dev.andikune.rentalio.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rental_state")
data class LocalRentalState(
    @PrimaryKey val id: Int = 1,
    val remainingTimeSeconds: Long = 3600L,
    val isExpired: Boolean = false,
    val localAdminPinHash: String = "8899",
    val whitelistPackages: String = "com.dts.freefireth,com.mobile.legends,com.tencent.ig,com.miHoYo.GenshinImpact,com.activision.callofduty.shooter",
    val isSuspended: Boolean = false,
    val adminId: String = "admin_01",
    val ownerId: String = "owner_root",
    val lastUpdatedTimestamp: Long = System.currentTimeMillis()
)
