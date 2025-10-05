package com.feuerwehr.checklist.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import com.feuerwehr.checklist.data.sync.SyncableEntity

/**
 * Room entity for TÜV Appointment (TuvTermin)
 * Mirrors backend SQLAlchemy model: app/models/checklist.py -> TuvTermin
 */
@Entity(
    tableName = "tuv_termine",
    foreignKeys = [
        ForeignKey(
            entity = VehicleEntity::class,
            parentColumns = ["id"],
            childColumns = ["fahrzeugId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["fahrzeugId"]),
        Index(value = ["ablaufDatum"]),
        Index(value = ["status"])
    ]
)
data class TuvAppointmentEntity(
    @PrimaryKey val id: Int,
    val fahrzeugId: Int,
    val ablaufDatum: LocalDate,          // Expiration date
    val status: String,                  // TuvStatus.value
    val letztePruefung: LocalDate?,      // Last inspection
    val createdAt: Instant,
    
    // Sync metadata
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val lastModified: Instant = createdAt,
    val version: Int = 1
) : SyncableEntity {
    override fun getEntityId(): String = id.toString()
    override fun getLastModifiedTime(): Instant = lastModified
    override fun getVersion(): Int = version
    override fun getSyncStatus(): SyncStatus = syncStatus
    
    override fun withUpdatedSync(
        syncStatus: SyncStatus,
        lastModified: Instant,
        version: Int
    ): SyncableEntity {
        return copy(
            syncStatus = syncStatus,
            lastModified = lastModified,
            version = version
        )
    }
    
    override fun isConflictWith(other: SyncableEntity): Boolean {
        return other is TuvAppointmentEntity &&
               other.getEntityId() == getEntityId() &&
               other.getLastModifiedTime() != getLastModifiedTime() &&
               other.getVersion() != getVersion()
    }
}

/**
 * Room entity for Checklist (Checkliste)
 * Mirrors backend SQLAlchemy model: app/models/checklist.py -> Checkliste
 */
@Entity(
    tableName = "checklisten",
    foreignKeys = [
        ForeignKey(
            entity = VehicleGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["fahrzeuggrupeId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["erstellerId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["fahrzeuggrupeId"]),
        Index(value = ["erstellerId"]),
        Index(value = ["name"])
    ]
)
data class ChecklistEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val fahrzeuggrupeId: Int,
    val erstellerId: Int?,               // Creator user ID (nullable)
    val template: Boolean = false,       // Is this a template?
    val createdAt: Instant,
    
    // Sync metadata
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val lastModified: Instant = createdAt,
    val version: Int = 1
) : SyncableEntity {
    override fun getEntityId(): String = id.toString()
    override fun getLastModifiedTime(): Instant = lastModified
    override fun getVersion(): Int = version
    override fun getSyncStatus(): SyncStatus = syncStatus
    
    override fun withUpdatedSync(
        syncStatus: SyncStatus,
        lastModified: Instant,
        version: Int
    ): SyncableEntity {
        return copy(
            syncStatus = syncStatus,
            lastModified = lastModified,
            version = version
        )
    }
    
    override fun isConflictWith(other: SyncableEntity): Boolean {
        return other is ChecklistEntity &&
               other.getEntityId() == getEntityId() &&
               other.getLastModifiedTime() != getLastModifiedTime() &&
               other.getVersion() != getVersion()
    }
}

/**
 * Room entity for ChecklistItem
 * Mirrors backend SQLAlchemy model: app/models/checklist.py -> ChecklistItem
 */
@Entity(
    tableName = "checklist_items",
    foreignKeys = [
        ForeignKey(
            entity = ChecklistEntity::class,
            parentColumns = ["id"],
            childColumns = ["checklisteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["checklisteId"]),
        Index(value = ["reihenfolge"])
    ]
)
data class ChecklistItemEntity(
    @PrimaryKey val id: Int,
    val checklisteId: Int,
    val beschreibung: String,            // Description
    val itemType: String,                // ChecklistItemType.value
    val validationConfig: String?,       // JSON string for validation rules
    val editableRoles: String?,          // JSON string for roles list
    val requiresTuv: Boolean = false,
    val subcategories: String?,          // JSON string for subcategories
    val pflicht: Boolean = true,         // Mandatory
    val reihenfolge: Int = 0,           // Order
    val createdAt: Instant,
    
    // Sync metadata
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val lastModified: Instant = createdAt,
    val version: Int = 1
) : SyncableEntity {
    override fun getEntityId(): String = id.toString()
    override fun getLastModifiedTime(): Instant = lastModified
    override fun getVersion(): Int = version
    override fun getSyncStatus(): SyncStatus = syncStatus
    
    override fun withUpdatedSync(
        syncStatus: SyncStatus,
        lastModified: Instant,
        version: Int
    ): SyncableEntity {
        return copy(
            syncStatus = syncStatus,
            lastModified = lastModified,
            version = version
        )
    }
    
    override fun isConflictWith(other: SyncableEntity): Boolean {
        return other is ChecklistItemEntity &&
               other.getEntityId() == getEntityId() &&
               other.getLastModifiedTime() != getLastModifiedTime() &&
               other.getVersion() != getVersion()
    }
}

/**
 * Room entity for ChecklistExecution (ChecklistAusfuehrung)
 * Mirrors backend SQLAlchemy model: app/models/checklist.py -> ChecklistAusfuehrung
 */
@Entity(
    tableName = "checklist_ausfuehrungen",
    foreignKeys = [
        ForeignKey(
            entity = ChecklistEntity::class,
            parentColumns = ["id"],
            childColumns = ["checklisteId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = VehicleEntity::class,
            parentColumns = ["id"],
            childColumns = ["fahrzeugId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["benutzerId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["checklisteId"]),
        Index(value = ["fahrzeugId"]),
        Index(value = ["benutzerId"]),
        Index(value = ["status"])
    ]
)
data class ChecklistExecutionEntity(
    @PrimaryKey val id: Int,
    val checklisteId: Int,
    val fahrzeugId: Int,
    val benutzerId: Int,
    val status: String,                  // ExecutionStatus.value
    val startedAt: Instant,
    val completedAt: Instant?,
    
    // Sync metadata
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val lastModified: Instant,
    val version: Int = 1
) : SyncableEntity {
    override fun getEntityId(): String = id.toString()
    override fun getLastModifiedTime(): Instant = lastModified
    override fun getVersion(): Int = version
    override fun getSyncStatus(): SyncStatus = syncStatus
    
    override fun withUpdatedSync(
        syncStatus: SyncStatus,
        lastModified: Instant,
        version: Int
    ): SyncableEntity {
        return copy(
            syncStatus = syncStatus,
            lastModified = lastModified,
            version = version
        )
    }
    
    override fun isConflictWith(other: SyncableEntity): Boolean {
        return other is ChecklistExecutionEntity &&
               other.getEntityId() == getEntityId() &&
               other.getLastModifiedTime() != getLastModifiedTime() &&
               other.getVersion() != getVersion()
    }
}

/**
 * Room entity for ItemResult (ItemErgebnis)
 * Mirrors backend SQLAlchemy model: app/models/checklist.py -> ItemErgebnis
 */
@Entity(
    tableName = "item_ergebnisse",
    foreignKeys = [
        ForeignKey(
            entity = ChecklistExecutionEntity::class,
            parentColumns = ["id"],
            childColumns = ["ausfuehrungId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ChecklistItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["ausfuehrungId"]),
        Index(value = ["itemId"]),
        Index(value = ["status"])
    ]
)
data class ItemResultEntity(
    @PrimaryKey val id: Int,
    val ausfuehrungId: Int,              // Execution ID
    val itemId: Int,
    val status: String,                  // ItemStatus.value
    val wert: String? = null,            // JSON string for item value
    val vorhanden: Boolean? = null,      // For standard items - is item present?
    val tuvDatum: LocalDate? = null,     // TÜV expiration date
    val tuvStatus: String? = null,       // current, warning, expired
    val menge: Int? = null,              // For quantity items
    val kommentar: String? = null,       // Comment
    val createdAt: Instant,
    
    // Sync metadata
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val lastModified: Instant = createdAt,
    val version: Int = 1
) : SyncableEntity {
    override fun getEntityId(): String = id.toString()
    override fun getLastModifiedTime(): Instant = lastModified
    override fun getVersion(): Int = version
    override fun getSyncStatus(): SyncStatus = syncStatus
    
    override fun withUpdatedSync(
        syncStatus: SyncStatus,
        lastModified: Instant,
        version: Int
    ): SyncableEntity {
        return copy(
            syncStatus = syncStatus,
            lastModified = lastModified,
            version = version
        )
    }
    
    override fun isConflictWith(other: SyncableEntity): Boolean {
        return other is ItemResultEntity &&
               other.getEntityId() == getEntityId() &&
               other.getLastModifiedTime() != getLastModifiedTime() &&
               other.getVersion() != getVersion()
    }
}