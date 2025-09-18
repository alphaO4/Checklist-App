package com.feuerwehr.checklist.data.mapper

import com.feuerwehr.checklist.data.local.entity.*
import com.feuerwehr.checklist.data.remote.dto.*
import com.feuerwehr.checklist.domain.model.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Mappers for converting between Entity, DTO, and Domain models
 * Following the clean architecture pattern for data transformation
 */

// Helper functions for JSON parsing using Gson
private val gson = Gson()

private fun parseJsonToMap(jsonString: String): Map<String, Any>? {
    return try {
        val type = object : TypeToken<Map<String, Any>>() {}.type
        gson.fromJson(jsonString, type)
    } catch (e: Exception) {
        null
    }
}

private fun parseJsonToStringList(jsonString: String): List<String>? {
    return try {
        val type = object : TypeToken<List<String>>() {}.type
        gson.fromJson(jsonString, type)
    } catch (e: Exception) {
        null
    }
}

private fun mapToJsonString(map: Map<String, Any>?): String? {
    return try {
        map?.let { gson.toJson(it) }
    } catch (e: Exception) {
        null
    }
}

private fun stringListToJsonString(list: List<String>?): String? {
    return try {
        list?.let { gson.toJson(it) }
    } catch (e: Exception) {
        null
    }
}

// Helper function to parse date strings from backend
// Handles both ISO format and German format (dd.MM.yyyy HH:mm:ss)
private fun parseBackendDate(dateString: String): Instant {
    return try {
        // Try ISO format first (2025-08-29T17:37:21)
        Instant.parse(dateString)
    } catch (e: Exception) {
        try {
            // Try German format (29.08.2025 17:37:21)
            val parts = dateString.split(" ")
            if (parts.size == 2) {
                val datePart = parts[0].split(".")
                val timePart = parts[1].split(":")
                if (datePart.size == 3 && timePart.size == 3) {
                    val day = datePart[0].toInt()
                    val month = datePart[1].toInt()
                    val year = datePart[2].toInt()
                    val hour = timePart[0].toInt()
                    val minute = timePart[1].toInt()
                    val second = timePart[2].toInt()
                    
                    LocalDateTime(year, month, day, hour, minute, second)
                        .toInstant(TimeZone.UTC)
                } else {
                    throw IllegalArgumentException("Invalid German date format: $dateString")
                }
            } else {
                throw IllegalArgumentException("Invalid German date format: $dateString")
            }
        } catch (e2: Exception) {
            // Fallback to current time if parsing fails
            Clock.System.now()
        }
    }
}

// Entity to Domain mappers
fun ChecklistEntity.toDomain(): Checklist = Checklist(
    id = id,
    name = name,
    fahrzeuggrupeId = fahrzeuggrupeId,
    erstellerId = erstellerId,
    template = template,
    createdAt = createdAt
)

fun ChecklistItemEntity.toDomain(): ChecklistItem = ChecklistItem(
    id = id,
    checklisteId = checklisteId,
    beschreibung = beschreibung,
    itemType = ChecklistItemType.fromString(itemType),
    validationConfig = validationConfig?.let { parseJsonToMap(it) },
    editableRoles = editableRoles?.let { parseJsonToStringList(it) },
    requiresTuv = requiresTuv,
    subcategories = subcategories?.let { parseJsonToMap(it) },
    pflicht = pflicht,
    reihenfolge = reihenfolge,
    createdAt = createdAt
)

fun ChecklistExecutionEntity.toDomain(): ChecklistExecution = ChecklistExecution(
    id = id,
    checklisteId = checklisteId,
    fahrzeugId = fahrzeugId,
    benutzerId = benutzerId,
    status = ExecutionStatus.fromString(status),
    startedAt = startedAt,
    completedAt = completedAt
)

fun ItemResultEntity.toDomain(): ItemResult = ItemResult(
    id = id,
    ausfuehrungId = ausfuehrungId,
    itemId = itemId,
    status = ItemStatus.fromString(status),
    wert = wert?.let { parseJsonToMap(it) },
    vorhanden = vorhanden,
    tuvDatum = tuvDatum,
    tuvStatus = tuvStatus,
    menge = menge,
    kommentar = kommentar,
    createdAt = createdAt
)

// DTO to Entity mappers
fun ChecklistDto.toEntity(): ChecklistEntity = ChecklistEntity(
    id = id,
    name = name,
    fahrzeuggrupeId = fahrzeuggrupeId,
    erstellerId = erstellerId,
    template = template,
    createdAt = parseBackendDate(createdAt),
    syncStatus = SyncStatus.SYNCED,
    lastModified = Clock.System.now()
)

fun ChecklistWithItemsDto.toEntity(): ChecklistEntity = ChecklistEntity(
    id = id,
    name = name,
    fahrzeuggrupeId = fahrzeuggrupeId,
    erstellerId = erstellerId,
    template = template,
    createdAt = parseBackendDate(createdAt),
    syncStatus = SyncStatus.SYNCED,
    lastModified = Clock.System.now()
)

