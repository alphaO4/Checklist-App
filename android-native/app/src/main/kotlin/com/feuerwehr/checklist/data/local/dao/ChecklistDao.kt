package com.feuerwehr.checklist.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Insert
import androidx.room.Update
import androidx.room.Delete
import androidx.room.OnConflictStrategy
import com.feuerwehr.checklist.data.local.entity.TuvAppointmentEntity
import com.feuerwehr.checklist.data.local.entity.ChecklistEntity
import com.feuerwehr.checklist.data.local.entity.ChecklistItemEntity
import com.feuerwehr.checklist.data.local.entity.ChecklistExecutionEntity
import com.feuerwehr.checklist.data.local.entity.ItemResultEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

@Dao
interface ChecklistDao {
    
    // TÜV Appointment queries
    @Query("SELECT * FROM tuv_termine ORDER BY ablaufDatum ASC")
    fun getAllTuvAppointmentsFlow(): Flow<List<TuvAppointmentEntity>>
    
    @Query("SELECT * FROM tuv_termine WHERE fahrzeugId = :vehicleId ORDER BY ablaufDatum DESC")
    fun getTuvAppointmentsByVehicleFlow(vehicleId: Int): Flow<List<TuvAppointmentEntity>>
    
    @Query("SELECT * FROM tuv_termine WHERE ablaufDatum <= :date ORDER BY ablaufDatum ASC")
    fun getTuvAppointmentsExpiringByFlow(date: LocalDate): Flow<List<TuvAppointmentEntity>>
    
