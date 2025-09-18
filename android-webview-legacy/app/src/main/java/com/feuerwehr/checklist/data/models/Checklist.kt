package com.feuerwehr.checklist.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.datetime.Instant

/**
 * Checklist data models for the Fire Department Checklist App
 * Corresponds to backend Checkliste, ChecklistItem, ChecklistAusfuehrung, and ItemErgebnis models
 */

// Network DTOs for API communication
data class ChecklistDto(
    val id: Int,
    val name: String,
    @SerializedName("fahrzeuggruppe_id")
    val fahrzeuggruppeId: Int,
    @SerializedName("ersteller_id")
    val erstellerId: Int,
    val template: Boolean = false,
    @SerializedName("created_at")
    val createdAt: String,
    val items: List<ChecklistItemDto>? = null,
    val fahrzeuggruppe: VehicleGroupDto? = null
)

data class ChecklistItemDto(
    val id: Int,
    @SerializedName("checkliste_id")
    val checklisteId: Int,
    val beschreibung: String,
    val pflicht: Boolean = false, // Required item
    val reihenfolge: Int = 0, // Order in checklist
    @SerializedName("created_at")
    val createdAt: String
)

data class ChecklistAusfuehrungDto(
    val id: Int,
    @SerializedName("checkliste_id")
    val checklisteId: Int,
    @SerializedName("fahrzeug_id")
    val fahrzeugId: Int,
    @SerializedName("benutzer_id")
    val benutzerId: Int,
    val status: String, // "in_progress", "completed", "cancelled"
    @SerializedName("started_at")
    val startedAt: String,
    @SerializedName("completed_at")
    val completedAt: String?,
    val checkliste: ChecklistDto? = null,
    val fahrzeug: VehicleDto? = null,
    val ergebnisse: List<ItemErgebnisDto>? = null
)

data class ItemErgebnisDto(
    val id: Int,
    @SerializedName("ausfuehrung_id")
    val ausfuehrungId: Int,
    @SerializedName("item_id")
    val itemId: Int,
    val status: String, // "ok", "fehler", "nicht_pruefbar"
    val kommentar: String?,
    @SerializedName("created_at")
    val createdAt: String,
    val item: ChecklistItemDto? = null
)

// Request/Response DTOs
data class CreateChecklistRequest(
    val name: String,
    @SerializedName("fahrzeuggruppe_id")
    val fahrzeuggruppeId: Int,
    val template: Boolean = false,
    val items: List<CreateChecklistItemRequest>
)

data class CreateChecklistItemRequest(
    val beschreibung: String,
    val pflicht: Boolean = false,
    val reihenfolge: Int = 0
)

data class StartChecklistRequest(
    @SerializedName("checkliste_id")
    val checklisteId: Int,
    @SerializedName("fahrzeug_id")
    val fahrzeugId: Int
)

data class UpdateItemErgebnisRequest(
    val status: String,
    val kommentar: String?
)

// Room database entities
@Entity(tableName = "checklists")
data class ChecklistEntity(
    @PrimaryKey
    val id: Int,
    val name: String,
    val fahrzeuggruppeId: Int,
    val erstellerId: Int,
    val template: Boolean = false,
    val createdAt: String,
    val lastSyncedAt: Long = 0L,
    val isLocalOnly: Boolean = false
)

@Entity(tableName = "checklist_items")
data class ChecklistItemEntity(
    @PrimaryKey
    val id: Int,
    val checklisteId: Int,
    val beschreibung: String,
    val pflicht: Boolean = false,
    val reihenfolge: Int = 0,
    val createdAt: String,
    val lastSyncedAt: Long = 0L,
    val isLocalOnly: Boolean = false
)

@Entity(tableName = "checklist_ausfuehrungen")
data class ChecklistAusfuehrungEntity(
    @PrimaryKey
    val id: Int,
    val checklisteId: Int,
    val fahrzeugId: Int,
    val benutzerId: Int,
    val status: String,
    val startedAt: String,
    val completedAt: String?,
    val lastSyncedAt: Long = 0L,
    val isLocalOnly: Boolean = false
)

@Entity(tableName = "item_ergebnisse")
data class ItemErgebnisEntity(
    @PrimaryKey
    val id: Int,
    val ausfuehrungId: Int,
    val itemId: Int,
    val status: String,
    val kommentar: String?,
    val createdAt: String,
    val lastSyncedAt: Long = 0L,
    val isLocalOnly: Boolean = false
)

// Domain models for UI layer
data class Checklist(
    val id: Int,
    val name: String,
    val fahrzeuggruppeId: Int,
    val erstellerId: Int,
    val template: Boolean = false,
    val createdAt: Instant,
    val items: List<ChecklistItem> = emptyList(),
    val fahrzeuggruppe: VehicleGroup? = null,
    val isLocalOnly: Boolean = false
)

data class ChecklistItem(
    val id: Int,
    val checklisteId: Int,
    val beschreibung: String,
    val pflicht: Boolean = false,
    val reihenfolge: Int = 0,
    val createdAt: Instant,
    val isLocalOnly: Boolean = false
)

data class ChecklistAusfuehrung(
    val id: Int,
    val checklisteId: Int,
    val fahrzeugId: Int,
    val benutzerId: Int,
    val status: ChecklistStatus,
    val startedAt: Instant,
    val completedAt: Instant?,
    val checkliste: Checklist? = null,
    val fahrzeug: Vehicle? = null,
    val ergebnisse: List<ItemErgebnis> = emptyList(),
    val isLocalOnly: Boolean = false
) {
    val progress: Float
        get() = if (checkliste?.items?.isEmpty() != false) 0f
                else ergebnisse.size.toFloat() / checkliste.items.size.toFloat()

    val isComplete: Boolean
        get() = status == ChecklistStatus.COMPLETED

    fun getResultForItem(itemId: Int): ItemErgebnis? {
        return ergebnisse.find { it.itemId == itemId }
    }
}

data class ItemErgebnis(
    val id: Int,
    val ausfuehrungId: Int,
    val itemId: Int,
    val status: ItemStatus,
    val kommentar: String?,
    val createdAt: Instant,
    val item: ChecklistItem? = null,
    val isLocalOnly: Boolean = false
)

enum class ChecklistStatus(val value: String, val displayName: String) {
    IN_PROGRESS("in_progress", "In Bearbeitung"),
    COMPLETED("completed", "Abgeschlossen"),
    CANCELLED("cancelled", "Abgebrochen");

    companion object {
        fun fromString(status: String): ChecklistStatus {
            return entries.find { it.value == status } ?: IN_PROGRESS
        }
    }
}

enum class ItemStatus(val value: String, val displayName: String, val isError: Boolean = false) {
    OK("ok", "OK", false),
    FEHLER("fehler", "Fehler", true),
    NICHT_PRUEFBAR("nicht_pruefbar", "Nicht prüfbar", false);

    companion object {
        fun fromString(status: String): ItemStatus {
            return entries.find { it.value == status } ?: OK
        }
    }
}