fun ChecklistItemDto.toEntity(): ChecklistItemEntity = ChecklistItemEntity(
    id = id,
    checklisteId = checklisteId,
    beschreibung = beschreibung,
    itemType = itemType,
    validationConfig = mapToJsonString(validationConfig),
    editableRoles = stringListToJsonString(editableRoles),
    requiresTuv = requiresTuv,
    subcategories = mapToJsonString(subcategories),
    pflicht = pflicht,
    reihenfolge = reihenfolge,
    createdAt = parseBackendDate(createdAt),
    syncStatus = SyncStatus.SYNCED,
    lastModified = Clock.System.now()
)

fun ChecklistExecutionDto.toEntity(): ChecklistExecutionEntity = ChecklistExecutionEntity(
    id = id,
    checklisteId = checklisteId,
    fahrzeugId = fahrzeugId,
    benutzerId = benutzerId,
    status = status,
    startedAt = startedAt?.let { parseBackendDate(it) } ?: Clock.System.now(),
    completedAt = completedAt?.let { parseBackendDate(it) },
    syncStatus = SyncStatus.SYNCED,
    lastModified = Clock.System.now()
)

fun ItemResultDto.toEntity(): ItemResultEntity = ItemResultEntity(
    id = id,
    ausfuehrungId = ausfuehrungId,
    itemId = itemId,
    status = status,
    wert = mapToJsonString(wert),
    vorhanden = vorhanden,
    tuvDatum = tuvDatum?.let { LocalDate.parse(it) },
    tuvStatus = tuvStatus,
    menge = menge,
    kommentar = kommentar,
    createdAt = parseBackendDate(createdAt),
    syncStatus = SyncStatus.SYNCED,
    lastModified = Clock.System.now()
)

// DTO to Domain mappers (direct conversion)
fun ChecklistDto.toDomain(): Checklist = Checklist(
    id = id,
    name = name,
    fahrzeuggrupeId = fahrzeuggrupeId,
    erstellerId = erstellerId,
    template = template,
    createdAt = parseBackendDate(createdAt)
)

fun ChecklistWithItemsDto.toDomain(): Checklist = Checklist(
    id = id,
    name = name,
    fahrzeuggrupeId = fahrzeuggrupeId,
    erstellerId = erstellerId,
    template = template,
    createdAt = parseBackendDate(createdAt),
    items = items.map { it.toDomain() }
)

fun ChecklistItemDto.toDomain(): ChecklistItem = ChecklistItem(
    id = id,
    checklisteId = checklisteId,
    beschreibung = beschreibung,
    itemType = ChecklistItemType.fromString(itemType),
    validationConfig = validationConfig,
    editableRoles = editableRoles,
    requiresTuv = requiresTuv,
    subcategories = subcategories,
    pflicht = pflicht,
    reihenfolge = reihenfolge,
    createdAt = parseBackendDate(createdAt)
)

fun ChecklistListDto.toDomain(): ChecklistPage = ChecklistPage(
    items = items.map { it.toDomain() },
    total = total,
    page = page,
    perPage = perPage,
    totalPages = totalPages
)

// Domain to Entity mappers (for offline storage)
fun Checklist.toEntity(): ChecklistEntity = ChecklistEntity(
    id = id,
    name = name,
    fahrzeuggrupeId = fahrzeuggrupeId,
    erstellerId = erstellerId,
    template = template,
    createdAt = createdAt,
    syncStatus = SyncStatus.SYNCED,
    lastModified = Clock.System.now()
)

fun ChecklistItem.toEntity(): ChecklistItemEntity = ChecklistItemEntity(
    id = id,
    checklisteId = checklisteId,
    beschreibung = beschreibung,
    itemType = itemType.value,
    validationConfig = mapToJsonString(validationConfig),
    editableRoles = stringListToJsonString(editableRoles),
    requiresTuv = requiresTuv,
    subcategories = mapToJsonString(subcategories),
    pflicht = pflicht,
    reihenfolge = reihenfolge,
    createdAt = createdAt,
    syncStatus = SyncStatus.SYNCED,
    lastModified = Clock.System.now()
)

// Pagination domain model
data class ChecklistPage(
    val items: List<Checklist>,
    val total: Int,
    val page: Int,
    val perPage: Int,
    val totalPages: Int
)

/**
 * Convert VehicleGroupDto to VehicleGroupEntity
 */
fun VehicleGroupDto.toEntity(): VehicleGroupEntity {
    return VehicleGroupEntity(
        id = id,
        name = name,
        createdAt = parseBackendDate(createdAt),
        syncStatus = SyncStatus.SYNCED,
        lastModified = Clock.System.now()
    )
}

/**
 * Convert ItemResult domain to ItemResultCreateDto for API calls
 */
fun ItemResult.toCreateDto(): ItemResultCreateDto = ItemResultCreateDto(
    itemId = itemId,
    status = status.value,
    wert = wert,
    vorhanden = vorhanden,
    tuvDatum = tuvDatum?.toString(),
    tuvStatus = tuvStatus,
    menge = menge,
    kommentar = kommentar
)