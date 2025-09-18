package com.feuerwehr.checklist.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Objects for Checklist API
 * Mirrors backend Pydantic schemas from app/schemas/checklist.py
 */

data class ChecklistItemDto(
    val id: Int,
    @SerializedName("checkliste_id") val checklisteId: Int,
    val beschreibung: String,
    @SerializedName("item_type") val itemType: String,
    @SerializedName("validation_config") val validationConfig: Map<String, Any>? = null,
    @SerializedName("editable_roles") val editableRoles: List<String>? = null,
    @SerializedName("requires_tuv") val requiresTuv: Boolean = false,
    val subcategories: Map<String, Any>? = null,
    val pflicht: Boolean = true,
    val reihenfolge: Int = 0,
    @SerializedName("created_at") val createdAt: String
)

data class ChecklistItemCreateDto(
    val beschreibung: String,
    @SerializedName("item_type") val itemType: String = "standard",
    @SerializedName("validation_config") val validationConfig: Map<String, Any>? = null,
    @SerializedName("editable_roles") val editableRoles: List<String>? = null,
    @SerializedName("requires_tuv") val requiresTuv: Boolean = false,
    val subcategories: Map<String, Any>? = null,
    val pflicht: Boolean = true,
    val reihenfolge: Int = 0
)

data class ChecklistDto(
    val id: Int,
    val name: String,
    @SerializedName("fahrzeuggruppe_id") val fahrzeuggrupeId: Int,
    @SerializedName("ersteller_id") val erstellerId: Int?,
    val template: Boolean,
    @SerializedName("created_at") val createdAt: String
)

data class ChecklistWithItemsDto(
    val id: Int,
    val name: String,
    @SerializedName("fahrzeuggruppe_id") val fahrzeuggrupeId: Int,
    @SerializedName("ersteller_id") val erstellerId: Int?,
    val template: Boolean,
    @SerializedName("created_at") val createdAt: String,
    val items: List<ChecklistItemDto>
)

data class ChecklistCreateDto(
    val name: String,
    @SerializedName("fahrzeuggruppe_id") val fahrzeuggrupeId: Int,
    val template: Boolean = false,
    val items: List<ChecklistItemCreateDto> = emptyList()
)

data class ChecklistListDto(
    val items: List<ChecklistDto>,
    val total: Int,
    val page: Int,
    @SerializedName("per_page") val perPage: Int,
    @SerializedName("total_pages") val totalPages: Int
)

data class ChecklistExecutionDto(
    val id: Int,
    @SerializedName("checkliste_id") val checklisteId: Int,
    @SerializedName("fahrzeug_id") val fahrzeugId: Int,
    @SerializedName("benutzer_id") val benutzerId: Int,
    val status: String,
    @SerializedName("started_at") val startedAt: String?,
    @SerializedName("completed_at") val completedAt: String?
)

data class ItemResultDto(
    val id: Int,
    @SerializedName("ausfuehrung_id") val ausfuehrungId: Int,
    @SerializedName("item_id") val itemId: Int,
    val status: String,
    val wert: Map<String, Any>? = null,        // Store item value (rating, percentage, etc.)
    val vorhanden: Boolean? = null,            // For standard items - is item present?
    @SerializedName("tuv_datum") val tuvDatum: String? = null,     // TÜV expiration date
    @SerializedName("tuv_status") val tuvStatus: String? = null,   // current, warning, expired
    val menge: Int? = null,                    // For quantity items
    val kommentar: String? = null,
    @SerializedName("created_at") val createdAt: String
)

data class ItemResultCreateDto(
    @SerializedName("item_id") val itemId: Int,
    val status: String,
    val wert: Map<String, Any>? = null,
    val vorhanden: Boolean? = null,
    @SerializedName("tuv_datum") val tuvDatum: String? = null,
    @SerializedName("tuv_status") val tuvStatus: String? = null,
    val menge: Int? = null,
    val kommentar: String? = null
)

/**
 * Response DTO for GET /checklists/{checklist_id}/vehicles
 */
data class ChecklistVehiclesDto(
    @SerializedName("checklist_id") val checklistId: Int,
    @SerializedName("checklist_name") val checklistName: String,
    @SerializedName("fahrzeuggruppe_id") val fahrzeuggruppeId: Int,
    @SerializedName("available_vehicles") val availableVehicles: List<VehicleWithStatusDto>
)

/**
 * Vehicle DTO with execution status for checklist-vehicle responses
 */
data class VehicleWithStatusDto(
    val id: Int,
    val kennzeichen: String,
    val fahrzeugtyp: VehicleTypeStatusDto?,
    @SerializedName("is_active") val isActive: Boolean,
    @SerializedName("active_execution_id") val activeExecutionId: Int?
)

/**
 * Vehicle type DTO for status responses
 */
data class VehicleTypeStatusDto(
    val id: Int,
    val name: String,
    val beschreibung: String?
)