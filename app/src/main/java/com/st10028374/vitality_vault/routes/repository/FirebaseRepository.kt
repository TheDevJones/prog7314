package com.st10028374.vitality_vault.routes.repository

import com.st10028374.vitality_vault.routes.models.RouteModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class FirebaseRepository {

    private val db = FirebaseFirestore.getInstance()
    private val routesCollection = db.collection("routes")

    // Save route to Firestore
    suspend fun saveRoute(route: RouteModel): Result<String> {
        return try {
            val docRef = routesCollection.add(route).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Get all routes for a user
    suspend fun getUserRoutes(userId: String): Result<List<RouteModel>> {
        return try {
            val snapshot = routesCollection
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
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

    // Get single route by ID
    suspend fun getRouteById(routeId: String): Result<RouteModel?> {
        return try {
            val snapshot = routesCollection.document(routeId).get().await()
            val route = snapshot.toObject(RouteModel::class.java)?.copy(id = snapshot.id)
            Result.success(route)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Delete route
    suspend fun deleteRoute(routeId: String): Result<Unit> {
        return try {
            routesCollection.document(routeId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Update route
    suspend fun updateRoute(routeId: String, route: RouteModel): Result<Unit> {
        return try {
            routesCollection.document(routeId).set(route).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Search routes by name
    suspend fun searchRoutes(userId: String, searchQuery: String): Result<List<RouteModel>> {
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

    companion object {
        fun generateRouteName(): String {
            val sdf = SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault())
            return "Run on ${sdf.format(Date())}"
        }
    }
}