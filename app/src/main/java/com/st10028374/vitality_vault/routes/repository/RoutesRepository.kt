package com.st10028374.vitality_vault.routes.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.st10028374.vitality_vault.routes.api.ApiResult
import com.st10028374.vitality_vault.routes.api.RetrofitClient
import com.st10028374.vitality_vault.routes.api.safeApiCall
import com.st10028374.vitality_vault.routes.models.*
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

/**
 * Repository for handling both Firebase and API operations
 * This provides a single source of truth for data operations
 */
class RoutesRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val routesCollection = firestore.collection("routes")
    private val apiService = RetrofitClient.apiService

    // ==================== FIREBASE OPERATIONS ====================

    /**
     * Save route to Firebase Firestore
     */
    suspend fun saveRouteToFirebase(route: RouteModel): Result<String> {
        return try {
            val docRef = routesCollection.add(route).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get all routes for a user from Firebase
     */
    suspend fun getRoutesFromFirebase(userId: String): Result<List<RouteModel>> {
        return try {
            val snapshot = routesCollection
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val routes = snapshot.documents.mapNotNull { doc ->
                doc.toObject(RouteModel::class.java)?.copy(id = doc.id)
            }
            Result.success(routes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get single route from Firebase
     */
    suspend fun getRouteByIdFromFirebase(routeId: String): Result<RouteModel?> {
        return try {
            val snapshot = routesCollection.document(routeId).get().await()
            val route = snapshot.toObject(RouteModel::class.java)?.copy(id = snapshot.id)
            Result.success(route)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update route in Firebase
     */
    suspend fun updateRouteInFirebase(routeId: String, route: RouteModel): Result<Unit> {
        return try {
            routesCollection.document(routeId).set(route).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete route from Firebase
     */
    suspend fun deleteRouteFromFirebase(routeId: String): Result<Unit> {
        return try {
            routesCollection.document(routeId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Search routes in Firebase
     */
    suspend fun searchRoutesInFirebase(userId: String, searchQuery: String): Result<List<RouteModel>> {
        return try {
            val snapshot = routesCollection
                .whereEqualTo("userId", userId)
                .orderBy("routeName")
                .startAt(searchQuery)
                .endAt(searchQuery + "\uf8ff")
                .get()
                .await()

            val routes = snapshot.documents.mapNotNull { doc ->
                doc.toObject(RouteModel::class.java)?.copy(id = doc.id)
            }
            Result.success(routes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== REST API OPERATIONS ====================

    /**
     * Save route via REST API
     */
    suspend fun saveRouteToApi(request: RouteRequest): ApiResult<String> {
        return when (val result = safeApiCall { apiService.saveRoute(request) }) {
            is ApiResult.Success -> {
                if (result.data.success) {
                    ApiResult.Success(result.data.routeId ?: "")
                } else {
                    ApiResult.Error(result.data.message)
                }
            }
            is ApiResult.Error -> result
            is ApiResult.Loading -> result
        }
    }

    /**
     * Get all routes for a user via REST API
     */
    suspend fun getRoutesFromApi(userId: String, limit: Int = 50, skip: Int = 0): ApiResult<List<RouteModel>> {
        return when (val result = safeApiCall { apiService.getUserRoutes(userId, limit, skip) }) {
            is ApiResult.Success -> {
                if (result.data.success) {
                    ApiResult.Success(result.data.routes)
                } else {
                    ApiResult.Error(result.data.message ?: "Failed to fetch routes")
                }
            }
            is ApiResult.Error -> result
            is ApiResult.Loading -> result
        }
    }

    /**
     * Get single route via REST API
     */
    suspend fun getRouteByIdFromApi(routeId: String): ApiResult<RouteModel?> {
        return when (val result = safeApiCall { apiService.getRouteById(routeId) }) {
            is ApiResult.Success -> {
                if (result.data.success) {
                    ApiResult.Success(result.data.route)
                } else {
                    ApiResult.Error(result.data.message ?: "Route not found")
                }
            }
            is ApiResult.Error -> result
            is ApiResult.Loading -> result
        }
    }

    /**
     * Update route via REST API
     */
    suspend fun updateRouteInApi(routeId: String, request: RouteRequest): ApiResult<Boolean> {
        return when (val result = safeApiCall { apiService.updateRoute(routeId, request) }) {
            is ApiResult.Success -> {
                ApiResult.Success(result.data.success)
            }
            is ApiResult.Error -> result
            is ApiResult.Loading -> result
        }
    }

    /**
     * Delete route via REST API
     */
    suspend fun deleteRouteFromApi(routeId: String): ApiResult<Boolean> {
        return when (val result = safeApiCall { apiService.deleteRoute(routeId) }) {
            is ApiResult.Success -> {
                ApiResult.Success(result.data.success)
            }
            is ApiResult.Error -> result
            is ApiResult.Loading -> result
        }
    }

    /**
     * Search routes via REST API
     */
    suspend fun searchRoutesInApi(userId: String, searchQuery: String): ApiResult<List<RouteModel>> {
        return when (val result = safeApiCall { apiService.searchRoutes(userId, searchQuery) }) {
            is ApiResult.Success -> {
                if (result.data.success) {
                    ApiResult.Success(result.data.routes)
                } else {
                    ApiResult.Error(result.data.message ?: "Search failed")
                }
            }
            is ApiResult.Error -> result
            is ApiResult.Loading -> result
        }
    }

    /**
     * Get user statistics via REST API
     */
    suspend fun getUserStatsFromApi(userId: String): ApiResult<UserStats?> {
        return when (val result = safeApiCall { apiService.getUserStats(userId) }) {
            is ApiResult.Success -> {
                if (result.data.success) {
                    ApiResult.Success(result.data.stats)
                } else {
                    ApiResult.Error(result.data.message ?: "Failed to fetch stats")
                }
            }
            is ApiResult.Error -> result
            is ApiResult.Loading -> result
        }
    }

    // ==================== UTILITY METHODS ====================

    companion object {
        /**
         * Generate a default route name based on current date/time
         */
        fun generateRouteName(): String {
            val sdf = SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault())
            return "Run on ${sdf.format(Date())}"
        }

        /**
         * Get formatted date string
         */
        fun getFormattedDate(): String {
            val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            return sdf.format(Date())
        }
    }
}