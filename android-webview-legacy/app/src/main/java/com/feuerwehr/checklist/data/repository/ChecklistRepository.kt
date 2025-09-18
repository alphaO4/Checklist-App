package com.feuerwehr.checklist.data.repository

import com.feuerwehr.checklist.data.database.*
import com.feuerwehr.checklist.data.models.*
import com.feuerwehr.checklist.data.network.ChecklistApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for checklist and TÜV-related operations
 * Handles CSV-imported templates and execution tracking
 */
@Singleton
class ChecklistRepository @Inject constructor(
    private val apiService: ChecklistApiService,
    private val checklistDao: ChecklistDao,
    private val checklistItemDao: ChecklistItemDao,
    private val checklistAusfuehrungDao: ChecklistAusfuehrungDao,
    private val itemErgebnisDao: ItemErgebnisDao,
    private val tuvTerminDao: TuvTerminDao
) {

    /**
     * Import CSV checklist templates from backend
     */
    suspend fun importCsvTemplates(): Result<String> {
        return try {
            val response = apiService.importCsvTemplates()
            if (response.isSuccessful) {
                val result = response.body()!!
                
                // Refresh local templates after import
                refreshTemplates()
                
                Result.success(result["message"] as String)
            } else {
                Result.failure(RepositoryException("Fehler beim Import: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(RepositoryException("Import fehlgeschlagen: ${e.message}"))
        }
    }

    /**
     * Get CSV summary from backend
     */
    suspend fun getCsvSummary(): Result<Map<String, Any>> {
        return try {
            val response = apiService.getCsvSummary()
            if (response.isSuccessful) {
                val summary = response.body()!!
                Result.success(summary)
            } else {
                Result.failure(RepositoryException("Fehler beim Laden der CSV-Übersicht"))
            }
        } catch (e: Exception) {
            Result.failure(RepositoryException("CSV-Übersicht nicht verfügbar: ${e.message}"))
        }
    }

    /**
     * Get checklist templates (offline-first)
     */
    fun getTemplates(forceRefresh: Boolean = false): Flow<Result<List<Checklist>>> = flow {
        try {
            // Emit cached templates first
            val cachedTemplates = checklistDao.getChecklistsByTemplate(true)
                .map { it.toDomainModel() }
            emit(Result.success(cachedTemplates))

            // Refresh from network if needed
            if (forceRefresh || cachedTemplates.isEmpty()) {
                refreshTemplates()
                
                // Emit updated templates
                val updatedTemplates = checklistDao.getChecklistsByTemplate(true)
                    .map { it.toDomainModel() }
                emit(Result.success(updatedTemplates))
            }
        } catch (e: Exception) {
            emit(Result.failure(RepositoryException("Fehler beim Laden der Templates: ${e.message}")))
        }
    }

    /**
     * Get checklist with items
     */
    suspend fun getChecklistWithItems(checklistId: Int): Result<Checklist> {
        return try {
            val response = apiService.getChecklist(checklistId)
            if (response.isSuccessful) {
                val checklistDto = response.body()!!
                val checklist = checklistDto.toDomainModel()
                
                // Cache locally
                checklistDao.insertChecklist(checklist.toEntity())
                
                Result.success(checklist)
            } else {
                // Try local cache
                val cached = checklistDao.getChecklistById(checklistId)?.toDomainModel()
                if (cached != null) {
                    Result.success(cached)
                } else {
                    Result.failure(RepositoryException("Checkliste nicht gefunden"))
                }
            }
        } catch (e: Exception) {
            // Fallback to cache
            val cached = checklistDao.getChecklistById(checklistId)?.toDomainModel()
            if (cached != null) {
                Result.success(cached)
            } else {
                Result.failure(RepositoryException("Fehler beim Laden: ${e.message}"))
            }
        }
    }

    /**
     * Start checklist execution for a vehicle
     */
    suspend fun startChecklistExecution(
        checklistId: Int,
        fahrzeugId: Int
    ): Result<ChecklistAusfuehrung> {
        return try {
            val request = StartChecklistRequest(checklistId, fahrzeugId)
            val response = apiService.startChecklistExecution(request)
            
            if (response.isSuccessful) {
                val execution = response.body()!!.toDomainModel()
                
                // Cache locally
                checklistAusfuehrungDao.insertExecution(execution.toEntity())
                
                Result.success(execution)
            } else {
                Result.failure(RepositoryException("Fehler beim Starten: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(RepositoryException("Checkliste konnte nicht gestartet werden: ${e.message}"))
        }
    }

    /**
     * Update item result in checklist execution
     */
    suspend fun updateItemResult(
        executionId: Int,
        itemId: Int,
        status: ItemStatus,
        kommentar: String? = null
    ): Result<ItemErgebnis> {
        return try {
            val request = UpdateItemErgebnisRequest(status.value, kommentar)
            val response = apiService.updateItemResult(executionId, itemId, request)
            
            if (response.isSuccessful) {
                val result = response.body()!!.toDomainModel()
                
                // Cache locally
                itemErgebnisDao.insertResult(result.toEntity())
                
                Result.success(result)
            } else {
                Result.failure(RepositoryException("Fehler beim Speichern: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(RepositoryException("Ergebnis konnte nicht gespeichert werden: ${e.message}"))
        }
    }

    /**
     * Complete checklist execution
     */
    suspend fun completeChecklistExecution(executionId: Int): Result<ChecklistAusfuehrung> {
        return try {
            val response = apiService.completeChecklistExecution(executionId)
            
            if (response.isSuccessful) {
                val execution = response.body()!!.toDomainModel()
                
                // Update local cache
                checklistAusfuehrungDao.insertExecution(execution.toEntity())
                
                Result.success(execution)
            } else {
                Result.failure(RepositoryException("Fehler beim Abschließen: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(RepositoryException("Checkliste konnte nicht abgeschlossen werden: ${e.message}"))
        }
    }

    /**
     * Get vehicle executions
     */
    suspend fun getVehicleExecutions(fahrzeugId: Int): Result<List<ChecklistAusfuehrung>> {
        return try {
            val response = apiService.getChecklistExecutions(fahrzeugId = fahrzeugId)
            if (response.isSuccessful) {
                val executions = response.body()?.map { it.toDomainModel() } ?: emptyList()
                
                // Cache locally
                val entities = executions.map { it.toEntity() }
                checklistAusfuehrungDao.insertExecutions(entities)
                
                Result.success(executions)
            } else {
                // Fallback to local cache
                val cached = checklistAusfuehrungDao.getExecutionsByVehicle(fahrzeugId)
                    .map { it.toDomainModel() }
                Result.success(cached)
            }
        } catch (e: Exception) {
            // Use cached data on network error
            val cached = checklistAusfuehrungDao.getExecutionsByVehicle(fahrzeugId)
                .map { it.toDomainModel() }
            Result.success(cached)
        }
    }

    /**
     * Get TÜV alerts (expiring or expired inspections)
     */
    suspend fun getTuvAlerts(): Result<List<TuvTermin>> {
        return try {
            val response = apiService.getTuvAlerts()
            if (response.isSuccessful) {
                val alerts = response.body()?.map { it.toDomainModel() } ?: emptyList()
                
                // Cache locally
                val entities = alerts.map { it.toEntity() }
                tuvTerminDao.insertTuvTermine(entities)
                
                Result.success(alerts)
            } else {
                // Use local alerts
                val cachedAlerts = tuvTerminDao.getTuvAlerts()
                    .map { it.toDomainModel() }
                Result.success(cachedAlerts)
            }
        } catch (e: Exception) {
            // Fallback to cached alerts
            val cachedAlerts = tuvTerminDao.getTuvAlerts()
                .map { it.toDomainModel() }
            Result.success(cachedAlerts)
        }
    }

    /**
     * Private helper to refresh templates from API
     */
    private suspend fun refreshTemplates() {
        try {
            val response = apiService.getChecklists(template = true)
            if (response.isSuccessful) {
                val templates = response.body() ?: emptyList()
                
                // Clear old templates and insert new ones
                checklistDao.clearChecklists()
                
                val entities = templates.map { it.toDomainModel().toEntity() }
                checklistDao.insertChecklists(entities)
                
                // Also refresh checklist items for each template
                templates.forEach { template ->
                    template.items?.forEach { item ->
                        checklistItemDao.insertItem(item.toDomainModel().toEntity())
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore refresh errors - use cached data
        }
    }
}

/**
 * Extension functions for checklist data mapping
 */
private fun ChecklistDto.toDomainModel(): Checklist {
    return Checklist(
        id = id,
        name = name,
        fahrzeuggruppeId = fahrzeuggruppeId,
        erstellerId = erstellerId,
        template = template,
        createdAt = Instant.parse(createdAt),
        items = items?.map { it.toDomainModel() } ?: emptyList(),
        fahrzeuggruppe = fahrzeuggruppe?.let {
            VehicleGroup(
                id = it.id,
                name = it.name,
                gruppeId = it.gruppeId,
                createdAt = Instant.parse(it.createdAt)
            )
        }
    )
}

private fun ChecklistItemDto.toDomainModel(): ChecklistItem {
    return ChecklistItem(
        id = id,
        checklisteId = checklisteId,
        beschreibung = beschreibung,
        pflicht = pflicht,
        reihenfolge = reihenfolge,
        createdAt = Instant.parse(createdAt)
    )
}

private fun ChecklistAusfuehrungDto.toDomainModel(): ChecklistAusfuehrung {
    return ChecklistAusfuehrung(
        id = id,
        checklisteId = checklisteId,
        fahrzeugId = fahrzeugId,
        benutzerId = benutzerId,
        status = ChecklistStatus.fromString(status),
        startedAt = Instant.parse(startedAt),
        completedAt = completedAt?.let { Instant.parse(it) },
        checkliste = checkliste?.toDomainModel(),
        fahrzeug = fahrzeug?.let { vehicleDto ->
            Vehicle(
                id = vehicleDto.id,
                kennzeichen = vehicleDto.kennzeichen,
                fahrzeugtypId = vehicleDto.fahrzeugtypId,
                fahrzeuggruppeId = vehicleDto.fahrzeuggruppeId,
                createdAt = Instant.parse(vehicleDto.createdAt)
            )
        },
        ergebnisse = ergebnisse?.map { it.toDomainModel() } ?: emptyList()
    )
}

private fun ItemErgebnisDto.toDomainModel(): ItemErgebnis {
    return ItemErgebnis(
        id = id,
        ausfuehrungId = ausfuehrungId,
        itemId = itemId,
        status = ItemStatus.fromString(status),
        kommentar = kommentar,
        createdAt = Instant.parse(createdAt),
        item = item?.toDomainModel()
    )
}

private fun TuvTerminDto.toDomainModel(): TuvTermin {
    return TuvTermin(
        id = id,
        fahrzeugId = fahrzeugId,
        ablaufDatum = LocalDate.parse(ablaufDatum),
        status = TuvStatus.fromString(status),
        letztePruefung = letztePruefung?.let { LocalDate.parse(it) },
        createdAt = Instant.parse(createdAt),
        fahrzeug = fahrzeug?.let { vehicleDto ->
            Vehicle(
                id = vehicleDto.id,
                kennzeichen = vehicleDto.kennzeichen,
                fahrzeugtypId = vehicleDto.fahrzeugtypId,
                fahrzeuggruppeId = vehicleDto.fahrzeuggruppeId,
                createdAt = Instant.parse(vehicleDto.createdAt)
            )
        }
    )
}

// Entity conversion extensions
private fun Checklist.toEntity(): ChecklistEntity {
    return ChecklistEntity(
        id = id,
        name = name,
        fahrzeuggruppeId = fahrzeuggruppeId,
        erstellerId = erstellerId,
        template = template,
        createdAt = createdAt.toString(),
        lastSyncedAt = Clock.System.now().toEpochMilliseconds(),
        isLocalOnly = isLocalOnly
    )
}

private fun ChecklistItem.toEntity(): ChecklistItemEntity {
    return ChecklistItemEntity(
        id = id,
        checklisteId = checklisteId,
        beschreibung = beschreibung,
        pflicht = pflicht,
        reihenfolge = reihenfolge,
        createdAt = createdAt.toString(),
        lastSyncedAt = Clock.System.now().toEpochMilliseconds(),
        isLocalOnly = isLocalOnly
    )
}

private fun ChecklistAusfuehrung.toEntity(): ChecklistAusfuehrungEntity {
    return ChecklistAusfuehrungEntity(
        id = id,
        checklisteId = checklisteId,
        fahrzeugId = fahrzeugId,
        benutzerId = benutzerId,
        status = status.value,
        startedAt = startedAt.toString(),
        completedAt = completedAt?.toString(),
        lastSyncedAt = Clock.System.now().toEpochMilliseconds(),
        isLocalOnly = isLocalOnly
    )
}

private fun ItemErgebnis.toEntity(): ItemErgebnisEntity {
    return ItemErgebnisEntity(
        id = id,
        ausfuehrungId = ausfuehrungId,
        itemId = itemId,
        status = status.value,
        kommentar = kommentar,
        createdAt = createdAt.toString(),
        lastSyncedAt = Clock.System.now().toEpochMilliseconds(),
        isLocalOnly = isLocalOnly
    )
}

private fun TuvTermin.toEntity(): TuvTerminEntity {
    return TuvTerminEntity(
        id = id,
        fahrzeugId = fahrzeugId,
        ablaufDatum = ablaufDatum.toString(),
        status = status.value,
        letztePruefung = letztePruefung?.toString(),
        createdAt = createdAt.toString(),
        lastSyncedAt = Clock.System.now().toEpochMilliseconds(),
        isLocalOnly = isLocalOnly
    )
}

private fun ChecklistEntity.toDomainModel(): Checklist {
    return Checklist(
        id = id,
        name = name,
        fahrzeuggruppeId = fahrzeuggruppeId,
        erstellerId = erstellerId,
        template = template,
        createdAt = Instant.parse(createdAt),
        isLocalOnly = isLocalOnly
    )
}

private fun ChecklistAusfuehrungEntity.toDomainModel(): ChecklistAusfuehrung {
    return ChecklistAusfuehrung(
        id = id,
        checklisteId = checklisteId,
        fahrzeugId = fahrzeugId,
        benutzerId = benutzerId,
        status = ChecklistStatus.fromString(status),
        startedAt = Instant.parse(startedAt),
        completedAt = completedAt?.let { Instant.parse(it) },
        isLocalOnly = isLocalOnly
    )
}

private fun TuvTerminEntity.toDomainModel(): TuvTermin {
    return TuvTermin(
        id = id,
        fahrzeugId = fahrzeugId,
        ablaufDatum = LocalDate.parse(ablaufDatum),
        status = TuvStatus.fromString(status),
        letztePruefung = letztePruefung?.let { LocalDate.parse(it) },
        createdAt = Instant.parse(createdAt),
        isLocalOnly = isLocalOnly
    )
}