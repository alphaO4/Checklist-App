package com.feuerwehr.checklist.data.mapper

import com.feuerwehr.checklist.data.local.entity.VehicleEntity
import com.feuerwehr.checklist.data.local.entity.VehicleGroupEntity
import com.feuerwehr.checklist.data.local.entity.VehicleTypeEntity
import com.feuerwehr.checklist.data.remote.dto.VehicleDto
import com.feuerwehr.checklist.data.remote.dto.VehicleTypeDto
import com.feuerwehr.checklist.data.remote.dto.VehicleWithGroupDto
import com.feuerwehr.checklist.data.remote.dto.ChecklistSummaryDto
import com.feuerwehr.checklist.data.remote.dto.AvailableChecklistDto
import com.feuerwehr.checklist.domain.model.Vehicle
import com.feuerwehr.checklist.domain.model.VehicleGroup
import com.feuerwehr.checklist.domain.model.Checklist
import com.feuerwehr.checklist.domain.model.VehicleType
import kotlinx.datetime.Instant

/**
 * Mapper functions for Vehicle-related data conversion
 * DTO ↔ Entity ↔ Domain Model
 * Note: VehicleGroupDto.toEntity() is defined in ChecklistMappers.kt
 */

// DTO to Entity mappings
fun VehicleTypeDto.toEntity(): VehicleTypeEntity {
    return VehicleTypeEntity(
        id = this.id,
        name = this.name,
        beschreibung = this.beschreibung,
        aktiv = this.aktiv,
        createdAt = parseIsoDateTime(this.createdAt)
    )
}

fun VehicleDto.toEntity(): VehicleEntity {
    return VehicleEntity(
        id = this.id,
        kennzeichen = this.kennzeichen,
        fahrzeugtypId = this.fahrzeugtypId,
        fahrzeuggruppeId = this.fahrzeuggruppeId,
        createdAt = parseIsoDateTime(this.createdAt)
    )
}

fun VehicleWithGroupDto.toEntity(): VehicleEntity {
    return VehicleEntity(
        id = this.id,
        kennzeichen = this.kennzeichen,
        fahrzeugtypId = this.fahrzeugtypId,
        fahrzeuggruppeId = this.fahrzeuggruppeId,
        createdAt = parseIsoDateTime(this.createdAt)
    )
}

// Entity to Domain mappings
fun VehicleTypeEntity.toDomain(): VehicleType {
    return VehicleType(
        id = this.id,
        name = this.name,
        beschreibung = this.beschreibung,
        aktiv = this.aktiv,
        createdAt = this.createdAt
    )
}

fun VehicleGroupEntity.toDomain(): VehicleGroup {
    return VehicleGroup(
        id = this.id,
        name = this.name,
        createdAt = this.createdAt
    )
}

fun VehicleEntity.toDomain(
    vehicleType: VehicleType? = null,
    vehicleGroup: VehicleGroup? = null
): Vehicle {
    return Vehicle(
        id = this.id,
        kennzeichen = this.kennzeichen,
        fahrzeugtypId = this.fahrzeugtypId,
        fahrzeuggruppeId = this.fahrzeuggruppeId,
        createdAt = this.createdAt,
        fahrzeugtyp = vehicleType ?: VehicleType(
            id = this.fahrzeugtypId,
            name = "Unbekannt",
            beschreibung = null,
            aktiv = true,
            createdAt = kotlinx.datetime.Clock.System.now()
        ),
        fahrzeuggruppe = vehicleGroup
    )
}

// Domain to Entity mappings (for potential write operations)
fun VehicleType.toEntity(): VehicleTypeEntity {
    return VehicleTypeEntity(
        id = this.id,
        name = this.name,
        beschreibung = this.beschreibung,
        aktiv = this.aktiv,
        createdAt = this.createdAt
    )
}

fun VehicleGroup.toEntity(): VehicleGroupEntity {
    return VehicleGroupEntity(
        id = this.id,
        name = this.name,
        createdAt = this.createdAt
    )
}

fun Vehicle.toEntity(): VehicleEntity {
    return VehicleEntity(
        id = this.id,
        kennzeichen = this.kennzeichen,
        fahrzeugtypId = this.fahrzeugtypId,
        fahrzeuggruppeId = this.fahrzeuggruppeId,
        createdAt = this.createdAt
    )
}

// New DTO mappers for vehicle-checklist functionality

/**
 * Maps ChecklistSummaryDto to Checklist domain model
 */
fun ChecklistSummaryDto.toDomain(): Checklist {
    return Checklist(
        id = this.id,
        name = this.name,
        fahrzeuggrupeId = this.fahrzeuggruppeId,
        erstellerId = null, // Not included in summary
        template = this.template,
        createdAt = parseIsoDateTime(this.createdAt),
        items = emptyList() // Summary doesn't include items
    )
}

/**
 * Maps AvailableChecklistDto to Checklist domain model
 */
fun AvailableChecklistDto.toDomain(): Checklist {
    return Checklist(
        id = this.id,
        name = this.name,
        fahrzeuggrupeId = this.fahrzeuggruppeId,
        erstellerId = null, // Not included in available response
        template = false, // Available checklists are non-template by definition
        createdAt = parseIsoDateTime(this.createdAt),
        items = emptyList() // Available response doesn't include items
    )
}

// Helper function to parse ISO datetime strings
private fun parseIsoDateTime(dateTimeString: String): Instant {
    return try {
        Instant.parse(dateTimeString)
    } catch (e: Exception) {
        kotlinx.datetime.Clock.System.now()
    }
}