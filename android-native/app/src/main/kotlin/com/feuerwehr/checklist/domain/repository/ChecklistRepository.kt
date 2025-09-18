package com.feuerwehr.checklist.domain.repository

import com.feuerwehr.checklist.domain.model.*
import com.feuerwehr.checklist.data.mapper.ChecklistPage
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Checklist operations
 * Follows clean architecture - domain layer interface
 */
interface ChecklistRepository {
    
    // Checklist operations
    fun getAllChecklists(): Flow<List<Checklist>>
    fun getChecklistsByVehicleGroup(groupId: Int): Flow<List<Checklist>>
    suspend fun getChecklistById(id: Int): Checklist?
    suspend fun getChecklistWithItems(id: Int): Checklist?
    suspend fun createChecklist(checklist: Checklist): Result<Checklist>
    suspend fun updateChecklist(checklist: Checklist): Result<Checklist>
    suspend fun deleteChecklist(id: Int): Result<Unit>
    
    // Remote operations with pagination
    suspend fun fetchChecklistsFromRemote(
        page: Int = 1,
        perPage: Int = 50,
        vehicleGroupId: Int? = null,
        template: Boolean? = null,
        name: String? = null
    ): Result<ChecklistPage>
    
    /**
     * Perform a full sync of all templates from remote, including handling deletions
     * This ensures that deleted templates (like test templates) are removed locally
     */
    suspend fun syncAllTemplatesFromRemote(): Result<Unit>
    
    // Template operations
    fun getTemplates(): Flow<List<Checklist>>
    fun getNonTemplateChecklists(): Flow<List<Checklist>>
    fun searchChecklistsByName(nameFilter: String): Flow<List<Checklist>>
    
    // Checklist Items
    fun getChecklistItems(checklistId: Int): Flow<List<ChecklistItem>>
    suspend fun createChecklistItem(item: ChecklistItem): Result<ChecklistItem>
    suspend fun updateChecklistItem(item: ChecklistItem): Result<ChecklistItem>
    suspend fun deleteChecklistItem(itemId: Int): Result<Unit>
    
    // Execution operations
    fun getAllExecutions(): Flow<List<ChecklistExecution>>
    fun getExecutionsByChecklist(checklistId: Int): Flow<List<ChecklistExecution>>
    fun getExecutionsByVehicle(vehicleId: Int): Flow<List<ChecklistExecution>>
    suspend fun startExecution(checklistId: Int, vehicleId: Int): Result<ChecklistExecution>
    suspend fun getExecutionById(id: Int): ChecklistExecution?
    suspend fun completeExecution(executionId: Int): Result<ChecklistExecution>
    
    // Item Results
    fun getItemResultsByExecution(executionId: Int): Flow<List<ItemResult>>
    suspend fun submitItemResult(result: ItemResult): Result<ItemResult>
    suspend fun updateItemResult(result: ItemResult): Result<ItemResult>
    
    // Sync operations
    suspend fun syncChecklists(): Result<Unit>
    suspend fun syncChecklistExecutions(): Result<Unit>
}