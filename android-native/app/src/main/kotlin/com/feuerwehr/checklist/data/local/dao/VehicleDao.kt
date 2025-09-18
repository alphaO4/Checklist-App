package com.feuerwehr.checklist.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Insert
import androidx.room.Update
import androidx.room.Delete
import androidx.room.OnConflictStrategy
import com.feuerwehr.checklist.data.local.entity.VehicleEntity
import com.feuerwehr.checklist.data.local.entity.VehicleGroupEntity
import com.feuerwehr.checklist.data.local.entity.VehicleTypeEntity
import com.feuerwehr.checklist.data.local.entity.ChecklistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleDao {
    
    // Vehicle queries
    @Query("SELECT * FROM fahrzeuge ORDER BY kennzeichen ASC")
    fun getAllVehiclesFlow(): Flow<List<VehicleEntity>>
    
    @Query("SELECT * FROM fahrzeuge WHERE id = :id")
    suspend fun getVehicleById(id: Int): VehicleEntity?
    
    @Query("SELECT * FROM fahrzeuge WHERE fahrzeuggruppeId = :groupId ORDER BY kennzeichen ASC")
    fun getVehiclesByGroupFlow(groupId: Int): Flow<List<VehicleEntity>>
    
    @Query("SELECT * FROM fahrzeuge WHERE kennzeichen LIKE :search ORDER BY kennzeichen ASC")
    fun searchVehiclesFlow(search: String): Flow<List<VehicleEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicle(vehicle: VehicleEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicles(vehicles: List<VehicleEntity>)
    
    @Update
    suspend fun updateVehicle(vehicle: VehicleEntity)
    
    @Delete
    suspend fun deleteVehicle(vehicle: VehicleEntity)
    
    @Query("DELETE FROM fahrzeuge WHERE id = :id")
    suspend fun deleteVehicleById(id: Int)
    
    // Vehicle Group queries
    @Query("SELECT * FROM fahrzeuggruppen ORDER BY name ASC")
    fun getAllVehicleGroupsFlow(): Flow<List<VehicleGroupEntity>>
    
    @Query("SELECT * FROM fahrzeuggruppen WHERE id = :id")
    suspend fun getVehicleGroupById(id: Int): VehicleGroupEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicleGroup(group: VehicleGroupEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicleGroups(groups: List<VehicleGroupEntity>)
    
    @Update
    suspend fun updateVehicleGroup(group: VehicleGroupEntity)
    
    @Delete
    suspend fun deleteVehicleGroup(group: VehicleGroupEntity)
    
    // Vehicle Type queries
    @Query("SELECT * FROM fahrzeugtypen ORDER BY name ASC")
    fun getAllVehicleTypesFlow(): Flow<List<VehicleTypeEntity>>
    
    @Query("SELECT * FROM fahrzeugtypen WHERE id = :id")
    suspend fun getVehicleTypeById(id: Int): VehicleTypeEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicleType(type: VehicleTypeEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicleTypes(types: List<VehicleTypeEntity>)
    
    @Update
    suspend fun updateVehicleType(type: VehicleTypeEntity)
    
    @Delete
    suspend fun deleteVehicleType(type: VehicleTypeEntity)
    
    // Vehicle-Checklist relationship queries
    
    /**
     * Get all checklists available for a specific vehicle through its fahrzeuggruppe
     */
    @Query("""
        SELECT c.* FROM checklisten c
        INNER JOIN fahrzeuge v ON v.fahrzeuggruppeId = c.fahrzeuggrupeId
        WHERE v.id = :vehicleId
        ORDER BY c.name ASC
    """)
    fun getChecklistsForVehicleFlow(vehicleId: Int): Flow<List<ChecklistEntity>>
    
    /**
     * Get all available (non-template) checklists for a specific vehicle
     */
    @Query("""
        SELECT c.* FROM checklisten c
        INNER JOIN fahrzeuge v ON v.fahrzeuggruppeId = c.fahrzeuggrupeId
        WHERE v.id = :vehicleId AND c.template = 0
        ORDER BY c.name ASC
    """)
    fun getAvailableChecklistsForVehicleFlow(vehicleId: Int): Flow<List<ChecklistEntity>>
    
    /**
     * Get all vehicles that can use a specific checklist (same fahrzeuggruppe)
     */
    @Query("""
        SELECT v.* FROM fahrzeuge v
        INNER JOIN checklisten c ON c.fahrzeuggrupeId = v.fahrzeuggruppeId
        WHERE c.id = :checklistId
        ORDER BY v.kennzeichen ASC
    """)
    fun getVehiclesForChecklistFlow(checklistId: Int): Flow<List<VehicleEntity>>
    
    /**
     * Check if a vehicle has an active execution for a specific checklist
     */
    @Query("""
        SELECT COUNT(*) > 0 FROM checklist_ausfuehrungen ca
        WHERE ca.fahrzeugId = :vehicleId 
        AND ca.checklisteId = :checklistId 
        AND ca.status = 'started'
    """)
    suspend fun hasActiveExecution(vehicleId: Int, checklistId: Int): Boolean
    
    /**
     * Get active checklist execution ID for a vehicle-checklist combination
     */
    @Query("""
        SELECT ca.id FROM checklist_ausfuehrungen ca
        WHERE ca.fahrzeugId = :vehicleId 
        AND ca.checklisteId = :checklistId 
        AND ca.status = 'started'
        LIMIT 1
    """)
    suspend fun getActiveExecutionId(vehicleId: Int, checklistId: Int): Int?
}