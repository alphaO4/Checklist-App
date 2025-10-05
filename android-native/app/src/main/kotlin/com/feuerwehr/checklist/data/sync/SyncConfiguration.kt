package com.feuerwehr.checklist.data.sync

import androidx.work.Constraints
import androidx.work.NetworkType
import kotlinx.datetime.DateTimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Configuration for sync operations
 */
object SyncConfiguration {
    
    /**
     * Periodic sync interval (how often to automatically sync)
     */
    val PERIODIC_SYNC_INTERVAL: Duration = 6.hours
    
    /**
     * Retry intervals for failed sync operations
     */
    val RETRY_INTERVALS = listOf(
        30.seconds,
        2.minutes,
        5.minutes,
        15.minutes,
        1.hours
    )
    
    /**
     * Maximum number of retry attempts
     */
    const val MAX_RETRY_ATTEMPTS = 5
    
    /**
     * Sync constraints requiring network connectivity
     */
    val SYNC_CONSTRAINTS = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .setRequiresBatteryNotLow(true)
        .build()
    
    /**
     * Timeout for individual sync operations
     */
    val SYNC_TIMEOUT: Duration = 30.seconds
    
    /**
     * Batch size for syncing entities
     */
    const val SYNC_BATCH_SIZE = 50
    
    /**
     * Tags for different sync workers
     */
    object WorkerTags {
        const val PERIODIC_SYNC = "periodic_sync"
        const val IMMEDIATE_SYNC = "immediate_sync"
        const val VEHICLE_SYNC = "vehicle_sync"
        const val CHECKLIST_SYNC = "checklist_sync"
        const val USER_SYNC = "user_sync"
    }
    
    /**
     * Entity sync priority order (higher priority synced first)
     */
    enum class SyncPriority(val order: Int) {
        USER(1),
        VEHICLE_GROUPS(2),
        VEHICLES(3),
        CHECKLISTS(4),
        CHECKLIST_ITEMS(5),
        EXECUTIONS(6)
    }
}