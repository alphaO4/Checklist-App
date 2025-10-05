package com.feuerwehr.checklist.data.sync

import com.feuerwehr.checklist.data.local.entity.SyncStatus
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles conflict resolution for offline sync
 * Implements various strategies for resolving sync conflicts
 */
@Singleton
class ConflictResolver @Inject constructor() {

    /**
     * Resolve conflicts between local and remote data
     */
    fun <T : SyncableEntity> resolveConflicts(
        local: List<T>,
        remote: List<T>,
        strategy: ConflictResolutionStrategy = ConflictResolutionStrategy.LAST_WRITE_WINS
    ): ConflictResolutionResult<T> {
        val conflicts = mutableListOf<DataConflict<T>>()
        val resolved = mutableListOf<T>()
        val toUpload = mutableListOf<T>()
        val toDownload = mutableListOf<T>()

        // Create lookup maps
        val localMap = local.associateBy { it.getId() }
        val remoteMap = remote.associateBy { it.getId() }
        
        // Process all unique IDs
        val allIds = (localMap.keys + remoteMap.keys).distinct()
        
        for (id in allIds) {
            val localEntity = localMap[id]
            val remoteEntity = remoteMap[id]
            
            when {
                // Only local exists - upload to remote
                localEntity != null && remoteEntity == null -> {
                    if (localEntity.getSyncStatus() == SyncStatus.PENDING_UPLOAD) {
                        toUpload.add(localEntity)
                    } else {
                        resolved.add(localEntity)
                    }
                }
                
                // Only remote exists - download to local
                localEntity == null && remoteEntity != null -> {
                    toDownload.add(remoteEntity)
                }
                
                // Both exist - check for conflicts
                localEntity != null && remoteEntity != null -> {
                    val resolution = resolveEntityConflict(localEntity, remoteEntity, strategy)
                    when (resolution) {
                        is EntityResolution.UseLocal -> {
                            resolved.add(localEntity)
                            if (localEntity.getSyncStatus() == SyncStatus.PENDING_UPLOAD) {
                                toUpload.add(localEntity)
                            }
                        }
                        is EntityResolution.UseRemote -> {
                            toDownload.add(remoteEntity)
                        }
                        is EntityResolution.Conflict -> {
                            conflicts.add(
                                DataConflict(
                                    id = id,
                                    local = localEntity,
                                    remote = remoteEntity,
                                    reason = resolution.reason
                                )
                            )
                        }
                    }
                }
            }
        }
        
        return ConflictResolutionResult(
            resolved = resolved,
            toUpload = toUpload,
            toDownload = toDownload,
            conflicts = conflicts
        )
    }

    /**
     * Resolve conflict between two entities
     */
    private fun <T : SyncableEntity> resolveEntityConflict(
        local: T,
        remote: T,
        strategy: ConflictResolutionStrategy
    ): EntityResolution<T> {
        return when (strategy) {
            ConflictResolutionStrategy.LAST_WRITE_WINS -> {
                val localTime = local.getLastModified()
                val remoteTime = remote.getLastModified()
                
                when {
                    localTime > remoteTime -> EntityResolution.UseLocal(local)
                    remoteTime > localTime -> EntityResolution.UseRemote(remote)
                    else -> {
                        // Same timestamp - check version
                        val localVersion = local.getVersion()
                        val remoteVersion = remote.getVersion()
                        
                        when {
                            localVersion > remoteVersion -> EntityResolution.UseLocal(local)
                            remoteVersion > localVersion -> EntityResolution.UseRemote(remote)
                            else -> EntityResolution.Conflict(
                                "Same timestamp and version",
                                local,
                                remote
                            )
                        }
                    }
                }
            }
            
            ConflictResolutionStrategy.REMOTE_WINS -> EntityResolution.UseRemote(remote)
            
            ConflictResolutionStrategy.LOCAL_WINS -> EntityResolution.UseLocal(local)
            
            ConflictResolutionStrategy.MANUAL_RESOLUTION -> EntityResolution.Conflict(
                "Manual resolution required",
                local,
                remote
            )
        }
    }
}

/**
 * Interface for entities that can be synchronized
 */
interface SyncableEntity {
    fun getId(): Int
    fun getLastModified(): Instant
    fun getVersion(): Int
    fun getSyncStatus(): SyncStatus
}

/**
 * Conflict resolution strategies
 */
enum class ConflictResolutionStrategy {
    LAST_WRITE_WINS,    // Use entity with latest timestamp
    REMOTE_WINS,        // Always prefer remote data
    LOCAL_WINS,         // Always prefer local data  
    MANUAL_RESOLUTION   // Mark as conflict for manual resolution
}

/**
 * Result of conflict resolution process
 */
data class ConflictResolutionResult<T : SyncableEntity>(
    val resolved: List<T>,
    val toUpload: List<T>,
    val toDownload: List<T>,
    val conflicts: List<DataConflict<T>>
)

/**
 * Represents a data conflict between local and remote
 */
data class DataConflict<T : SyncableEntity>(
    val id: Int,
    val local: T,
    val remote: T,
    val reason: String
)

/**
 * Entity resolution result
 */
sealed class EntityResolution<T : SyncableEntity> {
    data class UseLocal<T : SyncableEntity>(val entity: T) : EntityResolution<T>()
    data class UseRemote<T : SyncableEntity>(val entity: T) : EntityResolution<T>()
    data class Conflict<T : SyncableEntity>(
        val reason: String,
        val local: T,
        val remote: T
    ) : EntityResolution<T>()
}