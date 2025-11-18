package com.st10028374.vitality_vault.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.st10028374.vitality_vault.database.entities.WorkoutEntity

/**
 * DAO for Workout operations
 */
@Dao
interface WorkoutDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(workout: WorkoutEntity)

    @Query("SELECT * FROM workouts WHERE userId = :userId ORDER BY createdAt DESC")
    suspend fun getAllWorkouts(userId: String): List<WorkoutEntity>

    @Query("SELECT * FROM workouts WHERE userId = :userId ORDER BY createdAt DESC LIMIT 5")
    suspend fun getRecentWorkouts(userId: String): List<WorkoutEntity>

    @Query("SELECT * FROM workouts WHERE isSynced = 0")
    suspend fun getUnsyncedWorkouts(): List<WorkoutEntity>

    @Query("UPDATE workouts SET isSynced = 1 WHERE id = :workoutId")
    suspend fun markAsSynced(workoutId: String)

    @Query("DELETE FROM workouts WHERE id = :workoutId")
    suspend fun deleteWorkout(workoutId: String)

    @Query("DELETE FROM workouts WHERE isSynced = 1")
    suspend fun deleteAllSynced()
}
