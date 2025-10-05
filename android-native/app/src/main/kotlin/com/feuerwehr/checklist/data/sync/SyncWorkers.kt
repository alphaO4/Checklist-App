package com.feuerwehr.checklist.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.feuerwehr.checklist.domain.repository.AuthRepository
import com.feuerwehr.checklist.domain.repository.VehicleRepository
import com.feuerwehr.checklist.domain.repository.ChecklistRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Periodic background sync worker
 * Runs every hour to sync data when network is available
 */
@HiltWorker
class PeriodicSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncEngine: SyncEngine
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        return@withContext try {
            val syncResult = syncEngine.performFullSync()
            
            if (syncResult.isSuccess) {
                Result.success()
            } else {
                Result.failure(
                    workDataOf("error" to syncResult.errorMessage)
                )
            }
        } catch (e: Exception) {
            Result.failure(
                workDataOf("error" to "Periodic sync failed: ${e.message}")
            )
        }
    }
}

/**
 * Immediate sync worker for manual refresh
 */
@HiltWorker  
class ImmediateSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncEngine: SyncEngine
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        return@withContext try {
            val syncResult = syncEngine.performFullSync(forceRefresh = true)
            
            if (syncResult.isSuccess) {
                Result.success()
            } else {
                Result.failure(
                    workDataOf("error" to syncResult.errorMessage)
                )
            }
        } catch (e: Exception) {
            Result.failure(
                workDataOf("error" to "Immediate sync failed: ${e.message}")
            )
        }
    }
}