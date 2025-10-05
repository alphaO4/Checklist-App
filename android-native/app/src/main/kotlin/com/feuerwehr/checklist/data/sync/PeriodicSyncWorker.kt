package com.feuerwehr.checklist.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.feuerwehr.checklist.domain.repository.VehicleRepository
import com.feuerwehr.checklist.domain.repository.ChecklistRepository
import com.feuerwehr.checklist.domain.repository.AuthRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.Clock

/**
 * Periodic sync worker for background synchronization
 * Runs periodically to keep data in sync when network is available
 */
@HiltWorker
class PeriodicSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncManager: SyncManager,
    private val vehicleRepository: VehicleRepository,
    private val checklistRepository: ChecklistRepository,
    private val authRepository: AuthRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            if (!syncManager.canSync()) {
                return Result.success()
            }

            syncManager.updateSyncState(
                syncManager.syncState.replayCache.firstOrNull() ?: SyncState()
                    .copy(isSyncing = true, isOnline = true)
            )

            // Perform all sync operations
            val results = performFullSync()
            
            // Update sync state based on results
            val hasErrors = results.any { !it }
            val syncState = SyncState(
                isOnline = true,
                lastSyncTime = Clock.System.now(),
                isSyncing = false,
                lastError = if (hasErrors) "Some sync operations failed" else null
            )
            syncManager.updateSyncState(syncState)

            if (hasErrors) Result.retry() else Result.success()
            
        } catch (e: Exception) {
            syncManager.updateSyncState(
                SyncState(
                    isOnline = false,
                    isSyncing = false,
                    lastError = e.message
                )
            )
            Result.failure()
        }
    }

    /**
     * Perform all sync operations in parallel
     */
    private suspend fun performFullSync(): List<Boolean> = coroutineScope {
        listOf(
            async { syncVehicles() },
            async { syncChecklists() },
            async { syncChecklistExecutions() }
        ).awaitAll()
    }

    private suspend fun syncVehicles(): Boolean {
        return try {
            vehicleRepository.syncVehicles()
            true
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun syncChecklists(): Boolean {
        return try {
            checklistRepository.syncChecklists()
            true
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun syncChecklistExecutions(): Boolean {
        return try {
            checklistRepository.syncExecutions()
            true
        } catch (e: Exception) {
            false
        }
    }
}