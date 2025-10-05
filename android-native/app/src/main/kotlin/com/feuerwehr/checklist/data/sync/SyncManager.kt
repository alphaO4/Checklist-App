package com.feuerwehr.checklist.data.sync

import android.content.Context
import androidx.work.*
import com.feuerwehr.checklist.data.local.entity.SyncStatus
import com.feuerwehr.checklist.domain.repository.AuthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central sync manager for offline-first functionality
 * Manages background synchronization using WorkManager
 */
@Singleton
class SyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository
) {
    private val workManager = WorkManager.getInstance(context)
    
    private val _syncState = MutableStateFlow(SyncState())
    val syncState: Flow<SyncState> = _syncState.asStateFlow()
    
    companion object {
        private const val PERIODIC_SYNC_WORK_NAME = "periodic_sync"
        private const val IMMEDIATE_SYNC_WORK_NAME = "immediate_sync"
        private const val SYNC_INTERVAL_HOURS = 1L
    }
    
    /**
     * Initialize periodic sync when app starts
     */
    fun startPeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresStorageNotLow(true)
            .build()
            
        val periodicSyncRequest = PeriodicWorkRequestBuilder<PeriodicSyncWorker>(
            SYNC_INTERVAL_HOURS, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()
            
        workManager.enqueueUniquePeriodicWork(
            PERIODIC_SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicSyncRequest
        )
    }
    
    /**
     * Trigger immediate sync (manual refresh)
     */
    fun triggerImmediateSync(): Flow<SyncResult> {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
            
        val immediateSyncRequest = OneTimeWorkRequestBuilder<ImmediateSyncWorker>()
            .setConstraints(constraints)
            .addTag(IMMEDIATE_SYNC_WORK_NAME)
            .build()
            
        workManager.enqueueUniqueWork(
            IMMEDIATE_SYNC_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            immediateSyncRequest
        )
        
        // Return immediate success (fire and forget approach)
        return kotlinx.coroutines.flow.flowOf(SyncResult.Success("Sync request submitted"))
    }
    
    /**
     * Stop all sync operations
     */
    fun stopSync() {
        workManager.cancelUniqueWork(PERIODIC_SYNC_WORK_NAME)
        workManager.cancelUniqueWork(IMMEDIATE_SYNC_WORK_NAME)
    }
    
    /**
     * Check if user is authenticated for sync
     */
    fun canSync(): Boolean {
        return authRepository.isLoggedIn()
    }
    
    /**
     * Update sync state
     */
    internal fun updateSyncState(state: SyncState) {
        _syncState.value = state
    }
}

/**
 * Sync state data class
 */
data class SyncState(
    val isOnline: Boolean = false,
    val lastSyncTime: kotlinx.datetime.Instant? = null,
    val pendingUploads: Int = 0,
    val pendingDownloads: Int = 0,
    val conflicts: Int = 0,
    val isSyncing: Boolean = false,
    val lastError: String? = null
)

/**
 * Sync result sealed class
 */
sealed class SyncResult {
    data class Success(val message: String) : SyncResult()
    object InProgress : SyncResult()
    data class Error(val message: String) : SyncResult()
}