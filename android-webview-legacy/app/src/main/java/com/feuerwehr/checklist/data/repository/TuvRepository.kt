package com.feuerwehr.checklist.data.repository

import com.feuerwehr.checklist.data.database.TuvTerminDao
import com.feuerwehr.checklist.data.database.VehicleDao
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
 * Repository for TÜV (Technical Inspection) operations
 * Handles inspection scheduling and deadline tracking
 */
@Singleton
class TuvRepository @Inject constructor(
    private val apiService: ChecklistApiService,
    private val tuvTerminDao: TuvTerminDao,
    private val vehicleDao: VehicleDao
) {

    /**
     * Get all TÜV terms with offline-first approach
     */
    fun getTuvTermine(forceRefresh: Boolean = false): Flow<Result<List<TuvTermin>>> = flow {
        try {
            // Emit cached data first
            val cachedTermine = tuvTerminDao.getAllTuvTermine().map { it.toDomainModel() }
            emit(Result.success(cachedTermine))

            // Fetch from network if needed
            if (forceRefresh || shouldRefreshData()) {
                try {
                    val response = apiService.getTuvTermine()
                    if (response.isSuccessful) {
                        val networkTermine = response.body()?.map { it.toDomainModel() } ?: emptyList()
                        
                        // Update local cache
                        val entities = networkTermine.map { it.toEntity() }
                        tuvTerminDao.insertTuvTermine(entities)
                        
                        emit(Result.success(networkTermine))
                    }
                } catch (e: Exception) {
                    // Network error - keep using cached data
                    if (cachedTermine.isEmpty()) {
                        emit(Result.failure(RepositoryException("Netzwerkfehler: ${e.message}")))
                    }
                }
            }
        } catch (e: Exception) {
            emit(Result.failure(RepositoryException("Fehler beim Laden der TÜV-Termine: ${e.message}")))
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
                
                // Cache alerts locally
                val entities = alerts.map { it.toEntity() }
                tuvTerminDao.insertTuvTermine(entities)
                
                Result.success(alerts)
            } else {
                // Fallback to local alerts
                val cachedAlerts = tuvTerminDao.getTuvAlerts().map { it.toDomainModel() }
                Result.success(cachedAlerts)
            }
        } catch (e: Exception) {
            // Use local alerts on network error
            val cachedAlerts = tuvTerminDao.getTuvAlerts().map { it.toDomainModel() }
            Result.success(cachedAlerts)
        }
    }

    /**
     * Get TÜV terms for specific vehicle
     */
    suspend fun getTuvTermineForVehicle(fahrzeugId: Int): Result<List<TuvTermin>> {
        return try {
            val response = apiService.getTuvTermine(fahrzeugId = fahrzeugId)
            if (response.isSuccessful) {
                val termine = response.body()?.map { it.toDomainModel() } ?: emptyList()
                
                // Update cache
                val entities = termine.map { it.toEntity() }
                tuvTerminDao.insertTuvTermine(entities)
                
                Result.success(termine)
            } else {
                // Use cached data
                val cached = tuvTerminDao.getTuvTermineByVehicle(fahrzeugId)
                    .map { it.toDomainModel() }
                Result.success(cached)
            }
        } catch (e: Exception) {
            // Fallback to cache
            val cached = tuvTerminDao.getTuvTermineByVehicle(fahrzeugId)
                .map { it.toDomainModel() }
            Result.success(cached)
        }
    }

    /**
     * Create new TÜV appointment
     */
    suspend fun createTuvTermin(
        fahrzeugId: Int,
        ablaufDatum: LocalDate,
        letztePruefung: LocalDate? = null
    ): Result<TuvTermin> {
        return try {
            val request = CreateTuvTerminRequest(
                fahrzeugId = fahrzeugId,
                ablaufDatum = ablaufDatum.toString(),
                letztePruefung = letztePruefung?.toString()
            )

            val response = apiService.createTuvTermin(request)
            if (response.isSuccessful) {
                val tuvTermin = response.body()!!.toDomainModel()
                
                // Cache locally
                tuvTerminDao.insertTuvTermin(tuvTermin.toEntity())
                
                Result.success(tuvTermin)
            } else {
                val errorMessage = when (response.code()) {
                    400 -> "Ungültiges Datum"
                    403 -> "Keine Berechtigung"
                    404 -> "Fahrzeug nicht gefunden"
                    409 -> "TÜV-Termin existiert bereits"
                    else -> "Fehler beim Erstellen: ${response.message()}"
                }
                Result.failure(RepositoryException(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(RepositoryException("TÜV-Termin konnte nicht erstellt werden: ${e.message}"))
        }
    }

    /**
     * Update existing TÜV appointment
     */
    suspend fun updateTuvTermin(
        id: Int,
        ablaufDatum: LocalDate? = null,
        letztePruefung: LocalDate? = null
    ): Result<TuvTermin> {
        return try {
            val request = UpdateTuvTerminRequest(
                ablaufDatum = ablaufDatum?.toString(),
                letztePruefung = letztePruefung?.toString()
            )

            val response = apiService.updateTuvTermin(id, request)
            if (response.isSuccessful) {
                val tuvTermin = response.body()!!.toDomainModel()
                
                // Update cache
                tuvTerminDao.insertTuvTermin(tuvTermin.toEntity())
                
                Result.success(tuvTermin)
            } else {
                val errorMessage = when (response.code()) {
                    400 -> "Ungültige Daten"
                    403 -> "Keine Berechtigung"
                    404 -> "TÜV-Termin nicht gefunden"
                    else -> "Fehler beim Aktualisieren: ${response.message()}"
                }
                Result.failure(RepositoryException(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(RepositoryException("TÜV-Termin konnte nicht aktualisiert werden: ${e.message}"))
        }
    }

    /**
     * Delete TÜV appointment
     */
    suspend fun deleteTuvTermin(id: Int): Result<Unit> {
        return try {
            val response = apiService.deleteTuvTermin(id)
            if (response.isSuccessful) {
                // Remove from cache
                val entity = tuvTerminDao.getTuvTerminById(id)
                if (entity != null) {
                    tuvTerminDao.deleteTuvTermin(entity)
                }
                
                Result.success(Unit)
            } else {
                val errorMessage = when (response.code()) {
                    403 -> "Keine Berechtigung zum Löschen"
                    404 -> "TÜV-Termin nicht gefunden"
                    else -> "Fehler beim Löschen: ${response.message()}"
                }
                Result.failure(RepositoryException(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(RepositoryException("TÜV-Termin konnte nicht gelöscht werden: ${e.message}"))
        }
    }

    /**
     * Get upcoming TÜV deadlines (next 60 days)
     */
    suspend fun getUpcomingDeadlines(): Result<List<TuvTermin>> {
        return try {
            val allTermine = tuvTerminDao.getAllTuvTermine().map { it.toDomainModel() }
            
            val upcomingTermine = allTermine.filter { termin ->
                val daysUntil = termin.daysUntilExpiration()
                daysUntil in 0..60 // Next 60 days
            }.sortedBy { it.ablaufDatum }
            
            Result.success(upcomingTermine)
        } catch (e: Exception) {
            Result.failure(RepositoryException("Fehler beim Laden der anstehenden Termine: ${e.message}"))
        }
    }

    /**
     * Get overdue TÜV inspections
     */
    suspend fun getOverdueInspections(): Result<List<TuvTermin>> {
        return try {
            val allTermine = tuvTerminDao.getAllTuvTermine().map { it.toDomainModel() }
            
            val overdueTermine = allTermine.filter { it.isExpired() }
                .sortedBy { it.ablaufDatum }
            
            Result.success(overdueTermine)
        } catch (e: Exception) {
            Result.failure(RepositoryException("Fehler beim Laden der überfälligen Termine: ${e.message}"))
        }
    }

    private fun shouldRefreshData(): Boolean {
        // Simple refresh strategy - can be enhanced with timestamp checking
        return true
    }
}

/**
 * Extension functions for TuvTermin data mapping
 */
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