package com.feuerwehr.checklist.data.repository

import com.feuerwehr.checklist.data.database.*
import com.feuerwehr.checklist.data.models.*
import com.feuerwehr.checklist.data.network.ChecklistApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for vehicle-related operations
 * Implements offline-first architecture with network synchronization
 */
@Singleton
class VehicleRepository @Inject constructor(
    private val apiService: ChecklistApiService,
    private val vehicleDao: VehicleDao,
    private val vehicleTypeDao: VehicleTypeDao,
    private val vehicleGroupDao: VehicleGroupDao
) {

    /**
     * Get all vehicles with offline-first approach
     */
    fun getVehicles(forceRefresh: Boolean = false): Flow<Result<List<Vehicle>>> = flow {
        try {
            // Always emit cached data first
            val cachedVehicles = vehicleDao.getAllVehicles().map { it.toDomainModel() }
            emit(Result.success(cachedVehicles))

            // Fetch from network if needed
            if (forceRefresh || shouldRefreshData()) {
                try {
                    val response = apiService.getVehicles()
                    if (response.isSuccessful) {
                        val networkVehicles = response.body()?.map { it.toDomainModel() } ?: emptyList()
                        
                        // Update local cache
                        val entities = networkVehicles.map { it.toEntity() }
                        vehicleDao.insertVehicles(entities)
                        
                        // Emit updated data
                        emit(Result.success(networkVehicles))
                    }
                } catch (e: Exception) {
                    // Network error - keep using cached data
                    // Don't emit error if we have cached data
                    if (cachedVehicles.isEmpty()) {
                        emit(Result.failure(RepositoryException("Netzwerkfehler: ${e.message}")))
                    }
                }
            }
        } catch (e: Exception) {
            emit(Result.failure(RepositoryException("Fehler beim Laden der Fahrzeuge: ${e.message}")))
        }
    }

    /**
     * Get vehicle by ID
     */
    suspend fun getVehicleById(id: Int): Result<Vehicle?> {
        return try {
            // Try cache first
            val cachedVehicle = vehicleDao.getVehicleById(id)?.toDomainModel()
            
            // Try network update
            try {
                val response = apiService.getVehicle(id)
                if (response.isSuccessful) {
                    val networkVehicle = response.body()?.toDomainModel()
                    if (networkVehicle != null) {
                        vehicleDao.insertVehicle(networkVehicle.toEntity())
                        return Result.success(networkVehicle)
                    }
                }
            } catch (e: Exception) {
                // Network error - use cache
            }
            
            Result.success(cachedVehicle)
        } catch (e: Exception) {
            Result.failure(RepositoryException("Fehler beim Laden des Fahrzeugs: ${e.message}"))
        }
    }

    /**
     * Create new vehicle
     */
    suspend fun createVehicle(
        kennzeichen: String,
        fahrzeugtypId: Int,
        fahrzeuggruppeId: Int
    ): Result<Vehicle> {
        return try {
            val request = CreateVehicleRequest(
                kennzeichen = kennzeichen,
                fahrzeugtypId = fahrzeugtypId,
                fahrzeuggruppeId = fahrzeuggruppeId
            )

            val response = apiService.createVehicle(request)
            if (response.isSuccessful) {
                val vehicle = response.body()!!.toDomainModel()
                
                // Update local cache
                vehicleDao.insertVehicle(vehicle.toEntity())
                
                Result.success(vehicle)
            } else {
                val errorMessage = when (response.code()) {
                    400 -> "Ungültige Fahrzeugdaten"
                    403 -> "Keine Berechtigung zum Erstellen von Fahrzeugen"
                    409 -> "Fahrzeug mit diesem Kennzeichen existiert bereits"
                    else -> "Fehler beim Erstellen: ${response.message()}"
                }
                Result.failure(RepositoryException(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(RepositoryException("Netzwerkfehler: ${e.message}"))
        }
    }

    /**
     * Update existing vehicle
     */
    suspend fun updateVehicle(
        id: Int,
        kennzeichen: String? = null,
        fahrzeugtypId: Int? = null,
        fahrzeuggruppeId: Int? = null
    ): Result<Vehicle> {
        return try {
            val request = UpdateVehicleRequest(
                kennzeichen = kennzeichen,
                fahrzeugtypId = fahrzeugtypId,
                fahrzeuggruppeId = fahrzeuggruppeId
            )

            val response = apiService.updateVehicle(id, request)
            if (response.isSuccessful) {
                val vehicle = response.body()!!.toDomainModel()
                
                // Update local cache
                vehicleDao.insertVehicle(vehicle.toEntity())
                
                Result.success(vehicle)
            } else {
                val errorMessage = when (response.code()) {
                    400 -> "Ungültige Fahrzeugdaten"
                    403 -> "Keine Berechtigung zum Bearbeiten von Fahrzeugen"
                    404 -> "Fahrzeug nicht gefunden"
                    409 -> "Kennzeichen bereits vergeben"
                    else -> "Fehler beim Aktualisieren: ${response.message()}"
                }
                Result.failure(RepositoryException(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(RepositoryException("Netzwerkfehler: ${e.message}"))
        }
    }

    /**
     * Delete vehicle
     */
    suspend fun deleteVehicle(id: Int): Result<Unit> {
        return try {
            val response = apiService.deleteVehicle(id)
            if (response.isSuccessful) {
                // Remove from local cache
                val entity = vehicleDao.getVehicleById(id)
                if (entity != null) {
                    vehicleDao.deleteVehicle(entity)
                }
                
                Result.success(Unit)
            } else {
                val errorMessage = when (response.code()) {
                    403 -> "Keine Berechtigung zum Löschen von Fahrzeugen"
                    404 -> "Fahrzeug nicht gefunden"
                    409 -> "Fahrzeug kann nicht gelöscht werden (aktive Checklisten)"
                    else -> "Fehler beim Löschen: ${response.message()}"
                }
                Result.failure(RepositoryException(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(RepositoryException("Netzwerkfehler: ${e.message}"))
        }
    }

    /**
     * Search vehicles by license plate
     */
    suspend fun searchVehicles(kennzeichen: String): Result<List<Vehicle>> {
        return try {
            // Search in local cache
            val cachedResults = vehicleDao.searchVehiclesByKennzeichen(kennzeichen)
                .map { it.toDomainModel() }

            // Try network search as well
            try {
                val response = apiService.getVehicles(kennzeichen = kennzeichen)
                if (response.isSuccessful) {
                    val networkResults = response.body()?.map { it.toDomainModel() } ?: emptyList()
                    
                    // Update cache with network results
                    val entities = networkResults.map { it.toEntity() }
                    vehicleDao.insertVehicles(entities)
                    
                    return Result.success(networkResults)
                }
            } catch (e: Exception) {
                // Network error - use cached results
            }

            Result.success(cachedResults)
        } catch (e: Exception) {
            Result.failure(RepositoryException("Fehler bei der Suche: ${e.message}"))
        }
    }

    /**
     * Get vehicles by type
     */
    suspend fun getVehiclesByType(fahrzeugtypId: Int): Result<List<Vehicle>> {
        return try {
            val cachedVehicles = vehicleDao.getVehiclesByType(fahrzeugtypId)
                .map { it.toDomainModel() }

            // Try network fetch
            try {
                val response = apiService.getVehicles(fahrzeugtypId = fahrzeugtypId)
                if (response.isSuccessful) {
                    val networkVehicles = response.body()?.map { it.toDomainModel() } ?: emptyList()
                    
                    // Update cache
                    val entities = networkVehicles.map { it.toEntity() }
                    vehicleDao.insertVehicles(entities)
                    
                    return Result.success(networkVehicles)
                }
            } catch (e: Exception) {
                // Use cached data on network error
            }

            Result.success(cachedVehicles)
        } catch (e: Exception) {
            Result.failure(RepositoryException("Fehler beim Laden: ${e.message}"))
        }
    }

    /**
     * Check if data should be refreshed (simple 5-minute cache strategy)
     */
    private fun shouldRefreshData(): Boolean {
        // For now, always try to refresh - can be enhanced with timestamp checking
        return true
    }
}

/**
 * Extension functions for data mapping
 */
private fun VehicleDto.toDomainModel(): Vehicle {
    return Vehicle(
        id = id,
        kennzeichen = kennzeichen,
        fahrzeugtypId = fahrzeugtypId,
        fahrzeuggruppeId = fahrzeuggruppeId,
        createdAt = Instant.parse(createdAt),
        fahrzeugtyp = fahrzeugtyp?.toDomainModel(),
        fahrzeuggruppe = fahrzeuggruppe?.toDomainModel(),
        tuvTermine = tuvTermine?.map { it.toDomainModel() }
    )
}

private fun VehicleTypeDto.toDomainModel(): VehicleType {
    return VehicleType(
        id = id,
        name = name,
        beschreibung = beschreibung,
        createdAt = Instant.parse(createdAt)
    )
}

private fun VehicleGroupDto.toDomainModel(): VehicleGroup {
    return VehicleGroup(
        id = id,
        name = name,
        gruppeId = gruppeId,
        createdAt = Instant.parse(createdAt)
    )
}

private fun Vehicle.toEntity(): VehicleEntity {
    return VehicleEntity(
        id = id,
        kennzeichen = kennzeichen,
        fahrzeugtypId = fahrzeugtypId,
        fahrzeuggruppeId = fahrzeuggruppeId,
        createdAt = createdAt.toString(),
        lastSyncedAt = Clock.System.now().toEpochMilliseconds(),
        isLocalOnly = isLocalOnly
    )
}

private fun VehicleEntity.toDomainModel(): Vehicle {
    return Vehicle(
        id = id,
        kennzeichen = kennzeichen,
        fahrzeugtypId = fahrzeugtypId,
        fahrzeuggruppeId = fahrzeuggruppeId,
        createdAt = Instant.parse(createdAt),
        isLocalOnly = isLocalOnly
    )
}

/**
 * Custom exception for repository errors
 */
class RepositoryException(message: String) : Exception(message)