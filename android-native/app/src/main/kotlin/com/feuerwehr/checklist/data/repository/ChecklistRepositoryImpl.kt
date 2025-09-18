package com.feuerwehr.checklist.data.repository

import android.util.Log
import androidx.room.withTransaction
import com.feuerwehr.checklist.data.local.ChecklistDatabase
import com.feuerwehr.checklist.data.local.dao.ChecklistDao
import com.feuerwehr.checklist.data.local.dao.UserDao
import com.feuerwehr.checklist.data.local.dao.VehicleDao
import com.feuerwehr.checklist.data.local.entity.UserEntity
import com.feuerwehr.checklist.data.local.entity.VehicleGroupEntity
import com.feuerwehr.checklist.data.local.entity.SyncStatus
import com.feuerwehr.checklist.data.remote.api.ChecklistApiService
import com.feuerwehr.checklist.data.remote.api.AuthApiService
import com.feuerwehr.checklist.data.remote.api.VehicleApiService
import com.feuerwehr.checklist.data.mapper.*
import com.feuerwehr.checklist.domain.model.*
import com.feuerwehr.checklist.domain.repository.ChecklistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository implementation for Checklist operations
 * Implements offline-first pattern with Room + Retrofit
 */
@Singleton
class ChecklistRepositoryImpl @Inject constructor(
    private val database: ChecklistDatabase,
    private val checklistDao: ChecklistDao,
    private val checklistApi: ChecklistApiService,
    private val userDao: UserDao,
    private val authApi: AuthApiService,
    private val vehicleDao: VehicleDao,
    private val vehicleApi: VehicleApiService
) : ChecklistRepository {

    // Local data flows (offline-first)
    override fun getAllChecklists(): Flow<List<Checklist>> {
        return checklistDao.getAllChecklistsFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getChecklistsByVehicleGroup(groupId: Int): Flow<List<Checklist>> {
        return checklistDao.getChecklistsByVehicleGroupFlow(groupId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getChecklistById(id: Int): Checklist? {
        return checklistDao.getChecklistById(id)?.toDomain()
    }

    override suspend fun getChecklistWithItems(id: Int): Checklist? {
        val checklist = checklistDao.getChecklistById(id)?.toDomain()
        return if (checklist != null) {
            val items = checklistDao.getChecklistItemsFlow(id).map { entities ->
                entities.map { it.toDomain() }
            }
            // For now, return without items - need to handle Flow properly
            checklist
        } else null
    }

    // Template operations
    override fun getTemplates(): Flow<List<Checklist>> {
        return checklistDao.getTemplatesFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getNonTemplateChecklists(): Flow<List<Checklist>> {
        return checklistDao.getNonTemplateChecklistsFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun searchChecklistsByName(nameFilter: String): Flow<List<Checklist>> {
        return checklistDao.searchChecklistsByNameFlow(nameFilter).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * Perform a full sync that reflects all remote changes including deletions
     * Use this to ensure deleted templates (like removed test templates) are reflected locally
     */
    override suspend fun syncAllTemplatesFromRemote(): Result<Unit> {
        return try {
            Log.d("ChecklistRepository", "Starting full template sync to reflect remote deletions")
            
            database.withTransaction {
                // Sync dependencies first
                syncUsersFromRemote()
                syncVehicleGroupsFromRemote()
                
                // Fetch all templates from remote (large page size to get all)
                val response = checklistApi.getChecklists(
                    page = 1,
                    perPage = 1000, // Large enough to get all templates
                    template = true
                )
                
                val remoteTemplates = response.items.mapNotNull { dto ->
                    try {
                        dto.toEntity().copy(
                            erstellerId = if (dto.erstellerId == 2) null else dto.erstellerId
                        )
                    } catch (e: Exception) {
                        Log.w("ChecklistRepository", "Failed to convert template ${dto.id}: ${e.message}")
                        null
                    }
                }
                
                // Upsert remote templates using timestamp-aware logic
                if (remoteTemplates.isNotEmpty()) {
                    checklistDao.upsertChecklistsIfNewer(remoteTemplates)
                }
                
                // Remove templates that no longer exist on remote
                val remoteTemplateIds = remoteTemplates.map { it.id }
                Log.d("ChecklistRepository", "Remote template IDs: $remoteTemplateIds")
                
                // Clean up deleted templates
                if (remoteTemplateIds.isEmpty()) {
                    // If no remote templates, delete all local templates
                    checklistDao.deleteAllTemplates()
                } else {
                    // Delete templates not in the remote list
                    checklistDao.deleteTemplatesNotInRemote(remoteTemplateIds)
                }
                
                Log.d("ChecklistRepository", "Full template sync completed. ${remoteTemplates.size} templates processed")
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ChecklistRepository", "Error in syncAllTemplatesFromRemote: ${e.message}", e)
            Result.failure(e)
        }
    }

    // Remote operations
    override suspend fun fetchChecklistsFromRemote(
        page: Int,
        perPage: Int,
        vehicleGroupId: Int?,
        template: Boolean?,
        name: String?
    ): Result<ChecklistPage> {
        return try {
            Log.d("ChecklistRepository", "Starting checklist sync from remote with transaction")
            
            // Fetch data from API first
            val response = checklistApi.getChecklists(
                page = page,
                perPage = perPage,
                fahrzeuggrupeId = vehicleGroupId,
                template = template,
                name = name
            )
            
            // Use database transaction to ensure users and checklists are synchronized atomically
            database.withTransaction {
                Log.d("ChecklistRepository", "Starting transaction for user, vehicle group and checklist sync")
                
                // First, sync users and vehicle groups to avoid foreign key constraint issues
                syncUsersFromRemote()
                syncVehicleGroupsFromRemote()
                
                // Store checklists in local database within the same transaction
                // Convert DTOs to entities and handle foreign key constraint issues
                val entities = response.items.mapNotNull { dto ->
                    try {
                        val entity = dto.toEntity()
                        
                        // Clean up invalid foreign key references
                        val cleanedEntity = entity.copy(
                            // Set invalid user IDs to null (nullable field)
                            erstellerId = if (entity.erstellerId != null && entity.erstellerId == 2) {
                                Log.w("ChecklistRepository", "Checklist ${entity.id} references missing user ID 2, setting to null")
                                null
                            } else entity.erstellerId
                        )
                        
                        Log.d("ChecklistRepository", "Processing checklist ${cleanedEntity.id}: ${cleanedEntity.name}")
                        cleanedEntity
                    } catch (e: Exception) {
                        Log.w("ChecklistRepository", "Failed to convert checklist ${dto.id}: ${e.message}")
                        null
                    }
                }
                
                if (entities.isNotEmpty()) {
                    // Use timestamp-aware upsert to ensure newer checklists overwrite older ones
                    checklistDao.upsertChecklistsIfNewer(entities)
                    
                    // Remove checklists that no longer exist on remote (like deleted test templates)
                    val remoteIds = entities.map { it.id }
                    if (remoteIds.isNotEmpty()) {
                        val allLocalChecklists = checklistDao.getAllChecklistsFlow()
                        // For now, we'll handle this during full sync - partial sync shouldn't delete records
                        // checklistDao.deleteChecklistsNotInRemote(remoteIds)
                    }
                } else {
                    Log.w("ChecklistRepository", "No valid checklists to insert after validation")
                }
                
                Log.d("ChecklistRepository", "Successfully saved ${entities.size} checklists to database")
            }
            
            // Return domain model
            Result.success(response.toDomain())
        } catch (e: Exception) {
            Log.e("ChecklistRepository", "Error in fetchChecklistsFromRemote: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Sync users from remote to ensure foreign key constraints are satisfied
     * This is called before syncing checklists to avoid constraint violations
     */
    private suspend fun syncUsersFromRemote() {
        try {
            Log.d("ChecklistRepository", "Starting user sync from remote")
            val response = authApi.getUsers()
            Log.d("ChecklistRepository", "Received ${response.items.size} users from API")
            
            val userEntities = response.items.map { userDto ->
                Log.d("ChecklistRepository", "Processing user: ${userDto.username} (id=${userDto.id})")
                userDto.toUserEntity()
            }
            
            userDao.insertUsers(userEntities)
            Log.d("ChecklistRepository", "Successfully saved ${userEntities.size} users to database")
        } catch (e: Exception) {
            Log.w("ChecklistRepository", "Could not sync users from remote: ${e.message}")
            // Create a dummy user entry to avoid foreign key constraint issues
            try {
                val dummyUser = UserEntity(
                    id = -1,
                    username = "system",
                    email = "system@local.dev",
                    rolle = "system",
                    createdAt = kotlinx.datetime.Clock.System.now()
                )
                userDao.insertUser(dummyUser)
                Log.d("ChecklistRepository", "Created dummy user to avoid foreign key issues")
            } catch (insertError: Exception) {
                Log.d("ChecklistRepository", "Dummy user already exists or insert failed: ${insertError.message}")
            }
        }
    }

    /**
     * Sync vehicle groups from remote to ensure foreign key constraints are satisfied
     * This is called before syncing checklists to avoid constraint violations
     */
    private suspend fun syncVehicleGroupsFromRemote() {
        try {
            Log.d("ChecklistRepository", "Starting vehicle groups sync from remote")
            val vehicleGroups = vehicleApi.getVehicleGroups()
            Log.d("ChecklistRepository", "Received ${vehicleGroups.size} vehicle groups from API")
            
            val vehicleGroupEntities = vehicleGroups.map { vehicleGroupDto ->
                Log.d("ChecklistRepository", "Processing vehicle group: ${vehicleGroupDto.name} (id=${vehicleGroupDto.id})")
                vehicleGroupDto.toEntity()
            }
            
            vehicleDao.insertVehicleGroups(vehicleGroupEntities)
            Log.d("ChecklistRepository", "Successfully saved ${vehicleGroupEntities.size} vehicle groups to database")
        } catch (e: Exception) {
            Log.w("ChecklistRepository", "Could not sync vehicle groups from remote: ${e.message}")
            // Create a dummy vehicle group to avoid foreign key constraint issues
            try {
                val dummyVehicleGroup = VehicleGroupEntity(
                    id = -1,
                    name = "Default Vehicle Group",
                    createdAt = kotlinx.datetime.Clock.System.now()
                )
                vehicleDao.insertVehicleGroup(dummyVehicleGroup)
                Log.d("ChecklistRepository", "Created dummy vehicle group to avoid foreign key issues")
            } catch (insertError: Exception) {
                Log.d("ChecklistRepository", "Dummy vehicle group already exists or insert failed: ${insertError.message}")
            }
        }
    }

    override suspend fun createChecklist(checklist: Checklist): Result<Checklist> {
        return try {
            // Try remote first
            val createDto = com.feuerwehr.checklist.data.remote.dto.ChecklistCreateDto(
                name = checklist.name,
                fahrzeuggrupeId = checklist.fahrzeuggrupeId,
                template = checklist.template != null,
                items = emptyList() // TODO: Map items
            )
            
            val response = checklistApi.createChecklist(createDto)
            
            // Store in local database
            checklistDao.insertChecklist(response.toEntity())
            
            Result.success(response.toDomain())
        } catch (e: Exception) {
            // Fallback to local storage with sync flag
            try {
                val entity = checklist.toEntity().copy(
                    syncStatus = com.feuerwehr.checklist.data.local.entity.SyncStatus.PENDING_UPLOAD
                )
                checklistDao.insertChecklist(entity)
                Result.success(checklist)
            } catch (localE: Exception) {
                Result.failure(localE)
            }
        }
    }

    override suspend fun updateChecklist(checklist: Checklist): Result<Checklist> {
        return try {
            // Update local first
            checklistDao.updateChecklist(checklist.toEntity())
            
            // TODO: Sync to remote in background
            Result.success(checklist)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteChecklist(id: Int): Result<Unit> {
        return try {
            val checklist = checklistDao.getChecklistById(id)
            if (checklist != null) {
                checklistDao.deleteChecklist(checklist)
                // TODO: Sync deletion to remote
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Checklist Items
    override fun getChecklistItems(checklistId: Int): Flow<List<ChecklistItem>> {
        return checklistDao.getChecklistItemsFlow(checklistId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    // Execution operations
    override fun getAllExecutions(): Flow<List<ChecklistExecution>> {
        return checklistDao.getAllChecklistExecutionsFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getExecutionsByChecklist(checklistId: Int): Flow<List<ChecklistExecution>> {
        return checklistDao.getExecutionsByChecklistFlow(checklistId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getExecutionsByVehicle(vehicleId: Int): Flow<List<ChecklistExecution>> {
        return checklistDao.getExecutionsByVehicleFlow(vehicleId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun startExecution(checklistId: Int, vehicleId: Int): Result<ChecklistExecution> {
        return try {
            // Try remote first
            val response = checklistApi.startChecklistExecution(
                checklistId,
                mapOf("fahrzeug_id" to vehicleId)
            )
            
            // Store in local database
            checklistDao.insertChecklistExecution(response.toEntity())
            
            Result.success(response.toEntity().toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getExecutionById(id: Int): ChecklistExecution? {
        return checklistDao.getChecklistExecutionById(id)?.toDomain()
    }

    // Sync operations
    override suspend fun syncChecklists(): Result<Unit> {
        return try {
            // Fetch latest from remote and update local
            val response = checklistApi.getChecklists()
            val entities = response.items.map { it.toEntity() }
            checklistDao.insertChecklists(entities)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncChecklistExecutions(): Result<Unit> {
        // TODO: Implement sync for executions
        return Result.success(Unit)
    }

    // Enhanced checklist item operations
    override suspend fun createChecklistItem(item: ChecklistItem): Result<ChecklistItem> {
        return try {
            val entity = item.toEntity()
            val id = checklistDao.insertChecklistItem(entity)
            val newEntity = entity.copy(id = id.toInt())
            Result.success(newEntity.toDomain())
        } catch (e: Exception) {
            Log.e("ChecklistRepository", "Error creating checklist item", e)
            Result.failure(e)
        }
    }

    override suspend fun updateChecklistItem(item: ChecklistItem): Result<ChecklistItem> {
        return try {
            val entity = item.toEntity()
            checklistDao.updateChecklistItem(entity)
            Result.success(item)
        } catch (e: Exception) {
            Log.e("ChecklistRepository", "Error updating checklist item", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteChecklistItem(itemId: Int): Result<Unit> {
        return try {
            val entity = checklistDao.getChecklistItemById(itemId)
            if (entity != null) {
                checklistDao.deleteChecklistItem(entity)
                Result.success(Unit)
            } else {
                Result.failure(IllegalArgumentException("ChecklistItem not found"))
            }
        } catch (e: Exception) {
            Log.e("ChecklistRepository", "Error deleting checklist item", e)
            Result.failure(e)
        }
    }

    // Enhanced execution operations
    override suspend fun completeExecution(executionId: Int): Result<ChecklistExecution> {
        return try {
            val response = checklistApi.completeExecution(executionId)
            val entity = response.toEntity()
            checklistDao.updateChecklistExecution(entity)
            Result.success(entity.toDomain())
        } catch (e: Exception) {
            Log.e("ChecklistRepository", "Error completing execution", e)
            Result.failure(e)
        }
    }

    // Item results operations
    override fun getItemResultsByExecution(executionId: Int): Flow<List<ItemResult>> {
        return checklistDao.getItemResultsByExecutionFlow(executionId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun submitItemResult(result: ItemResult): Result<ItemResult> {
        return try {
            val createDto = result.toCreateDto()
            val response = checklistApi.submitItemResult(result.ausfuehrungId, createDto)
            val entity = response.toEntity()
            checklistDao.insertItemResult(entity)
            Result.success(entity.toDomain())
        } catch (e: Exception) {
            Log.e("ChecklistRepository", "Error submitting item result", e)
            Result.failure(e)
        }
    }

    override suspend fun updateItemResult(result: ItemResult): Result<ItemResult> {
        return try {
            val createDto = result.toCreateDto()
            val response = checklistApi.updateItemResult(result.ausfuehrungId, result.id, createDto)
            val entity = response.toEntity()
            checklistDao.updateItemResult(entity)
            Result.success(entity.toDomain())
        } catch (e: Exception) {
            Log.e("ChecklistRepository", "Error updating item result", e)
            Result.failure(e)
        }
    }
}

// Extension function to convert UserDto to UserEntity
private fun com.feuerwehr.checklist.data.remote.dto.UserDto.toUserEntity(): com.feuerwehr.checklist.data.local.entity.UserEntity {
    return com.feuerwehr.checklist.data.local.entity.UserEntity(
        id = id,
        username = username,
        email = email,
        rolle = rolle,
        createdAt = parseBackendDate(createdAt),
        syncStatus = com.feuerwehr.checklist.data.local.entity.SyncStatus.SYNCED,
        lastModified = kotlinx.datetime.Clock.System.now()
    )
}

// Helper function to parse date strings from backend  
// Same as in ChecklistMappers.kt - should be extracted to a common util
private fun parseBackendDate(dateString: String): kotlinx.datetime.Instant {
    return try {
        // Try ISO format first (2025-08-29T17:37:21)
        kotlinx.datetime.Instant.parse(dateString)
    } catch (e: Exception) {
        try {
            // Try German format (29.08.2025 17:37:21)
            val parts = dateString.split(" ")
            if (parts.size == 2) {
                val datePart = parts[0].split(".")
                val timePart = parts[1].split(":")
                if (datePart.size == 3 && timePart.size == 3) {
                    val day = datePart[0].toInt()
                    val month = datePart[1].toInt()
                    val year = datePart[2].toInt()
                    val hour = timePart[0].toInt()
                    val minute = timePart[1].toInt()
                    val second = timePart[2].toInt()
                    
                    kotlinx.datetime.LocalDateTime(year, month, day, hour, minute, second)
                        .toInstant(kotlinx.datetime.TimeZone.UTC)
                } else {
                    throw IllegalArgumentException("Invalid German date format: $dateString")
                }
            } else {
                throw IllegalArgumentException("Invalid German date format: $dateString")
            }
        } catch (e2: Exception) {
            // Fallback to current time if parsing fails
            kotlinx.datetime.Clock.System.now()
        }
    }
}

