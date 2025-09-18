package com.feuerwehr.checklist.domain.model

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

/**
 * Enum for checklist item types
 * Maps to backend: app/models/checklist.py -> ChecklistItemTypeEnum
 */
enum class ChecklistItemType(val value: String) {
    VEHICLE_INFO("vehicle_info"),
    RATING_1_6("rating_1_6"),
    PERCENTAGE("percentage"),
    ATEMSCHUTZ("atemschutz"),
    STANDARD("standard"),
    QUANTITY("quantity"),
    DATE_CHECK("date_check"),
    STATUS_CHECK("status_check");
    
    companion object {
        fun fromString(value: String): ChecklistItemType = 
            values().find { it.value == value } ?: STANDARD
    }
}

/**
 * Domain model for TÜV Appointment (TuvTermin)  
 * Maps to backend: app/models/checklist.py -> TuvTermin
 */
data class TuvAppointment(
    val id: Int,
    val fahrzeugId: Int,
    val ablaufDatum: LocalDate,           // Expiration date
    val status: TuvStatus,
    val letztePruefung: LocalDate?,       // Last inspection
    val createdAt: Instant
)

enum class TuvStatus(val value: String) {
    REMINDER("reminder"),                 // Default status
    CURRENT("current"),                   // Valid/current
    WARNING("warning"),                   // Expires soon
    EXPIRED("expired");                   // Expired
    
    companion object {
        fun fromString(value: String): TuvStatus = 
            values().find { it.value == value } ?: REMINDER
    }
}

/**
 * Domain model for Checklist (Checkliste)
 * Maps to backend: app/models/checklist.py -> Checkliste
 */
data class Checklist(
    val id: Int,
    val name: String,
    val fahrzeuggrupeId: Int,
    val erstellerId: Int?,                // Creator user ID (nullable)
    val template: Boolean = false,        // Is this a template?
    val createdAt: Instant,
    val items: List<ChecklistItem> = emptyList()
)

/**
 * Domain model for ChecklistItem
 * Maps to backend: app/models/checklist.py -> ChecklistItem
 */
data class ChecklistItem(
    val id: Int,
    val checklisteId: Int,
    val beschreibung: String,             // Description
    val itemType: ChecklistItemType = ChecklistItemType.STANDARD,
    val validationConfig: Map<String, Any>? = null,  // Validation rules as Map
    val editableRoles: List<String>? = null,          // Roles that can edit this item
    val requiresTuv: Boolean = false,
    val subcategories: Map<String, Any>? = null,      // For complex items like Atemschutz
    val pflicht: Boolean = true,                      // Mandatory
    val reihenfolge: Int = 0,                        // Order
    val createdAt: Instant
)

/**
 * Domain model for ChecklistExecution (ChecklistAusfuehrung)
 * Maps to backend: app/models/checklist.py -> ChecklistAusfuehrung
 */
data class ChecklistExecution(
    val id: Int,
    val checklisteId: Int,
    val fahrzeugId: Int,
    val benutzerId: Int,
    val status: ExecutionStatus,
    val startedAt: Instant,
    val completedAt: Instant?,
    val results: List<ItemResult> = emptyList()
)

/**
 * Domain model for ItemResult (ItemErgebnis)
 * Maps to backend: app/models/checklist.py -> ItemErgebnis
 */
data class ItemResult(
    val id: Int,
    val ausfuehrungId: Int,              // Execution ID
    val itemId: Int,
    val status: ItemStatus,
    val wert: Map<String, Any>? = null,          // Store item value (rating, percentage, etc.)
    val vorhanden: Boolean? = null,              // For standard items - is item present?
    val tuvDatum: LocalDate? = null,             // TÜV expiration date
    val tuvStatus: String? = null,               // current, warning, expired
    val menge: Int? = null,                      // For quantity items
    val kommentar: String? = null,               // Comment
    val createdAt: Instant
)

enum class ExecutionStatus(val value: String) {
    STARTED("started"),
    IN_PROGRESS("in_progress"),
    COMPLETED("completed"),
    CANCELLED("cancelled");
    
    companion object {
        fun fromString(value: String): ExecutionStatus = 
            values().find { it.value == value } ?: STARTED
    }
}

enum class ItemStatus(val value: String) {
    OK("ok"),
    FEHLER("fehler"),                    // Error/fault
    NICHT_PRUEFBAR("nicht_pruefbar");    // Not testable
    
    companion object {
        fun fromString(value: String): ItemStatus = 
            values().find { it.value == value } ?: OK
    }
}