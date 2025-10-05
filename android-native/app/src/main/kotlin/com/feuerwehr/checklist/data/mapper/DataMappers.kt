package com.feuerwehr.checklist.data.mapper

import com.feuerwehr.checklist.data.local.entity.*
import com.feuerwehr.checklist.data.remote.dto.*
import com.feuerwehr.checklist.domain.model.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

/**
 * Mapping utilities for converting between DTOs, Entities, and Domain Models
 */

// Helper function to parse ISO datetime strings from backend
fun parseIsoDateTime(dateString: String): Instant {
    return try {
        // Handle format: "2025-08-29T17:37:21.621277" 
        val cleanedString = if (dateString.contains('.')) {
            dateString.substring(0, 19) // Take only "2025-08-29T17:37:21"
        } else {
            dateString
        }
        LocalDateTime.parse(cleanedString).toInstant(TimeZone.UTC)
    } catch (e: Exception) {
        // Fallback to current time if parsing fails
        Clock.System.now()
    }
}

// ======================== Vehicle Mappings ========================

// VehicleType mappings
fun VehicleTypeDto.toEntity(): VehicleTypeEntity = VehicleTypeEntity(
    id = id,
    name = name,
    beschreibung = beschreibung,
    aktiv = aktiv,
    createdAt = parseIsoDateTime(createdAt),
    syncStatus = SyncStatus.SYNCED,
    lastModified = Clock.System.now(),
    version = 1
)

fun VehicleTypeEntity.toDomain(): VehicleType = VehicleType(
    id = id,
    name = name,
    beschreibung = beschreibung,
    aktiv = aktiv,
    createdAt = createdAt
)

fun VehicleType.toEntity(): VehicleTypeEntity = VehicleTypeEntity(
    id = id,
    name = name,
    beschreibung = beschreibung,
    aktiv = aktiv,
    createdAt = createdAt,
    syncStatus = SyncStatus.PENDING_SYNC,
    lastModified = Clock.System.now(),
    version = 1
)

// VehicleGroup mappings
fun VehicleGroupDto.toEntity(): VehicleGroupEntity = VehicleGroupEntity(
    id = id,
    name = name,
    createdAt = parseIsoDateTime(createdAt),
    syncStatus = SyncStatus.SYNCED,
    lastModified = Clock.System.now(),
    version = 1
)

fun VehicleGroupEntity.toDomain(): VehicleGroup = VehicleGroup(
    id = id,
    name = name,
    createdAt = createdAt
)

fun VehicleGroup.toEntity(): VehicleGroupEntity = VehicleGroupEntity(
    id = id,
    name = name,
    createdAt = createdAt,
    syncStatus = SyncStatus.PENDING_SYNC,
    lastModified = Clock.System.now(),
    version = 1
)

// Vehicle mappings  
fun VehicleDto.toEntity(): VehicleEntity = VehicleEntity(
    id = id,
    kennzeichen = kennzeichen,
    fahrzeugtypId = fahrzeugtypId,
    fahrzeuggruppeId = fahrzeuggruppeId,
    createdAt = parseIsoDateTime(createdAt),
    syncStatus = SyncStatus.SYNCED,
    lastModified = Clock.System.now(),
    version = 1
)

fun VehicleEntity.toDomain(
    vehicleType: VehicleType? = null,
    vehicleGroup: VehicleGroup? = null
): Vehicle {
    // Use provided relationships or create minimal placeholders
    val finalVehicleType = vehicleType ?: VehicleType(
        id = fahrzeugtypId,
        name = "Unknown",
        beschreibung = null,
        aktiv = true,
        createdAt = createdAt
    )
    
    val finalVehicleGroup = vehicleGroup
    
    return Vehicle(
        id = id,
        kennzeichen = kennzeichen,
        fahrzeugtypId = fahrzeugtypId,
        fahrzeuggruppeId = fahrzeuggruppeId,
        createdAt = createdAt,
        fahrzeugtyp = finalVehicleType,
        fahrzeuggruppe = finalVehicleGroup
    )
}

fun Vehicle.toEntity(): VehicleEntity = VehicleEntity(
    id = id,
    kennzeichen = kennzeichen,
    fahrzeugtypId = fahrzeugtypId,
    fahrzeuggruppeId = fahrzeuggruppeId,
    createdAt = createdAt,
    syncStatus = SyncStatus.PENDING_SYNC,
    lastModified = Clock.System.now(),
    version = 1
)

// ======================== Checklist Mappings ========================

