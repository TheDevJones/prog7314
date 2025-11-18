package com.st10028374.vitality_vault.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.st10028374.vitality_vault.database.entities.RouteEntity


@Dao
interface RouteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoute(route: RouteEntity)

    @Query("SELECT * FROM routes WHERE userId = :userId ORDER BY createdAt DESC")
    suspend fun getAllRoutes(userId: String): List<RouteEntity>

    @Query("SELECT * FROM routes WHERE id = :routeId")
    suspend fun getRouteById(routeId: String): RouteEntity?

    @Query("SELECT * FROM routes WHERE isSynced = 0")
    suspend fun getUnsyncedRoutes(): List<RouteEntity>

    @Query("UPDATE routes SET isSynced = 1 WHERE id = :routeId")
    suspend fun markAsSynced(routeId: String)

    @Query("DELETE FROM routes WHERE id = :routeId")
    suspend fun deleteRoute(routeId: String)

    @Query("DELETE FROM routes WHERE isSynced = 1")
    suspend fun deleteAllSynced()

    @Query("SELECT * FROM routes WHERE userId = :userId AND routeName LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    suspend fun searchRoutes(userId: String, query: String): List<RouteEntity>
}