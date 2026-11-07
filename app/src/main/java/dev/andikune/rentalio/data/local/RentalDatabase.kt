package dev.andikune.rentalio.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [LocalRentalState::class], version = 2, exportSchema = false)
abstract class RentalDatabase : RoomDatabase() {
    abstract fun rentalDao(): LocalRentalDao

    companion object {
        @Volatile
        private var INSTANCE: RentalDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE rental_state ADD COLUMN isRunning INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE rental_state ADD COLUMN isUnlimited INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE rental_state ADD COLUMN timerEndEpochMs INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE rental_state ADD COLUMN unlimitedStartEpochMs INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getInstance(context: Context): RentalDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RentalDatabase::class.java,
                    "rental_io.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
