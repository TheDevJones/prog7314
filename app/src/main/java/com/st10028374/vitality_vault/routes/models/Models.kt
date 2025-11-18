package com.st10028374.vitality_vault.routes.models

import android.os.Parcelable
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import java.util.Date

/**
 * Location Point representing a GPS coordinate
 */
@Parcelize
data class LocationPoint(
    @SerializedName("latitude")
    val latitude: Double = 0.0,

    @SerializedName("longitude")
    val longitude: Double = 0.0,

    @SerializedName("timestamp")
    val timestamp: Long = 0L
) : Parcelable

/**
 * Route Model for both Firebase and API
 */
@Parcelize
data class RouteModel(
    @DocumentId
    @SerializedName("id")
    val id: String = "",

    @SerializedName("userId")
    val userId: String = "",

    @SerializedName("routeName")
    val routeName: String = "",

    @SerializedName("distance")
    val distance: Double = 0.0, // kilometers

    @SerializedName("duration")
    val duration: Long = 0L, // milliseconds

    @SerializedName("averagePace")
    val averagePace: Double = 0.0, // min/km

    @SerializedName("estimatedHeartRate")
    val estimatedHeartRate: Int = 0,

    @SerializedName("caloriesBurnt")
    val caloriesBurnt: Double = 0.0,

    @SerializedName("pathPoints")
    val pathPoints: List<LocationPoint> = emptyList(),

    @SerializedName("dateFormatted")
    val dateFormatted: String = "",

    @ServerTimestamp
    @SerializedName("timestamp")
    val timestamp: Date? = null,

    @SerializedName("createdAt")
    val createdAt: Long = System.currentTimeMillis()
) : Parcelable

/**
 * Request model for saving/updating routes
 */
data class RouteRequest(
    @SerializedName("userId")
    val userId: String,

    @SerializedName("routeName")
    val routeName: String,

    @SerializedName("distance")
    val distance: Double,

    @SerializedName("duration")
    val duration: Long,

    @SerializedName("averagePace")
    val averagePace: Double,

    @SerializedName("estimatedHeartRate")
    val estimatedHeartRate: Int,

    @SerializedName("caloriesBurnt")
    val caloriesBurnt: Double,

    @SerializedName("pathPoints")
    val pathPoints: List<LocationPoint>,

    @SerializedName("dateFormatted")
    val dateFormatted: String
)

/**
 * Response model for route operations
 */
data class RouteResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String,

    @SerializedName("routeId")
    val routeId: String? = null
)

/**
 * Response model for single route fetch
 */
data class SingleRouteResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("route")
    val route: RouteModel? = null,

    @SerializedName("message")
    val message: String? = null
)

/**
 * Response model for routes list
 */
data class RoutesListResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("routes")
    val routes: List<RouteModel> = emptyList(),

    @SerializedName("count")
    val count: Int = 0,

    @SerializedName("total")
    val total: Int = 0,

    @SerializedName("message")
    val message: String? = null
)

/**
 * User statistics model
 */
data class UserStats(
    @SerializedName("totalRoutes")
    val totalRoutes: Int = 0,

    @SerializedName("totalDistance")
    val totalDistance: Double = 0.0,

    @SerializedName("totalDuration")
    val totalDuration: Long = 0L,

    @SerializedName("totalCalories")
    val totalCalories: Double = 0.0,

    @SerializedName("avgPace")
    val avgPace: Double = 0.0,

    @SerializedName("avgHeartRate")
    val avgHeartRate: Double = 0.0
)

/**
 * Response model for user statistics
 */
data class StatsResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("stats")
    val stats: UserStats? = null,

    @SerializedName("message")
    val message: String? = null
)

/**
 * Tracking data model for real-time updates
 */
data class TrackingData(
    var distance: Double = 0.0,
    var duration: Long = 0L,
    var currentPace: Double = 0.0,
    var averagePace: Double = 0.0,
    var estimatedHeartRate: Int = 0,
    var caloriesBurnt: Double = 0.0,
    val pathPoints: MutableList<LocationPoint> = mutableListOf()
)

/**
 * User profile model
 */
@Parcelize
data class UserProfile(
    val userId: String = "",
    val age: Int = 30,
    val weight: Double = 70.0, // kg
    val height: Double = 170.0, // cm
    val gender: String = "Male" // Male/Female
) : Parcelable

/**
 * Extension function to convert RouteModel to RouteRequest
 */
fun RouteModel.toRequest(): RouteRequest {
    return RouteRequest(
        userId = userId,
        routeName = routeName,
        distance = distance,
        duration = duration,
        averagePace = averagePace,
        estimatedHeartRate = estimatedHeartRate,
        caloriesBurnt = caloriesBurnt,
        pathPoints = pathPoints,
        dateFormatted = dateFormatted
    )
}

/**
 * Extension function to convert TrackingData to RouteRequest
 */
fun TrackingData.toRouteRequest(userId: String, routeName: String, dateFormatted: String): RouteRequest {
    return RouteRequest(
        userId = userId,
        routeName = routeName,
        distance = distance,
        duration = duration,
        averagePace = averagePace,
        estimatedHeartRate = estimatedHeartRate,
        caloriesBurnt = caloriesBurnt,
        pathPoints = pathPoints.toList(),
        dateFormatted = dateFormatted
    )
}