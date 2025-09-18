package com.feuerwehr.checklist.domain.usecase

import android.util.Log
import com.feuerwehr.checklist.domain.repository.ChecklistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for synchronizing templates with timestamp-based conflict resolution
 * Ensures that deleted templates (like test templates) are properly removed locally
 */
@Singleton
class SyncTemplatesUseCase @Inject constructor(
    private val checklistRepository: ChecklistRepository
) {
    
    /**
     * Sync all templates from remote, handling deletions and timestamp-based updates
     */
    suspend fun syncTemplates(): Result<Unit> {
        return try {
            Log.d("SyncTemplatesUseCase", "Starting template sync with deletion handling")
            
            val result = checklistRepository.syncAllTemplatesFromRemote()
            
            if (result.isSuccess) {
                Log.d("SyncTemplatesUseCase", "Template sync completed successfully")
            } else {
                Log.e("SyncTemplatesUseCase", "Template sync failed: ${result.exceptionOrNull()?.message}")
            }
            
            result
        } catch (e: Exception) {
            Log.e("SyncTemplatesUseCase", "Template sync error: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Monitor sync status - could be extended to show sync progress
     */
    fun observeSyncStatus(): Flow<SyncStatus> = flow {
        emit(SyncStatus.Idle)
        
        try {
            emit(SyncStatus.Syncing)
            val result = syncTemplates()
            
            if (result.isSuccess) {
                emit(SyncStatus.Success)
            } else {
                emit(SyncStatus.Error(result.exceptionOrNull()?.message ?: "Unknown error"))
            }
        } catch (e: Exception) {
            emit(SyncStatus.Error(e.message ?: "Sync failed"))
        }
    }
}

/**
 * Represents the current sync status
 */
sealed class SyncStatus {
    object Idle : SyncStatus()
    object Syncing : SyncStatus()
    object Success : SyncStatus()
    data class Error(val message: String) : SyncStatus()
}