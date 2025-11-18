package com.st10028374.vitality_vault.routes.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.st10028374.vitality_vault.database.repository.OfflineRepository
import com.st10028374.vitality_vault.routes.api.ApiResult
import com.st10028374.vitality_vault.routes.models.*
import com.st10028374.vitality_vault.routes.repository.RoutesRepository
import com.st10028374.vitality_vault.routes.utils.TrackingUtils
import kotlinx.coroutines.launch

/**
 * ViewModel for managing routes and tracking data
 * Handles all business logic and data operations with offline support
 */
class RoutesViewModel : ViewModel() {

    private val repository = RoutesRepository()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var offlineRepository: OfflineRepository

    // ==================== TRACKING STATE ====================

    private val _trackingData = MutableLiveData<TrackingData>()
    val trackingData: LiveData<TrackingData> = _trackingData

    private val _isTracking = MutableLiveData(false)
    val isTracking: LiveData<Boolean> = _isTracking

    private val _isPaused = MutableLiveData(false)
    val isPaused: LiveData<Boolean> = _isPaused

    // ==================== ROUTES DATA ====================

    private val _savedRoutes = MutableLiveData<List<RouteModel>>()
    val savedRoutes: LiveData<List<RouteModel>> = _savedRoutes

    private val _currentRoute = MutableLiveData<RouteModel?>()
    val currentRoute: LiveData<RouteModel?> = _currentRoute

    // ==================== UI STATE ====================

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isSaving = MutableLiveData(false)
    val isSaving: LiveData<Boolean> = _isSaving

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _successMessage = MutableLiveData<String?>()
    val successMessage: LiveData<String?> = _successMessage

    private val _saveComplete = MutableLiveData<Boolean>()
    val saveComplete: LiveData<Boolean> = _saveComplete

    // ==================== USER PROFILE ====================

    // Get user ID from Firebase Auth or generate unique ID
    private val userId: String
        get() = auth.currentUser?.uid ?: "user_${System.currentTimeMillis()}"

    // User profile for calculations
    private val userProfile = UserProfile(
        userId = userId,
        age = 30,
        weight = 70.0,
        height = 170.0,
        gender = "Male"
    )

    init {
        _trackingData.value = TrackingData()
    }

    // ==================== INITIALIZATION ====================

    /**
     * Initialize offline repository - MUST be called before using offline methods
     */
    fun initOfflineRepository(context: Context) {
        offlineRepository = OfflineRepository(context.applicationContext)
    }

    // ==================== TRACKING METHODS ====================

    /**
     * Start tracking a new route
     */
    fun startTracking() {
        _isTracking.value = true
        _isPaused.value = false
        _trackingData.value = TrackingData()
        _saveComplete.value = false
    }

    /**
     * Pause current tracking
     */
    fun pauseTracking() {
        _isPaused.value = true
    }

    /**
     * Resume paused tracking
     */
    fun resumeTracking() {
        _isPaused.value = false
    }

    /**
     * Stop tracking
     */
    fun stopTracking() {
        _isTracking.value = false
        _isPaused.value = false
    }

    /**
     * Add a new location point to the current route
     */
    fun addLocationPoint(locationPoint: LocationPoint) {
        val currentData = _trackingData.value ?: TrackingData()
        currentData.pathPoints.add(locationPoint)

        // Calculate distance if we have at least 2 points
        if (currentData.pathPoints.size >= 2) {
            val lastPoint = currentData.pathPoints[currentData.pathPoints.size - 2]
            val distanceAdded = TrackingUtils.calculateDistance(lastPoint, locationPoint)
            currentData.distance += distanceAdded
        }

        _trackingData.value = currentData
    }

    /**
     * Update tracking metrics
     */
    fun updateTrackingData(durationMillis: Long) {
        val currentData = _trackingData.value ?: TrackingData()
        currentData.duration = durationMillis

        if (currentData.distance > 0) {
            // Calculate pace
            currentData.currentPace = TrackingUtils.calculatePace(
                currentData.distance,
                durationMillis
            )
            currentData.averagePace = currentData.currentPace

            // Estimate heart rate
            currentData.estimatedHeartRate = TrackingUtils.estimateHeartRate(
                currentData.averagePace,
                userProfile
            )

            // Calculate calories
            currentData.caloriesBurnt = TrackingUtils.calculateCaloriesBurnt(
                currentData.distance,
                durationMillis,
                userProfile
            )
        }

        _trackingData.value = currentData
    }

