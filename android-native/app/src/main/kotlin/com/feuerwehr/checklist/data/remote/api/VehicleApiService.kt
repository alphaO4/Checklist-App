package com.feuerwehr.checklist.data.remote.api

import com.feuerwehr.checklist.data.remote.dto.VehicleDto
import com.feuerwehr.checklist.data.remote.dto.VehicleGroupDto
import com.feuerwehr.checklist.data.remote.dto.VehicleTypeDto
import com.feuerwehr.checklist.data.remote.dto.VehicleListDto
import com.feuerwehr.checklist.data.remote.dto.VehicleChecklistsDto
import com.feuerwehr.checklist.data.remote.dto.VehicleAvailableChecklistsDto
import com.feuerwehr.checklist.data.remote.dto.ChecklistExecutionDto
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit API interface for Vehicle-related endpoints
 */
interface VehicleApiService {

    /**
     * Get all vehicles with pagination
     * GET /vehicles
     */
    @GET("vehicles")
    suspend fun getVehicles(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 50,
        @Query("fahrzeuggruppe_id") vehicleGroupId: Int? = null
    ): VehicleListDto

    /**
     * Get a specific vehicle by ID
     * GET /vehicles/{vehicle_id}
     */
    @GET("vehicles/{vehicle_id}")
    suspend fun getVehicleById(@Path("vehicle_id") vehicleId: Int): VehicleDto

    /**
     * Get all vehicle groups
     * GET /fahrzeuggruppen
     */
    @GET("fahrzeuggruppen")
    suspend fun getVehicleGroups(): List<VehicleGroupDto>

    /**
     * Get available vehicle types
     * GET /vehicle-types
     */
    @GET("vehicle-types")
    suspend fun getVehicleTypes(): List<VehicleTypeDto>
    
    /**
     * Get all checklists available for a specific vehicle through its fahrzeuggruppe
     * GET /vehicles/{vehicle_id}/checklists
     */
    @GET("vehicles/{vehicle_id}/checklists")
    suspend fun getVehicleChecklists(
        @Path("vehicle_id") vehicleId: Int,
        @Query("template") template: Boolean? = null
    ): VehicleChecklistsDto
    
    /**
     * Get all checklists available for execution on a specific vehicle
     * GET /vehicles/{vehicle_id}/available-checklists
     */
    @GET("vehicles/{vehicle_id}/available-checklists")
    suspend fun getAvailableChecklistsForVehicle(
        @Path("vehicle_id") vehicleId: Int
    ): VehicleAvailableChecklistsDto
    
    /**
     * Start a checklist execution for a specific vehicle
     * POST /vehicles/{vehicle_id}/checklists/{checklist_id}/start
     */
    @POST("vehicles/{vehicle_id}/checklists/{checklist_id}/start")
    suspend fun startChecklistForVehicle(
        @Path("vehicle_id") vehicleId: Int,
        @Path("checklist_id") checklistId: Int
    ): ChecklistExecutionDto
}