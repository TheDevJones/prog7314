package com.st10028374.vitality_vault.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.st10028374.vitality_vault.database.converters.DateConverter
import java.util.Date

/**
 * Room Entity for Workouts
 */
@Entity(tableName = "workouts")
@TypeConverters(DateConverter::class)
data class WorkoutEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val activityType: String,
    val duration: String,
    val intensity: String,
    val date: String,
    val completionPercentage: Int,
    val timestamp: Date,
    val isSynced: Boolean = false, // Flag to track if synced to Firestore
    val createdAt: Long = System.currentTimeMillis()
)
