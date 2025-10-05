package com.feuerwehr.checklist.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
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
 * Immediate sync worker for manual refresh operations
 * Triggered by user actions like pull-to-refresh
 */
@HiltWorker
class ImmediateSyncWorker @AssistedInject constructor(
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

            // Update sync state to show active sync
            syncManager.updateSyncState(
                SyncState(
                    isOnline = true,
                    isSyncing = true
                )
            )

            // Perform immediate sync with higher priority
            val results = performImmediateSync()
            
            // Update final sync state
            val hasErrors = results.any { !it }
            syncManager.updateSyncState(
                SyncState(
                    isOnline = true,
                    lastSyncTime = Clock.System.now(),
                    isSyncing = false,
                    lastError = if (hasErrors) "Some operations failed during sync" else null
                )
            )

            if (hasErrors) Result.retry() else Result.success()
            
        } catch (e: Exception) {
            syncManager.updateSyncState(
                SyncState(
                    isOnline = false,
                    isSyncing = false,
                    lastError = "Immediate sync failed: ${e.message}"
                )
            )
            Result.failure()
        }
    }

    /**
     * Perform immediate sync operations with focus on user-critical data
     */
    private suspend fun performImmediateSync(): List<Boolean> = coroutineScope {
        listOf(
            // Priority order: User-visible data first
            async { syncVehicles() },
            async { syncChecklists() },
            async { uploadPendingExecutions() },
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

    private suspend fun uploadPendingExecutions(): Boolean {
        return try {
            checklistRepository.uploadPendingExecutions()
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