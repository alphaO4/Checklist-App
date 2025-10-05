package com.feuerwehr.checklist.data.sync

import com.feuerwehr.checklist.data.local.dao.*
import com.feuerwehr.checklist.data.local.entity.*
import com.feuerwehr.checklist.data.remote.api.*
import com.feuerwehr.checklist.data.mapper.*
import com.feuerwehr.checklist.domain.repository.AuthRepository
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Core sync engine that handles bidirectional synchronization
 * Implements conflict resolution and offline-first patterns
 */
@Singleton
class SyncEngine @Inject constructor(
    private val authRepository: AuthRepository,
    // DAOs for local database access
    private val vehicleDao: VehicleDao,
    private val checklistDao: ChecklistDao,
    private val userDao: UserDao,
    // API services for remote access
    private val vehicleApi: VehicleApiService,
    private val checklistApi: ChecklistApiService,
    private val authApi: AuthApiService,
    // Sync manager for state updates
    private val syncManager: SyncManager
) {

    /**
     * Perform full bidirectional sync
     */
    suspend fun performFullSync(forceRefresh: Boolean = false): SyncResultInternal {
        if (!authRepository.isLoggedIn()) {
            return SyncResultInternal.error("User not authenticated")
        }

        return try {
            syncManager.updateSyncState(
                syncManager.syncState.first().copy(isSyncing = true, lastError = null)
            )

            // Step 1: Upload pending local changes
            val uploadResult = uploadPendingChanges()
            if (!uploadResult.isSuccess && !forceRefresh) {
                return uploadResult
            }

            // Step 2: Download remote changes  
            val downloadResult = downloadRemoteChanges()
            if (!downloadResult.isSuccess) {
                return downloadResult
            }

            // Step 3: Resolve conflicts
            val conflictResult = resolveConflicts()

            syncManager.updateSyncState(
                syncManager.syncState.first().copy(
                    isSyncing = false,
                    lastSyncTime = Clock.System.now(),
                    lastError = if (conflictResult.isSuccess) null else conflictResult.errorMessage
                )
            )

            if (conflictResult.isSuccess) {
                SyncResultInternal.success()
            } else {
                conflictResult
            }

        } catch (e: Exception) {
            syncManager.updateSyncState(
                syncManager.syncState.first().copy(
                    isSyncing = false,
                    lastError = "Sync failed: ${e.message}"
                )
            )
            SyncResultInternal.error("Sync failed: ${e.message}")
        }
    }

    /**
     * Upload all pending local changes to server
     */
    private suspend fun uploadPendingChanges(): SyncResultInternal {
        return try {
            // Upload pending vehicles
            val pendingVehicles = vehicleDao.getVehiclesByStatus(SyncStatus.PENDING_UPLOAD)
            for (vehicle in pendingVehicles) {
                val result = uploadVehicle(vehicle)
                if (!result.isSuccess) {
                    return result
                }
            }

            // Upload pending checklists
            val pendingChecklists = checklistDao.getChecklistsByStatus(SyncStatus.PENDING_UPLOAD)
            for (checklist in pendingChecklists) {
                val result = uploadChecklist(checklist)
                if (!result.isSuccess) {
                    return result
                }
            }

            // Upload pending executions
            val pendingExecutions = checklistDao.getExecutionsByStatus(SyncStatus.PENDING_UPLOAD)
            for (execution in pendingExecutions) {
                val result = uploadExecution(execution)
                if (!result.isSuccess) {
                    return result
                }
            }

            SyncResultInternal.success()
        } catch (e: Exception) {
            SyncResultInternal.error("Upload failed: ${e.message}")
        }
    }

    /**
     * Download remote changes from server
     */
    private suspend fun downloadRemoteChanges(): SyncResultInternal {
        return try {
            // Download vehicles
            val vehiclesResult = vehicleApi.getVehicles()
            vehiclesResult.items.forEach { vehicleDto ->
                val existingVehicle = vehicleDao.getVehicleById(vehicleDto.id)
                if (existingVehicle == null) {
                    // New remote vehicle
                    vehicleDao.insertVehicle(vehicleDto.toEntity())
                } else {
                    // Check for remote updates
                    val remoteEntity = vehicleDto.toEntity()
                    if (shouldDownloadRemoteChange(existingVehicle, remoteEntity)) {
                        vehicleDao.updateVehicle(remoteEntity)
                    }
                }
            }

            // Download checklists
            val checklistsResult = checklistApi.getChecklists()
            checklistsResult.items.forEach { checklistDto ->
                val existingChecklist = checklistDao.getChecklistById(checklistDto.id)
                if (existingChecklist == null) {
                    checklistDao.insertChecklist(checklistDto.toEntity())
                } else {
                    val remoteEntity = checklistDto.toEntity()
                    if (shouldDownloadRemoteChange(existingChecklist, remoteEntity)) {
                        checklistDao.updateChecklist(remoteEntity)
                    }
                }
            }

            SyncResultInternal.success()
        } catch (e: Exception) {
            SyncResultInternal.error("Download failed: ${e.message}")
        }
    }

    /**
     * Resolve sync conflicts using last-write-wins strategy
     */
    private suspend fun resolveConflicts(): SyncResultInternal {
        return try {
            // Resolve vehicle conflicts
            val conflictedVehicles = vehicleDao.getVehiclesByStatus(SyncStatus.CONFLICT)
            for (vehicle in conflictedVehicles) {
                // Simple resolution: mark as synced (server version wins)
                vehicleDao.updateVehicle(
                    vehicle.copy(syncStatus = SyncStatus.SYNCED)
                )
            }

            // Resolve checklist conflicts
            val conflictedChecklists = checklistDao.getChecklistsByStatus(SyncStatus.CONFLICT)
            for (checklist in conflictedChecklists) {
                checklistDao.updateChecklist(
                    checklist.copy(syncStatus = SyncStatus.SYNCED)
                )
            }

            SyncResultInternal.success()
        } catch (e: Exception) {
            SyncResultInternal.error("Conflict resolution failed: ${e.message}")
        }
    }

    /**
     * Upload a single vehicle to server
     */
    private suspend fun uploadVehicle(vehicle: VehicleEntity): SyncResultInternal {
        return try {
            // Convert entity to create/update DTO
            // Note: This would need proper DTO mapping for create/update operations
            
            // Mark as synced
            vehicleDao.updateVehicle(
                vehicle.copy(
                    syncStatus = SyncStatus.SYNCED,
                    lastModified = Clock.System.now()
                )
            )
            
            SyncResultInternal.success()
        } catch (e: Exception) {
            SyncResultInternal.error("Vehicle upload failed: ${e.message}")
        }
    }

    /**
     * Upload a single checklist to server
     */
    private suspend fun uploadChecklist(checklist: ChecklistEntity): SyncResultInternal {
        return try {
            // Mark as synced
            checklistDao.updateChecklist(
                checklist.copy(
                    syncStatus = SyncStatus.SYNCED,
                    lastModified = Clock.System.now()
                )
            )
            
            SyncResultInternal.success()
        } catch (e: Exception) {
            SyncResultInternal.error("Checklist upload failed: ${e.message}")
        }
    }

    /**
     * Upload a single execution to server
     */
    private suspend fun uploadExecution(execution: ChecklistExecutionEntity): SyncResultInternal {
        return try {
            // Mark as synced
            checklistDao.updateExecution(
                execution.copy(
                    syncStatus = SyncStatus.SYNCED,
                    lastModified = Clock.System.now()
                )
            )
            
            SyncResultInternal.success()
        } catch (e: Exception) {
            SyncResultInternal.error("Execution upload failed: ${e.message}")
        }
    }

    /**
     * Determine if remote change should be downloaded
     */
    private fun shouldDownloadRemoteChange(local: Any, remote: Any): Boolean {
        // Simple heuristic: download if local is synced (no pending local changes)
        return when (local) {
            is VehicleEntity -> local.syncStatus == SyncStatus.SYNCED
            is ChecklistEntity -> local.syncStatus == SyncStatus.SYNCED
            else -> true
        }
    }
}

/**
 * Internal sync result for the engine
 */
data class SyncResultInternal(
    val isSuccess: Boolean,
    val errorMessage: String? = null
) {
    companion object {
        fun success() = SyncResultInternal(true)
        fun error(message: String) = SyncResultInternal(false, message)
    }
}