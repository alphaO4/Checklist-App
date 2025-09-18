package com.feuerwehr.checklist.data.network

import com.feuerwehr.checklist.data.models.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit API interface for Fire Department Checklist App
 * Corresponds to FastAPI backend endpoints
 */
interface ChecklistApiService {
    
    // ================== Authentication Endpoints ==================
    
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>
    
    @GET("auth/me")
    suspend fun getCurrentUser(): Response<UserDto>
    
    @POST("auth/logout")
    suspend fun logout(): Response<Map<String, String>>
    
    @POST("auth/users")
    suspend fun createUser(@Body request: UserDto): Response<UserDto>
    
    @GET("auth/users")
    suspend fun listUsers(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 50,
        @Query("rolle") rolle: String? = null
    ): Response<List<UserDto>>
    
    // ================== Vehicle Type Endpoints ==================
    
    @GET("vehicle-types")
    suspend fun getVehicleTypes(): Response<List<VehicleTypeDto>>
    
    @POST("vehicle-types")
    suspend fun createVehicleType(@Body vehicleType: VehicleTypeDto): Response<VehicleTypeDto>
    
    @PUT("vehicle-types/{id}")
    suspend fun updateVehicleType(
        @Path("id") id: Int,
        @Body vehicleType: VehicleTypeDto
    ): Response<VehicleTypeDto>
    
    @DELETE("vehicle-types/{id}")
    suspend fun deleteVehicleType(@Path("id") id: Int): Response<Map<String, String>>
    
    // ================== Vehicle Group Endpoints ==================
    
    @GET("fahrzeuggruppen")
    suspend fun getVehicleGroups(
        @Query("gruppe_id") gruppeId: Int? = null
    ): Response<List<VehicleGroupDto>>
    
    @POST("fahrzeuggruppen")
    suspend fun createVehicleGroup(@Body vehicleGroup: VehicleGroupDto): Response<VehicleGroupDto>
    
    @PUT("fahrzeuggruppen/{id}")
    suspend fun updateVehicleGroup(
        @Path("id") id: Int,
        @Body vehicleGroup: VehicleGroupDto
    ): Response<VehicleGroupDto>
    
    @DELETE("fahrzeuggruppen/{id}")
    suspend fun deleteVehicleGroup(@Path("id") id: Int): Response<Map<String, String>>
    
    // ================== Vehicle Endpoints ==================
    
    @GET("vehicles")
    suspend fun getVehicles(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 50,
        @Query("kennzeichen") kennzeichen: String? = null,
        @Query("fahrzeugtyp_id") fahrzeugtypId: Int? = null,
        @Query("fahrzeuggruppe_id") fahrzeuggruppeId: Int? = null
    ): Response<List<VehicleDto>>
    
    @GET("vehicles/{id}")
    suspend fun getVehicle(@Path("id") id: Int): Response<VehicleDto>
    
    @POST("vehicles")
    suspend fun createVehicle(@Body request: CreateVehicleRequest): Response<VehicleDto>
    
    @PUT("vehicles/{id}")
    suspend fun updateVehicle(
        @Path("id") id: Int,
        @Body request: UpdateVehicleRequest
    ): Response<VehicleDto>
    
    @DELETE("vehicles/{id}")
    suspend fun deleteVehicle(@Path("id") id: Int): Response<Map<String, String>>
    
    // ================== TÜV Termine Endpoints ==================
    
    @GET("tuv")
    suspend fun getTuvTermine(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 50,
        @Query("status") status: String? = null,
        @Query("fahrzeug_id") fahrzeugId: Int? = null
    ): Response<List<TuvTerminDto>>
    
    @GET("tuv/{id}")
    suspend fun getTuvTermin(@Path("id") id: Int): Response<TuvTerminDto>
    
    @POST("tuv")
    suspend fun createTuvTermin(@Body request: CreateTuvTerminRequest): Response<TuvTerminDto>
    
