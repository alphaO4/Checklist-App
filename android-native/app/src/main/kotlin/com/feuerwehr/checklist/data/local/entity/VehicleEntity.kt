package com.feuerwehr.checklist.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index
import kotlinx.datetime.Instant
import com.feuerwehr.checklist.data.sync.SyncableEntity

/**
 * Room entity for VehicleType (FahrzeugTyp)
 * Mirrors backend SQLAlchemy model: app/models/vehicle_type.py -> FahrzeugTyp
 */
@Entity(
    tableName = "fahrzeugtypen",
    indices = [Index(value = ["name"], unique = true)]
)
data class VehicleTypeEntity(
    @PrimaryKey val id: Int,
    val name: String,                    // MTF, RTB, LF, etc.
    val beschreibung: String?,
    val aktiv: Boolean = true,
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
        return other is VehicleTypeEntity &&
               other.getEntityId() == getEntityId() &&
               other.getLastModifiedTime() != getLastModifiedTime() &&
               other.getVersion() != getVersion()
    }
}

/**
 * Room entity for VehicleGroup (FahrzeugGruppe)
 * Mirrors backend SQLAlchemy model: app/models/vehicle.py -> FahrzeugGruppe
 */
@Entity(tableName = "fahrzeuggruppen")
data class VehicleGroupEntity(
    @PrimaryKey val id: Int,
    val name: String,
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
        return other is VehicleGroupEntity &&
               other.getEntityId() == getEntityId() &&
               other.getLastModifiedTime() != getLastModifiedTime() &&
               other.getVersion() != getVersion()
    }
}

/**
 * Room entity for Vehicle (Fahrzeug)
 * Mirrors backend SQLAlchemy model: app/models/vehicle.py -> Fahrzeug
 */
@Entity(
    tableName = "fahrzeuge",
    foreignKeys = [
        ForeignKey(
            entity = VehicleTypeEntity::class,
            parentColumns = ["id"],
            childColumns = ["fahrzeugtypId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = VehicleGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["fahrzeuggruppeId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["kennzeichen"], unique = true),
        Index(value = ["fahrzeugtypId"]),
        Index(value = ["fahrzeuggruppeId"])
    ]
)
data class VehicleEntity(
    @PrimaryKey val id: Int,
    val kennzeichen: String,             // License plate
    val fahrzeugtypId: Int,
    val fahrzeuggruppeId: Int,
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
        return other is VehicleEntity &&
               other.getEntityId() == getEntityId() &&
               other.getLastModifiedTime() != getLastModifiedTime() &&
               other.getVersion() != getVersion()
    }
}