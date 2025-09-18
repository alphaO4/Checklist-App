package com.feuerwehr.checklist.data.remote.api

import com.feuerwehr.checklist.data.remote.dto.*
import retrofit2.http.*

/**
 * Retrofit API interface for Checklist endpoints
 * Mirrors backend FastAPI routes from app/api/routes/checklists.py
 */
interface ChecklistApiService {

    /**
     * List checklists with filtering and pagination
     * GET /checklists?page=1&per_page=50&fahrzeuggruppe_id=1&template=false&name=search
     */
    @GET("checklists")
    suspend fun getChecklists(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 50,
        @Query("fahrzeuggruppe_id") fahrzeuggrupeId: Int? = null,
        @Query("template") template: Boolean? = null,
        @Query("name") name: String? = null
    ): ChecklistListDto

    /**
     * Get checklist by ID with items
     * GET /checklists/{checklist_id}
     */
    @GET("checklists/{checklist_id}")
    suspend fun getChecklistById(@Path("checklist_id") checklistId: Int): ChecklistWithItemsDto

    /**
     * Create new checklist with items
     * POST /checklists
     */
    @POST("checklists")
    suspend fun createChecklist(@Body checklist: ChecklistCreateDto): ChecklistWithItemsDto

    /**
     * Update existing checklist
     * PUT /checklists/{checklist_id}
     */
    @PUT("checklists/{checklist_id}")
    suspend fun updateChecklist(
        @Path("checklist_id") checklistId: Int,
        @Body checklist: ChecklistCreateDto
    ): ChecklistDto

    /**
     * Delete checklist
     * DELETE /checklists/{checklist_id}
     */
    @DELETE("checklists/{checklist_id}")
    suspend fun deleteChecklist(@Path("checklist_id") checklistId: Int)

    /**
     * Start checklist execution
     * POST /checklists/{checklist_id}/execute
     */
    @POST("checklists/{checklist_id}/execute")
    suspend fun startChecklistExecution(
        @Path("checklist_id") checklistId: Int,
        @Body execution: Map<String, Int>  // fahrzeug_id
    ): ChecklistExecutionDto

    /**
     * Get checklist executions
     * GET /checklists/{checklist_id}/executions
     */
    @GET("checklists/{checklist_id}/executions")
    suspend fun getChecklistExecutions(
        @Path("checklist_id") checklistId: Int
    ): List<ChecklistExecutionDto>

    /**
     * Get execution by ID with results
     * GET /checklists/executions/{execution_id}
     */
    @GET("checklists/executions/{execution_id}")
    suspend fun getExecutionById(@Path("execution_id") executionId: Int): ChecklistExecutionDto

    /**
     * Submit item result
     * POST /checklists/executions/{execution_id}/results
     */
    @POST("checklists/executions/{execution_id}/results")
    suspend fun submitItemResult(
        @Path("execution_id") executionId: Int,
        @Body result: ItemResultCreateDto
    ): ItemResultDto

    /**
     * Update item result
     * PUT /checklists/executions/{execution_id}/results/{result_id}
     */
    @PUT("checklists/executions/{execution_id}/results/{result_id}")
    suspend fun updateItemResult(
        @Path("execution_id") executionId: Int,
        @Path("result_id") resultId: Int,
        @Body result: ItemResultCreateDto
    ): ItemResultDto

    /**
     * Complete checklist execution
     * POST /checklists/executions/{execution_id}/complete
     */
    @POST("checklists/executions/{execution_id}/complete")
    suspend fun completeExecution(@Path("execution_id") executionId: Int): ChecklistExecutionDto
    
    /**
     * Get all vehicles that can use this checklist (through fahrzeuggruppe)
     * GET /checklists/{checklist_id}/vehicles
     */
    @GET("checklists/{checklist_id}/vehicles")
    suspend fun getChecklistVehicles(@Path("checklist_id") checklistId: Int): ChecklistVehiclesDto
}