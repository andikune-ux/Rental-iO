package dev.andikune.rentalio.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [LocalRentalState::class], version = 1, exportSchema = false)
abstract class RentalDatabase : RoomDatabase() {
    abstract fun rentalDao(): LocalRentalDao

    companion object {
        @Volatile
        private var INSTANCE: RentalDatabase? = null

        fun getInstance(context: Context): RentalDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RentalDatabase::class.java,
                    "rental_io.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