    // ==================== OFFLINE SAVE METHODS ====================

    /**
     * Save route to offline storage (Room DB) - Works without internet
     * Automatically syncs to Firestore when online
     */
    fun saveRouteOffline(routeName: String) {
        val data = _trackingData.value ?: return

        viewModelScope.launch {
            _isSaving.value = true

            val finalRouteName = if (routeName.isBlank()) {
                RoutesRepository.generateRouteName()
            } else {
                routeName
            }

            val route = RouteModel(
                userId = userId,
                routeName = finalRouteName,
                distance = data.distance,
                duration = data.duration,
                averagePace = data.averagePace,
                estimatedHeartRate = data.estimatedHeartRate,
                caloriesBurnt = data.caloriesBurnt,
                pathPoints = data.pathPoints.toList(),
                dateFormatted = RoutesRepository.getFormattedDate(),
                createdAt = System.currentTimeMillis()
            )

            val result = offlineRepository.saveRoute(route)
            _isSaving.value = false

            if (result.isSuccess) {
                // Check if data was synced
                val unsyncedCount = offlineRepository.getUnsyncedCount()
                _successMessage.value = if (unsyncedCount.second == 0) {
                    "Route saved and synced!"
                } else {
                    "Route saved offline. Will sync when online."
                }
                _saveComplete.value = true
                loadRoutesOffline()
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to save route"
            }
        }
    }

    /**
     * Load routes from offline storage (Room DB) - Works without internet
     */
    fun loadRoutesOffline() {
        viewModelScope.launch {
            _isLoading.value = true

            val result = offlineRepository.getUserRoutes(userId)
            _isLoading.value = false

            if (result.isSuccess) {
                val routeEntities = result.getOrNull() ?: emptyList()

                // Convert RouteEntity to RouteModel
                val routes = routeEntities.map { entity ->
                    RouteModel(
                        id = entity.id,
                        userId = entity.userId,
                        routeName = entity.routeName,
                        distance = entity.distance,
                        duration = entity.duration,
                        averagePace = entity.averagePace,
                        estimatedHeartRate = entity.estimatedHeartRate,
                        caloriesBurnt = entity.caloriesBurnt,
                        pathPoints = entity.pathPoints,
                        dateFormatted = entity.dateFormatted,
                        timestamp = entity.timestamp,
                        createdAt = entity.createdAt
                    )
                }

                _savedRoutes.value = routes
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to load routes"
            }
        }
    }

    /**
     * Load single route from offline storage by ID
     */
    fun loadRouteOffline(routeId: String) {
        viewModelScope.launch {
            _isLoading.value = true

            val result = offlineRepository.getRouteById(routeId)
            _isLoading.value = false

            if (result.isSuccess) {
                val entity = result.getOrNull()
                if (entity != null) {
                    _currentRoute.value = RouteModel(
                        id = entity.id,
                        userId = entity.userId,
                        routeName = entity.routeName,
                        distance = entity.distance,
                        duration = entity.duration,
                        averagePace = entity.averagePace,
                        estimatedHeartRate = entity.estimatedHeartRate,
                        caloriesBurnt = entity.caloriesBurnt,
                        pathPoints = entity.pathPoints,
                        dateFormatted = entity.dateFormatted,
                        timestamp = entity.timestamp,
                        createdAt = entity.createdAt
                    )
                }
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to load route"
            }
        }
    }

    /**
     * Delete route from offline storage
     */
    fun deleteRouteOffline(routeId: String) {
        viewModelScope.launch {
            _isLoading.value = true

            val result = offlineRepository.deleteRoute(routeId)
            _isLoading.value = false

            if (result.isSuccess) {
                _successMessage.value = "Route deleted successfully"
                loadRoutesOffline()
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to delete route"
            }
        }
    }

