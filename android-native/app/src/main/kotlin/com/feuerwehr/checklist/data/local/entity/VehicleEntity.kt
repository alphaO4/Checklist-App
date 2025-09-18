package com.feuerwehr.checklist.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index
import kotlinx.datetime.Instant

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
)

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
)

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
)