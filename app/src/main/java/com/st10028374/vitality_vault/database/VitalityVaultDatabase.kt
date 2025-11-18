package com.st10028374.vitality_vault.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.st10028374.vitality_vault.database.converters.DateConverter
import com.st10028374.vitality_vault.database.converters.LocationPointListConverter
import com.st10028374.vitality_vault.database.dao.RouteDao
import com.st10028374.vitality_vault.database.dao.WorkoutDao
import com.st10028374.vitality_vault.database.entities.RouteEntity
import com.st10028374.vitality_vault.database.entities.WorkoutEntity

@Database(
    entities = [WorkoutEntity::class, RouteEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(DateConverter::class, LocationPointListConverter::class)
abstract class VitalityVaultDatabase : RoomDatabase() {

    abstract fun workoutDao(): WorkoutDao
    abstract fun routeDao(): RouteDao

    companion object {
        @Volatile
        private var INSTANCE: VitalityVaultDatabase? = null

        fun getDatabase(context: Context): VitalityVaultDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VitalityVaultDatabase::class.java,
                    "vitality_vault_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}