    /**
     * Search routes in offline storage
     */
    fun searchRoutesOffline(query: String) {
        viewModelScope.launch {
            _isLoading.value = true

            val result = offlineRepository.searchRoutes(userId, query)
            _isLoading.value = false

            if (result.isSuccess) {
                val routeEntities = result.getOrNull() ?: emptyList()

                // Convert RouteEntity to RouteModel
                val routes = routeEntities.map { entity ->
                    RouteModel(
                        id = entity.id,
                        userId = entity.userId,
                        routeName = entity.routeName,
                        distance = entity.distance,
                        duration = entity.duration,
                        averagePace = entity.averagePace,
                        estimatedHeartRate = entity.estimatedHeartRate,
                        caloriesBurnt = entity.caloriesBurnt,
                        pathPoints = entity.pathPoints,
                        dateFormatted = entity.dateFormatted,
                        timestamp = entity.timestamp,
                        createdAt = entity.createdAt
                    )
                }

                _savedRoutes.value = routes
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Search failed"
            }
        }
    }

    /**
     * Manually sync all unsynced data to Firebase
     */
    fun syncData() {
        viewModelScope.launch {
            _isLoading.value = true

            val result = offlineRepository.syncAllData()
            _isLoading.value = false

            if (result.isSuccess) {
                val (workoutsSynced, routesSynced) = result.getOrDefault(Pair(0, 0))
                _successMessage.value = "Synced $workoutsSynced workouts and $routesSynced routes"
                loadRoutesOffline()
            } else {
                _errorMessage.value = "Sync failed: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    /**
     * Get count of unsynced items
     */
    fun getUnsyncedCount(callback: (workouts: Int, routes: Int) -> Unit) {
        viewModelScope.launch {
            val count = offlineRepository.getUnsyncedCount()
            callback(count.first, count.second)
        }
    }

    // ==================== FIREBASE SAVE METHODS (Original) ====================

    /**
     * Save route to Firebase (direct) - Requires internet
     */
    fun saveRouteToFirebase(routeName: String) {
        val data = _trackingData.value ?: return

        viewModelScope.launch {
            _isSaving.value = true

            val finalRouteName = if (routeName.isBlank()) {
                RoutesRepository.generateRouteName()
            } else {
                routeName
            }

            val route = RouteModel(
                userId = userId,
                routeName = finalRouteName,
                distance = data.distance,
                duration = data.duration,
                averagePace = data.averagePace,
                estimatedHeartRate = data.estimatedHeartRate,
                caloriesBurnt = data.caloriesBurnt,
                pathPoints = data.pathPoints.toList(),
                dateFormatted = RoutesRepository.getFormattedDate(),
                createdAt = System.currentTimeMillis()
            )

            val result = repository.saveRouteToFirebase(route)
            _isSaving.value = false

            if (result.isSuccess) {
                _successMessage.value = "Route saved successfully!"
                _saveComplete.value = true
                loadRoutesFromFirebase()
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message
                    ?: "Failed to save route"
            }
        }
    }

    /**
     * Save route via REST API
     */
    fun saveRouteToApi(routeName: String) {
        val data = _trackingData.value ?: return

        viewModelScope.launch {
            _isSaving.value = true

            val finalRouteName = if (routeName.isBlank()) {
                RoutesRepository.generateRouteName()
            } else {
                routeName
            }

            val request = data.toRouteRequest(
                userId = userId,
                routeName = finalRouteName,
                dateFormatted = RoutesRepository.getFormattedDate()
            )

            when (val result = repository.saveRouteToApi(request)) {
                is ApiResult.Success -> {
                    _isSaving.value = false
                    _successMessage.value = "Route saved successfully!"
                    _saveComplete.value = true
                    loadRoutesFromApi()
                }

                is ApiResult.Error -> {
                    _isSaving.value = false
                    _errorMessage.value = result.message
                }

                is ApiResult.Loading -> {
                    _isSaving.value = true
                }
            }
        }
    }

    // ==================== LOAD METHODS (Original) ====================

    /**
     * Load all routes from Firebase
     */
    fun loadRoutesFromFirebase() {
        viewModelScope.launch {
            _isLoading.value = true

            val result = repository.getRoutesFromFirebase(userId)
            _isLoading.value = false

            if (result.isSuccess) {
                _savedRoutes.value = result.getOrNull() ?: emptyList()
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message
                    ?: "Failed to load routes"
            }
        }
    }

    /**
     * Load all routes from REST API
     */
    fun loadRoutesFromApi() {
        viewModelScope.launch {
            _isLoading.value = true

            when (val result = repository.getRoutesFromApi(userId)) {
                is ApiResult.Success -> {
                    _isLoading.value = false
                    _savedRoutes.value = result.data
                }

                is ApiResult.Error -> {
                    _isLoading.value = false
                    _errorMessage.value = result.message
                }

                is ApiResult.Loading -> {
                    _isLoading.value = true
                }
            }
        }
    }

    /**
     * Load single route from Firebase
     */
    fun loadRouteFromFirebase(routeId: String) {
        viewModelScope.launch {
            _isLoading.value = true

            val result = repository.getRouteByIdFromFirebase(routeId)
            _isLoading.value = false

            if (result.isSuccess) {
                _currentRoute.value = result.getOrNull()
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message
                    ?: "Failed to load route"
            }
        }
    }

    /**
     * Load single route from REST API
     */
    fun loadRouteFromApi(routeId: String) {
        viewModelScope.launch {
            _isLoading.value = true

            when (val result = repository.getRouteByIdFromApi(routeId)) {
                is ApiResult.Success -> {
                    _isLoading.value = false
                    _currentRoute.value = result.data
                }

                is ApiResult.Error -> {
                    _isLoading.value = false
                    _errorMessage.value = result.message
                }

                is ApiResult.Loading -> {
                    _isLoading.value = true
                }
            }
        }
    }

    // ==================== DELETE METHODS (Original) ====================

    /**
     * Delete route from Firebase
     */
    fun deleteRouteFromFirebase(routeId: String) {
        viewModelScope.launch {
            _isLoading.value = true

            val result = repository.deleteRouteFromFirebase(routeId)
            _isLoading.value = false

            if (result.isSuccess) {
                _successMessage.value = "Route deleted successfully"
                loadRoutesFromFirebase()
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message
                    ?: "Failed to delete route"
            }
        }
    }

    /**
     * Delete route from REST API
     */
    fun deleteRouteFromApi(routeId: String) {
        viewModelScope.launch {
            _isLoading.value = true

            when (val result = repository.deleteRouteFromApi(routeId)) {
                is ApiResult.Success -> {
                    _isLoading.value = false
                    _successMessage.value = "Route deleted successfully"
                    loadRoutesFromApi()
                }

                is ApiResult.Error -> {
                    _isLoading.value = false
                    _errorMessage.value = result.message
                }

                is ApiResult.Loading -> {
                    _isLoading.value = true
                }
            }
        }
    }

    // ==================== SEARCH METHODS (Original) ====================

    /**
     * Search routes in Firebase
     */
    fun searchRoutesInFirebase(query: String) {
        viewModelScope.launch {
            _isLoading.value = true

            val result = repository.searchRoutesInFirebase(userId, query)
            _isLoading.value = false

            if (result.isSuccess) {
                _savedRoutes.value = result.getOrNull() ?: emptyList()
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message
                    ?: "Search failed"
            }
        }
    }

    /**
     * Search routes via REST API
     */
    fun searchRoutesInApi(query: String) {
        viewModelScope.launch {
            _isLoading.value = true

            when (val result = repository.searchRoutesInApi(userId, query)) {
                is ApiResult.Success -> {
                    _isLoading.value = false
                    _savedRoutes.value = result.data
                }

                is ApiResult.Error -> {
                    _isLoading.value = false
                    _errorMessage.value = result.message
                }

                is ApiResult.Loading -> {
                    _isLoading.value = true
                }
            }
        }
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Clear success message
     */
    fun clearSuccess() {
        _successMessage.value = null
    }

    /**
     * Reset save complete flag
     */
    fun resetSaveComplete() {
        _saveComplete.value = false
    }
}