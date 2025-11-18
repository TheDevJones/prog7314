package com.st10028374.vitality_vault.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.st10028374.vitality_vault.database.converters.DateConverter
import com.st10028374.vitality_vault.database.converters.LocationPointListConverter
import com.st10028374.vitality_vault.routes.models.LocationPoint
import java.util.Date

/**
 * Room Entity for Routes
 */
@Entity(tableName = "routes")
@TypeConverters(DateConverter::class, LocationPointListConverter::class)
data class RouteEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val routeName: String,
    val distance: Double,
    val duration: Long,
    val averagePace: Double,
    val estimatedHeartRate: Int,
    val caloriesBurnt: Double,
    val pathPoints: List<LocationPoint>,
    val dateFormatted: String,
    val timestamp: Date?,
    val createdAt: Long,
    val isSynced: Boolean = false // Flag to track if synced to Firestore
)