    @Query("SELECT * FROM tuv_termine WHERE status = :status ORDER BY ablaufDatum ASC")
    fun getTuvAppointmentsByStatusFlow(status: String): Flow<List<TuvAppointmentEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTuvAppointment(appointment: TuvAppointmentEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTuvAppointments(appointments: List<TuvAppointmentEntity>)
    
    @Update
    suspend fun updateTuvAppointment(appointment: TuvAppointmentEntity)
    
    @Delete
    suspend fun deleteTuvAppointment(appointment: TuvAppointmentEntity)
    
    // Checklist queries
    @Query("SELECT * FROM checklisten ORDER BY name ASC")
    fun getAllChecklistsFlow(): Flow<List<ChecklistEntity>>
    
    @Query("SELECT * FROM checklisten WHERE id = :id")
    suspend fun getChecklistById(id: Int): ChecklistEntity?
    
    @Query("SELECT * FROM checklisten WHERE fahrzeuggrupeId = :groupId ORDER BY name ASC")
    fun getChecklistsByVehicleGroupFlow(groupId: Int): Flow<List<ChecklistEntity>>
    
    @Query("SELECT * FROM checklisten WHERE erstellerId = :creatorId ORDER BY createdAt DESC")
    fun getChecklistsByCreatorFlow(creatorId: Int): Flow<List<ChecklistEntity>>
    
    @Query("SELECT * FROM checklisten WHERE template = 1 ORDER BY name ASC")
    fun getTemplatesFlow(): Flow<List<ChecklistEntity>>
    
    @Query("SELECT * FROM checklisten WHERE template = 0 ORDER BY createdAt DESC")
    fun getNonTemplateChecklistsFlow(): Flow<List<ChecklistEntity>>
    
    @Query("SELECT * FROM checklisten WHERE name LIKE '%' || :nameFilter || '%' ORDER BY name ASC")
    fun searchChecklistsByNameFlow(nameFilter: String): Flow<List<ChecklistEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChecklist(checklist: ChecklistEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChecklists(checklists: List<ChecklistEntity>)
    
    /**
     * Smart upsert that only updates if the new record is newer based on timestamp comparison
     * Uses lastModified first (for updates) and falls back to createdAt (for new records)
     * This ensures that newer checklists overwrite older ones during sync
     */
    suspend fun upsertChecklistIfNewer(checklist: ChecklistEntity) {
        val existing = getChecklistById(checklist.id)
        if (existing == null) {
            // New record - insert it
            insertChecklist(checklist)
        } else {
            // Compare timestamps - use lastModified for updates, createdAt as fallback
            val newTimestamp = maxOf(checklist.lastModified, checklist.createdAt)
            val existingTimestamp = maxOf(existing.lastModified, existing.createdAt)
            
            if (newTimestamp > existingTimestamp) {
                insertChecklist(checklist)
            }
        }
    }
    
    /**
     * Batch upsert for multiple checklists with timestamp checking
     */
    suspend fun upsertChecklistsIfNewer(checklists: List<ChecklistEntity>) {
        checklists.forEach { checklist ->
            upsertChecklistIfNewer(checklist)
        }
    }
    
    @Update
    suspend fun updateChecklist(checklist: ChecklistEntity)
    
    @Delete
    suspend fun deleteChecklist(checklist: ChecklistEntity)
    
    /**
     * Delete checklists that are no longer present in the remote source
     * This ensures deleted templates (like the test templates we just cleaned up) are removed locally
     */
    @Query("DELETE FROM checklisten WHERE id NOT IN (:remoteIds)")
    suspend fun deleteChecklistsNotInRemote(remoteIds: List<Int>)
    
    /**
     * Delete only TEMPLATES that are no longer present in the remote source
     * This is safer as it won't affect regular checklists, only templates
     */
    @Query("DELETE FROM checklisten WHERE template = 1 AND id NOT IN (:remoteIds)")
    suspend fun deleteTemplatesNotInRemote(remoteIds: List<Int>)
    
    /**
     * Delete all templates from local database
     */
    @Query("DELETE FROM checklisten WHERE template = 1")
    suspend fun deleteAllTemplates()
    
    @Query("DELETE FROM checklist_items WHERE id NOT IN (:remoteIds)")
    suspend fun deleteChecklistItemsNotInRemote(remoteIds: List<Int>)
    
    // Checklist Item queries
    @Query("SELECT * FROM checklist_items WHERE checklisteId = :checklistId ORDER BY reihenfolge ASC")
    fun getChecklistItemsFlow(checklistId: Int): Flow<List<ChecklistItemEntity>>
    
    @Query("SELECT * FROM checklist_items WHERE id = :id")
    suspend fun getChecklistItemById(id: Int): ChecklistItemEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChecklistItem(item: ChecklistItemEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChecklistItems(items: List<ChecklistItemEntity>)
    
    /**
     * Smart upsert for checklist items with timestamp checking
     */
    suspend fun upsertChecklistItemIfNewer(item: ChecklistItemEntity) {
        val existing = getChecklistItemById(item.id)
        if (existing == null) {
            // New record - insert it
            insertChecklistItem(item)
        } else {
            // Compare timestamps - use lastModified for updates, createdAt as fallback
            val newTimestamp = maxOf(item.lastModified, item.createdAt)
            val existingTimestamp = maxOf(existing.lastModified, existing.createdAt)
            
            if (newTimestamp > existingTimestamp) {
                insertChecklistItem(item)
            }
        }
    }
    
    /**
     * Batch upsert for checklist items with timestamp checking
     */
    suspend fun upsertChecklistItemsIfNewer(items: List<ChecklistItemEntity>) {
        items.forEach { item ->
            upsertChecklistItemIfNewer(item)
        }
    }
    
    @Update
    suspend fun updateChecklistItem(item: ChecklistItemEntity)
    
    @Delete
    suspend fun deleteChecklistItem(item: ChecklistItemEntity)
    
    // Checklist Execution queries
    @Query("SELECT * FROM checklist_ausfuehrungen ORDER BY startedAt DESC")
    fun getAllChecklistExecutionsFlow(): Flow<List<ChecklistExecutionEntity>>
    
    @Query("SELECT * FROM checklist_ausfuehrungen WHERE id = :id")
    suspend fun getChecklistExecutionById(id: Int): ChecklistExecutionEntity?
    
    @Query("SELECT * FROM checklist_ausfuehrungen WHERE checklisteId = :checklistId ORDER BY startedAt DESC")
    fun getExecutionsByChecklistFlow(checklistId: Int): Flow<List<ChecklistExecutionEntity>>
    
    @Query("SELECT * FROM checklist_ausfuehrungen WHERE fahrzeugId = :vehicleId ORDER BY startedAt DESC")
    fun getExecutionsByVehicleFlow(vehicleId: Int): Flow<List<ChecklistExecutionEntity>>
    
    @Query("SELECT * FROM checklist_ausfuehrungen WHERE benutzerId = :userId ORDER BY startedAt DESC")
    fun getExecutionsByUserFlow(userId: Int): Flow<List<ChecklistExecutionEntity>>
    
    @Query("SELECT * FROM checklist_ausfuehrungen WHERE status = :status ORDER BY startedAt DESC")
    fun getExecutionsByStatusFlow(status: String): Flow<List<ChecklistExecutionEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChecklistExecution(execution: ChecklistExecutionEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChecklistExecutions(executions: List<ChecklistExecutionEntity>)
    
    @Update
    suspend fun updateChecklistExecution(execution: ChecklistExecutionEntity)
    
    @Delete
    suspend fun deleteChecklistExecution(execution: ChecklistExecutionEntity)
    
    // Item Result queries
    @Query("SELECT * FROM item_ergebnisse WHERE ausfuehrungId = :executionId ORDER BY createdAt ASC")
    fun getItemResultsByExecutionFlow(executionId: Int): Flow<List<ItemResultEntity>>
    
    @Query("SELECT * FROM item_ergebnisse WHERE id = :id")
    suspend fun getItemResultById(id: Int): ItemResultEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItemResult(result: ItemResultEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItemResults(results: List<ItemResultEntity>)
    
    @Update
    suspend fun updateItemResult(result: ItemResultEntity)
    
    @Delete
    suspend fun deleteItemResult(result: ItemResultEntity)

    // Sync-related queries
    @Query("SELECT * FROM checklisten WHERE syncStatus = :status")
    suspend fun getChecklistsByStatus(status: com.feuerwehr.checklist.data.local.entity.SyncStatus): List<ChecklistEntity>

    @Query("SELECT * FROM checklist_ausfuehrungen WHERE syncStatus = :status")
    suspend fun getExecutionsByStatus(status: com.feuerwehr.checklist.data.local.entity.SyncStatus): List<ChecklistExecutionEntity>

    @Query("SELECT COUNT(*) FROM checklisten WHERE syncStatus = 'PENDING_UPLOAD'")
    suspend fun getPendingChecklistUploadCount(): Int

    @Query("SELECT COUNT(*) FROM checklist_ausfuehrungen WHERE syncStatus = 'PENDING_UPLOAD'")
    suspend fun getPendingExecutionUploadCount(): Int

    @Query("SELECT COUNT(*) FROM checklisten WHERE syncStatus = 'CONFLICT'")
    suspend fun getChecklistConflictCount(): Int

    @Query("SELECT COUNT(*) FROM checklist_ausfuehrungen WHERE syncStatus = 'CONFLICT'")
    suspend fun getExecutionConflictCount(): Int

    // Helper method for updating execution status
    @Update
    suspend fun updateExecution(execution: ChecklistExecutionEntity)
}