fun ChecklistDto.toEntity(): ChecklistEntity = ChecklistEntity(
    id = id,
    name = name,
    fahrzeuggruppeId = fahrzeuggruppeId,
    isTemplate = isTemplate,
    csvFilename = csvFilename,
    createdAt = parseIsoDateTime(createdAt),
    syncStatus = SyncStatus.SYNCED,
    lastModified = Clock.System.now(),
    version = 1
)

fun ChecklistEntity.toDomain(): Checklist = Checklist(
    id = id,
    name = name,
    fahrzeuggruppeId = fahrzeuggruppeId,
    isTemplate = isTemplate,
    csvFilename = csvFilename,
    createdAt = createdAt
)

fun Checklist.toEntity(): ChecklistEntity = ChecklistEntity(
    id = id,
    name = name,
    fahrzeuggruppeId = fahrzeuggruppeId,
    isTemplate = isTemplate,
    csvFilename = csvFilename,
    createdAt = createdAt,
    syncStatus = SyncStatus.PENDING_SYNC,
    lastModified = Clock.System.now(),
    version = 1
)

// ChecklistItem mappings
fun ChecklistItemDto.toEntity(): ChecklistItemEntity = ChecklistItemEntity(
    id = id,
    checklistId = checklistId,
    name = name,
    beschreibung = beschreibung,
    kategorie = kategorie,
    reihenfolge = reihenfolge,
    createdAt = parseIsoDateTime(createdAt),
    syncStatus = SyncStatus.SYNCED,
    lastModified = Clock.System.now(),
    version = 1
)

fun ChecklistItemEntity.toDomain(): ChecklistItem = ChecklistItem(
    id = id,
    checklistId = checklistId,
    name = name,
    beschreibung = beschreibung,
    kategorie = kategorie,
    reihenfolge = reihenfolge,
    createdAt = createdAt
)

fun ChecklistItem.toEntity(): ChecklistItemEntity = ChecklistItemEntity(
    id = id,
    checklistId = checklistId,
    name = name,
    beschreibung = beschreibung,
    kategorie = kategorie,
    reihenfolge = reihenfolge,
    createdAt = createdAt,
    syncStatus = SyncStatus.PENDING_SYNC,
    lastModified = Clock.System.now(),
    version = 1
)

// ChecklistExecution mappings
fun ChecklistExecutionDto.toEntity(): ChecklistExecutionEntity = ChecklistExecutionEntity(
    id = id,
    checklistId = checklistId,
    fahrzeugId = fahrzeugId,
    benutzerId = benutzerId,
    status = status,
    startedAt = parseIsoDateTime(startedAt),
    completedAt = completedAt?.let { parseIsoDateTime(it) },
    notizen = notizen,
    createdAt = parseIsoDateTime(createdAt),
    syncStatus = SyncStatus.SYNCED,
    lastModified = Clock.System.now(),
    version = 1
)

fun ChecklistExecutionEntity.toDomain(): ChecklistExecution = ChecklistExecution(
    id = id,
    checklistId = checklistId,
    fahrzeugId = fahrzeugId,
    benutzerId = benutzerId,
    status = status,
    startedAt = startedAt,
    completedAt = completedAt,
    notizen = notizen,
    createdAt = createdAt
)

fun ChecklistExecution.toEntity(): ChecklistExecutionEntity = ChecklistExecutionEntity(
    id = id,
    checklistId = checklistId,
    fahrzeugId = fahrzeugId,
    benutzerId = benutzerId,
    status = status,
    startedAt = startedAt,
    completedAt = completedAt,
    notizen = notizen,
    createdAt = createdAt,
    syncStatus = SyncStatus.PENDING_SYNC,
    lastModified = Clock.System.now(),
    version = 1
)

// ======================== User Mappings ========================

fun UserDto.toEntity(): UserEntity = UserEntity(
    id = id,
    username = username,
    rolle = rolle,
    createdAt = parseIsoDateTime(createdAt),
    syncStatus = SyncStatus.SYNCED,
    lastModified = Clock.System.now(),
    version = 1
)

fun UserEntity.toDomain(): User = User(
    id = id,
    username = username,
    email = "", // Backend doesn't provide email yet
    rolle = UserRole.fromString(rolle),
    createdAt = createdAt
)

fun User.toEntity(): UserEntity = UserEntity(
    id = id,
    username = username,
    rolle = rolle.value,
    createdAt = createdAt,
    syncStatus = SyncStatus.PENDING_SYNC,
    lastModified = Clock.System.now(),
    version = 1
)