    @PUT("tuv/{id}")
    suspend fun updateTuvTermin(
        @Path("id") id: Int,
        @Body request: UpdateTuvTerminRequest
    ): Response<TuvTerminDto>
    
    @DELETE("tuv/{id}")
    suspend fun deleteTuvTermin(@Path("id") id: Int): Response<Map<String, String>>
    
    @GET("tuv/alerts")
    suspend fun getTuvAlerts(): Response<List<TuvTerminDto>>
    
    // ================== Groups Endpoints ==================
    
    @GET("groups")
    suspend fun getGroups(): Response<List<GroupDto>>
    
    @POST("groups")
    suspend fun createGroup(@Body group: GroupDto): Response<GroupDto>
    
    @PUT("groups/{id}")
    suspend fun updateGroup(
        @Path("id") id: Int,
        @Body group: GroupDto
    ): Response<GroupDto>
    
    @DELETE("groups/{id}")
    suspend fun deleteGroup(@Path("id") id: Int): Response<Map<String, String>>
    
    // ================== Checklist Endpoints ==================
    
    @GET("checklists")
    suspend fun getChecklists(
        @Query("fahrzeuggruppe_id") fahrzeuggruppeId: Int? = null,
        @Query("template") template: Boolean? = null
    ): Response<List<ChecklistDto>>
    
    @GET("checklists/{id}")
    suspend fun getChecklist(@Path("id") id: Int): Response<ChecklistDto>
    
    @POST("checklists")
    suspend fun createChecklist(@Body request: CreateChecklistRequest): Response<ChecklistDto>
    
    @PUT("checklists/{id}")
    suspend fun updateChecklist(
        @Path("id") id: Int,
        @Body request: CreateChecklistRequest
    ): Response<ChecklistDto>
    
    @DELETE("checklists/{id}")
    suspend fun deleteChecklist(@Path("id") id: Int): Response<Map<String, String>>
    
    // ================== Checklist Execution Endpoints ==================
    
    @GET("checklists/executions")
    suspend fun getChecklistExecutions(
        @Query("fahrzeug_id") fahrzeugId: Int? = null,
        @Query("benutzer_id") benutzerId: Int? = null,
        @Query("status") status: String? = null
    ): Response<List<ChecklistAusfuehrungDto>>
    
    @GET("checklists/executions/{id}")
    suspend fun getChecklistExecution(@Path("id") id: Int): Response<ChecklistAusfuehrungDto>
    
    @POST("checklists/executions")
    suspend fun startChecklistExecution(@Body request: StartChecklistRequest): Response<ChecklistAusfuehrungDto>
    
    @PUT("checklists/executions/{id}/complete")
    suspend fun completeChecklistExecution(@Path("id") id: Int): Response<ChecklistAusfuehrungDto>
    
    @PUT("checklists/executions/{id}/cancel")
    suspend fun cancelChecklistExecution(@Path("id") id: Int): Response<ChecklistAusfuehrungDto>
    
    @DELETE("checklists/executions/{id}")
    suspend fun deleteChecklistExecution(@Path("id") id: Int): Response<Map<String, String>>
    
    // ================== Item Result Endpoints ==================
    
    @PUT("checklists/executions/{executionId}/items/{itemId}")
    suspend fun updateItemResult(
        @Path("executionId") executionId: Int,
        @Path("itemId") itemId: Int,
        @Body request: UpdateItemErgebnisRequest
    ): Response<ItemErgebnisDto>
    
    // ================== CSV Import Endpoints ==================
    
    @POST("checklists/import-csv-templates")
    suspend fun importCsvTemplates(): Response<Map<String, Any>>
    
    @GET("checklists/csv-summary")
    suspend fun getCsvSummary(): Response<Map<String, Any>>
    
    // ================== Health Check ==================
    
    @GET("health")
    suspend fun healthCheck(): Response<Map<String, String>>
    
    @GET("health/db")
    suspend fun databaseHealthCheck(): Response<Map<String, String>>
}