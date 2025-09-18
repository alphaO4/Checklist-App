package com.feuerwehr.checklist.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Vehicle-related Data Transfer Objects
 * Mirrors backend Pydantic schemas
 */

data class VehicleTypeDto(
    val id: Int,
    val name: String,
    val beschreibung: String?,
    val aktiv: Boolean,
    @SerializedName("created_at") val createdAt: String
)

data class VehicleGroupDto(
    val id: Int,
    val name: String,
    @SerializedName("created_at") val createdAt: String
)

data class VehicleDto(
    val id: Int,
    val kennzeichen: String,
    @SerializedName("fahrzeugtyp_id") val fahrzeugtypId: Int,
    @SerializedName("fahrzeuggruppe_id") val fahrzeuggruppeId: Int,
    @SerializedName("created_at") val createdAt: String,
    val fahrzeugtyp: VehicleTypeDto?
)

data class VehicleWithGroupDto(
    val id: Int,
    val kennzeichen: String,
    @SerializedName("fahrzeugtyp_id") val fahrzeugtypId: Int,
    @SerializedName("fahrzeuggruppe_id") val fahrzeuggruppeId: Int,
    @SerializedName("created_at") val createdAt: String,
    val fahrzeugtyp: VehicleTypeDto?,
    val fahrzeuggruppe: VehicleGroupDto
)

data class VehicleListDto(
    val items: List<VehicleDto>,
    val total: Int,
    val page: Int,
    @SerializedName("per_page") val perPage: Int,
    @SerializedName("total_pages") val totalPages: Int
)

/**
 * Response DTO for GET /vehicles/{vehicle_id}/checklists
 * Maps to backend response structure
 */
data class VehicleChecklistsDto(
    @SerializedName("vehicle_id") val vehicleId: Int,
    val kennzeichen: String,
    @SerializedName("fahrzeuggruppe_id") val fahrzeuggruppeId: Int,
    val checklists: List<ChecklistSummaryDto>
)

/**
 * Response DTO for GET /vehicles/{vehicle_id}/available-checklists
 * Maps to backend response structure
 */
data class VehicleAvailableChecklistsDto(
    @SerializedName("vehicle_id") val vehicleId: Int,
    val kennzeichen: String,
    @SerializedName("available_checklists") val availableChecklists: List<AvailableChecklistDto>
)

/**
 * Checklist summary DTO for vehicle-checklist responses
 */
data class ChecklistSummaryDto(
    val id: Int,
    val name: String,
    val template: Boolean,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("fahrzeuggruppe_id") val fahrzeuggruppeId: Int
)

/**
 * Available checklist DTO with execution status
 */
data class AvailableChecklistDto(
    val id: Int,
    val name: String,
    @SerializedName("fahrzeuggruppe_id") val fahrzeuggruppeId: Int,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("is_active") val isActive: Boolean,
    @SerializedName("active_execution_id") val activeExecutionId: Int?
)