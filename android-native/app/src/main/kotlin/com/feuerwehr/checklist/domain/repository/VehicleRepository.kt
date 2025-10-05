package com.feuerwehr.checklist.domain.repository

import com.feuerwehr.checklist.domain.model.Vehicle
import com.feuerwehr.checklist.domain.model.VehicleGroup
import com.feuerwehr.checklist.domain.model.Checklist
import com.feuerwehr.checklist.domain.model.ChecklistExecution
import com.feuerwehr.checklist.domain.model.VehicleChecklistStatus
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Vehicle operations
 * Provides offline-first access to vehicle data with backend sync
 */
interface VehicleRepository {
    
    /**
     * Get all vehicles as a Flow (offline-first)
     * @return Flow of list of vehicles from local database
     */
    fun getVehicles(): Flow<List<Vehicle>>
    
    /**
     * Get a specific vehicle by ID
     * @param id Vehicle ID
     * @return Vehicle or null if not found
     */
    suspend fun getVehicleById(id: Int): Vehicle?
    
    /**
     * Get vehicles by vehicle group ID
     * @param vehicleGroupId ID of the vehicle group
     * @return Flow of vehicles in the specified group
     */
    fun getVehiclesByGroup(vehicleGroupId: Int): Flow<List<Vehicle>>
    
    /**
     * Fetch vehicles from remote API and update local database
     * @return Result with success/failure information
     */
    suspend fun fetchVehiclesFromRemote(): Result<List<Vehicle>>
    
    /**
     * Sync vehicles between local and remote
     * @return Result with success/failure information
     */
    suspend fun syncVehicles(): Result<Unit>
    
    /**
     * Upload pending local vehicle changes to remote
     * @return Result with success/failure information
     */
    suspend fun uploadPendingVehicles(): Result<Unit>
    
    /**
     * Get all vehicle groups
     * @return Flow of vehicle groups
     */
    fun getVehicleGroups(): Flow<List<VehicleGroup>>
    
    /**
     * Search vehicles by kennzeichen (license plate)
     * @param query Search query
     * @return Flow of matching vehicles
     */
    fun searchVehicles(query: String): Flow<List<Vehicle>>
    
    // Vehicle-Checklist relationship methods
    
    /**
     * Get all checklists available for a specific vehicle through its fahrzeuggruppe
     * @param vehicleId Vehicle ID
     * @return Flow of checklists available for the vehicle
     */
    fun getChecklistsForVehicle(vehicleId: Int): Flow<List<Checklist>>
    
    /**
     * Get all available (non-template) checklists for execution on a specific vehicle
     * @param vehicleId Vehicle ID  
     * @return Flow of available checklists with execution status
     */
    fun getAvailableChecklistsForVehicle(vehicleId: Int): Flow<List<VehicleChecklistStatus>>
    
    /**
     * Start a checklist execution for a specific vehicle
     * @param vehicleId Vehicle ID
     * @param checklistId Checklist ID
     * @return Result with execution information or error
     */
    suspend fun startChecklistForVehicle(vehicleId: Int, checklistId: Int): Result<ChecklistExecution>
    
    /**
     * Get vehicles that can use a specific checklist (same fahrzeuggruppe)
     * @param checklistId Checklist ID
     * @return Flow of vehicles that can use the checklist
     */
    fun getVehiclesForChecklist(checklistId: Int): Flow<List<VehicleChecklistStatus>>
    
    /**
     * Check if a vehicle has an active execution for a specific checklist
     * @param vehicleId Vehicle ID
     * @param checklistId Checklist ID
     * @return True if there's an active execution
     */
    suspend fun hasActiveExecution(vehicleId: Int, checklistId: Int): Boolean
    
    /**
     * Fetch vehicle checklists from remote API and update local database
     * @param vehicleId Vehicle ID
     * @return Result with success/failure information
     */
    suspend fun fetchVehicleChecklistsFromRemote(vehicleId: Int): Result<List<Checklist>>
}