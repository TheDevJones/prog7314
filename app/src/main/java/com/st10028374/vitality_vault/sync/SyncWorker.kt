package com.st10028374.vitality_vault.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.st10028374.vitality_vault.database.repository.OfflineRepository

/**
 * Background worker to sync unsynced data when device comes online
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val offlineRepository = OfflineRepository(context)

    override suspend fun doWork(): Result {
        return try {
            // Get unsynced counts
            val (unsyncedWorkouts, unsyncedRoutes) = offlineRepository.getUnsyncedCount()

            if (unsyncedWorkouts == 0 && unsyncedRoutes == 0) {
                return Result.success()
            }

            // Sync all data
            val syncResult = offlineRepository.syncAllData()

            if (syncResult.isSuccess) {
                val (workoutsSynced, routesSynced) = syncResult.getOrDefault(Pair(0, 0))

                // Show notification about sync success
                showSyncNotification(
                    "Sync Complete",
                    "$workoutsSynced workouts and $routesSynced routes synced"
                )

                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun showSyncNotification(title: String, message: String) {
        // TODO
    }
}