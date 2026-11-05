package dev.andikune.rentalio.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalRentalDao {
    @Query("SELECT * FROM rental_state WHERE id = 1 LIMIT 1")
    fun getRentalStateFlow(): Flow<LocalRentalState?>

    @Query("SELECT * FROM rental_state WHERE id = 1 LIMIT 1")
    suspend fun getRentalState(): LocalRentalState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(state: LocalRentalState)

    @Query("UPDATE rental_state SET remainingTimeSeconds = :seconds, isExpired = :isExpired, lastUpdatedTimestamp = :timestamp WHERE id = 1")
    suspend fun updateTimer(seconds: Long, isExpired: Boolean, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE rental_state SET localAdminPinHash = :pinHash WHERE id = 1")
    suspend fun updatePin(pinHash: String)

    @Query("UPDATE rental_state SET whitelistPackages = :packages WHERE id = 1")
    suspend fun updateWhitelist(packages: String)
}
