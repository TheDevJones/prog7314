package com.st10028374.vitality_vault.routes.utils

import android.location.Location
import com.st10028374.vitality_vault.routes.models.LocationPoint
import com.st10028374.vitality_vault.routes.models.UserProfile

object TrackingUtils {

    // Calculate distance between two LocationPoints in kilometers
    fun calculateDistance(point1: LocationPoint, point2: LocationPoint): Double {
        val results = FloatArray(1)
        Location.distanceBetween(
            point1.latitude, point1.longitude,
            point2.latitude, point2.longitude,
            results
        )
        return results[0] / 1000.0 // Convert meters to kilometers
    }

    // Calculate total distance for a list of points
    fun calculateTotalDistance(points: List<LocationPoint>): Double {
        if (points.size < 2) return 0.0

        var totalDistance = 0.0
        for (i in 0 until points.size - 1) {
            totalDistance += calculateDistance(points[i], points[i + 1])
        }
        return totalDistance
    }

    // Calculate pace in min/km
    fun calculatePace(distance: Double, durationMillis: Long): Double {
        if (distance <= 0) return 0.0
        val durationMinutes = durationMillis / 60000.0
        return durationMinutes / distance
    }

    // Format pace as MM:SS per km
    fun formatPace(paceMinPerKm: Double): String {
        if (paceMinPerKm.isInfinite() || paceMinPerKm.isNaN() || paceMinPerKm <= 0) {
            return "0:00"
        }
        val minutes = paceMinPerKm.toInt()
        val seconds = ((paceMinPerKm - minutes) * 60).toInt()
        return String.format("%d:%02d", minutes, seconds)
    }

    // Estimate heart rate based on pace and user profile
    fun estimateHeartRate(
        paceMinPerKm: Double,
        userProfile: UserProfile
    ): Int {
        // Calculate max heart rate: 220 - age
        val maxHR = 220 - userProfile.age

        // Estimate based on pace (this is a simplified estimation)
        // Slower pace (>8 min/km) = 60-70% max HR
        // Medium pace (6-8 min/km) = 70-85% max HR
        // Fast pace (<6 min/km) = 85-95% max HR

        val percentage = when {
            paceMinPerKm > 8 -> 0.65
            paceMinPerKm > 6 -> 0.775
            else -> 0.90
        }

        return (maxHR * percentage).toInt()
    }

    // Calculate calories burnt
    fun calculateCaloriesBurnt(
        distance: Double,
        durationMillis: Long,
        userProfile: UserProfile
    ): Double {
        // MET (Metabolic Equivalent) calculation
        // Running MET values vary by speed
        val durationHours = durationMillis / 3600000.0
        val speedKmh = if (durationHours > 0) distance / durationHours else 0.0

        val met = when {
            speedKmh < 6.4 -> 6.0   // Slow jogging
            speedKmh < 8.0 -> 8.3   // Jogging
            speedKmh < 9.7 -> 9.8   // Running
            speedKmh < 11.3 -> 10.5 // Fast running
            speedKmh < 12.9 -> 11.0 // Very fast running
            else -> 11.5            // Sprinting
        }

        // Calories = MET * weight(kg) * duration(hours)
        return met * userProfile.weight * durationHours
    }

    // Format distance
    fun formatDistance(distanceKm: Double): String {
        return String.format("%.2f", distanceKm)
    }

    // Format duration as HH:MM:SS
    fun formatDuration(durationMillis: Long): String {
        val seconds = (durationMillis / 1000) % 60
        val minutes = (durationMillis / (1000 * 60)) % 60
        val hours = (durationMillis / (1000 * 60 * 60))

        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    // Check if location is valid
    fun isValidLocation(location: Location?): Boolean {
        return location != null &&
                location.accuracy <= 50 && // Accuracy better than 50 meters
                location.hasAccuracy()
    }

    // Calculate current speed in km/h
    fun calculateSpeed(location: Location): Double {
        return if (location.hasSpeed()) {
            (location.speed * 3.6) // Convert m/s to km/h
        } else {
            0.0
        }
    }
}