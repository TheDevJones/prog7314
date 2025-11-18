package com.st10028374.vitality_vault.database.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.google.firebase.firestore.FirebaseFirestore
import com.st10028374.vitality_vault.database.VitalityVaultDatabase
import com.st10028374.vitality_vault.database.entities.RouteEntity
import com.st10028374.vitality_vault.database.entities.WorkoutEntity
import com.st10028374.vitality_vault.routes.models.RouteModel
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Repository for handling offline storage and sync
 */
class OfflineRepository(context: Context) {

    private val database = VitalityVaultDatabase.getDatabase(context)
    private val workoutDao = database.workoutDao()
    private val routeDao = database.routeDao()
    private val firestore = FirebaseFirestore.getInstance()
    private val appContext = context.applicationContext

    // ==================== WORKOUT OPERATIONS ====================

    /**
     * Save workout - saves to Room DB first, then syncs if online
     */
    suspend fun saveWorkout(
        userId: String,
        activityType: String,
        duration: String,
        intensity: String,
        date: String,
        completionPercentage: Int,
        timestamp: java.util.Date
    ): Result<String> {
        return try {
            val workoutId = UUID.randomUUID().toString()

            // Save to Room DB first (offline storage)
            val workoutEntity = WorkoutEntity(
                id = workoutId,
                userId = userId,
                activityType = activityType,
                duration = duration,
                intensity = intensity,
                date = date,
                completionPercentage = completionPercentage,
                timestamp = timestamp,
                isSynced = false
            )

            workoutDao.insertWorkout(workoutEntity)

            // Try to sync immediately if online
            if (isOnline()) {
                syncWorkoutToFirestore(workoutEntity)
            }

            Result.success(workoutId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get all workouts for a user (from Room DB)
     */
    suspend fun getUserWorkouts(userId: String): Result<List<WorkoutEntity>> {
        return try {
            val workouts = workoutDao.getAllWorkouts(userId)
            Result.success(workouts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get recent workouts (from Room DB)
     */
    suspend fun getRecentWorkouts(userId: String): Result<List<WorkoutEntity>> {
        return try {
            val workouts = workoutDao.getRecentWorkouts(userId)
            Result.success(workouts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sync a single workout to Firestore
     */
    private suspend fun syncWorkoutToFirestore(workout: WorkoutEntity): Boolean {
        return try {
            val workoutData = hashMapOf(
                "userId" to workout.userId,
                "activityType" to workout.activityType,
                "duration" to workout.duration,
                "intensity" to workout.intensity,
                "date" to workout.date,
                "completionPercentage" to workout.completionPercentage,
                "timestamp" to workout.timestamp
            )

            firestore.collection("workouts")
                .document(workout.id)
                .set(workoutData)
                .await()

            // Mark as synced in Room DB
            workoutDao.markAsSynced(workout.id)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Sync all unsynced workouts to Firestore
     */
    suspend fun syncAllWorkouts(): Result<Int> {
        return try {
            if (!isOnline()) {
                return Result.failure(Exception("No internet connection"))
            }

            val unsyncedWorkouts = workoutDao.getUnsyncedWorkouts()
            var syncedCount = 0

            unsyncedWorkouts.forEach { workout ->
                if (syncWorkoutToFirestore(workout)) {
                    syncedCount++
                }
            }

            Result.success(syncedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== ROUTE OPERATIONS ====================

    /**
     * Save route - saves to Room DB first, then syncs if online
     */
    suspend fun saveRoute(route: RouteModel): Result<String> {
        return try {
            val routeId = if (route.id.isBlank()) UUID.randomUUID().toString() else route.id

            // Save to Room DB first (offline storage)
            val routeEntity = RouteEntity(
                id = routeId,
                userId = route.userId,
                routeName = route.routeName,
                distance = route.distance,
                duration = route.duration,
                averagePace = route.averagePace,
                estimatedHeartRate = route.estimatedHeartRate,
                caloriesBurnt = route.caloriesBurnt,
                pathPoints = route.pathPoints,
                dateFormatted = route.dateFormatted,
                timestamp = route.timestamp,
                createdAt = route.createdAt,
                isSynced = false
            )

            routeDao.insertRoute(routeEntity)

            // Try to sync immediately if online
            if (isOnline()) {
                syncRouteToFirestore(routeEntity)
            }

            Result.success(routeId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get all routes for a user (from Room DB)
     */
    suspend fun getUserRoutes(userId: String): Result<List<RouteEntity>> {
        return try {
            val routes = routeDao.getAllRoutes(userId)
            Result.success(routes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get route by ID (from Room DB)
     */
    suspend fun getRouteById(routeId: String): Result<RouteEntity?> {
        return try {
            val route = routeDao.getRouteById(routeId)
            Result.success(route)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sync a single route to Firestore
     */
    private suspend fun syncRouteToFirestore(route: RouteEntity): Boolean {
        return try {
            val routeData = hashMapOf(
                "userId" to route.userId,
                "routeName" to route.routeName,
                "distance" to route.distance,
                "duration" to route.duration,
                "averagePace" to route.averagePace,
                "estimatedHeartRate" to route.estimatedHeartRate,
                "caloriesBurnt" to route.caloriesBurnt,
                "pathPoints" to route.pathPoints,
                "dateFormatted" to route.dateFormatted,
                "timestamp" to route.timestamp,
                "createdAt" to route.createdAt
            )

            firestore.collection("routes")
                .document(route.id)
                .set(routeData)
                .await()

            // Mark as synced in Room DB
            routeDao.markAsSynced(route.id)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Sync all unsynced routes to Firestore
     */
    suspend fun syncAllRoutes(): Result<Int> {
        return try {
            if (!isOnline()) {
                return Result.failure(Exception("No internet connection"))
            }

            val unsyncedRoutes = routeDao.getUnsyncedRoutes()
            var syncedCount = 0

            unsyncedRoutes.forEach { route ->
                if (syncRouteToFirestore(route)) {
                    syncedCount++
                }
            }

            Result.success(syncedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete route
     */
    suspend fun deleteRoute(routeId: String): Result<Unit> {
        return try {
            routeDao.deleteRoute(routeId)

            // Also delete from Firestore if online
            if (isOnline()) {
                firestore.collection("routes")
                    .document(routeId)
                    .delete()
                    .await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Search routes
     */
    suspend fun searchRoutes(userId: String, query: String): Result<List<RouteEntity>> {
        return try {
            val routes = routeDao.searchRoutes(userId, query)
            Result.success(routes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Check if device is online
     */
    fun isOnline(): Boolean {
        val connectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /**
     * Get count of unsynced items
     */
    suspend fun getUnsyncedCount(): Pair<Int, Int> {
        val unsyncedWorkouts = workoutDao.getUnsyncedWorkouts().size
        val unsyncedRoutes = routeDao.getUnsyncedRoutes().size
        return Pair(unsyncedWorkouts, unsyncedRoutes)
    }

    /**
     * Sync all unsynced data
     */
    suspend fun syncAllData(): Result<Pair<Int, Int>> {
        return try {
            if (!isOnline()) {
                return Result.failure(Exception("No internet connection"))
            }

            val workoutsSynced = syncAllWorkouts().getOrDefault(0)
            val routesSynced = syncAllRoutes().getOrDefault(0)

            Result.success(Pair(workoutsSynced, routesSynced))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}