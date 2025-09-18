package com.feuerwehr.checklist.data.repository

import com.feuerwehr.checklist.data.local.dao.VehicleDao
import com.feuerwehr.checklist.data.remote.api.VehicleApiService
import com.feuerwehr.checklist.data.mapper.*
import com.feuerwehr.checklist.domain.model.Vehicle
import com.feuerwehr.checklist.domain.model.VehicleGroup
import com.feuerwehr.checklist.domain.model.VehicleType
import com.feuerwehr.checklist.domain.model.Checklist
import com.feuerwehr.checklist.domain.model.ChecklistExecution
import com.feuerwehr.checklist.domain.model.VehicleChecklistStatus
import com.feuerwehr.checklist.domain.model.ExecutionStatus
import com.feuerwehr.checklist.domain.repository.VehicleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository implementation for Vehicle operations
 * Provides offline-first access with remote sync
 */
@Singleton
class VehicleRepositoryImpl @Inject constructor(
    private val vehicleDao: VehicleDao,
    private val vehicleApi: VehicleApiService
) : VehicleRepository {

    override fun getVehicles(): Flow<List<Vehicle>> {
        return combine(
            vehicleDao.getAllVehiclesFlow(),
            vehicleDao.getAllVehicleTypesFlow(),
            vehicleDao.getAllVehicleGroupsFlow()
        ) { vehicles, vehicleTypes, vehicleGroups ->
            vehicles.map { vehicleEntity ->
                val vehicleType = vehicleTypes.find { it.id == vehicleEntity.fahrzeugtypId }?.toDomain()
                val vehicleGroup = vehicleGroups.find { it.id == vehicleEntity.fahrzeuggruppeId }?.toDomain()
                vehicleEntity.toDomain(vehicleType, vehicleGroup)
            }
        }
    }

    override suspend fun getVehicleById(id: Int): Vehicle? {
        val vehicleEntity = vehicleDao.getVehicleById(id) ?: return null
        val vehicleType = vehicleDao.getVehicleTypeById(vehicleEntity.fahrzeugtypId)?.toDomain()
        val vehicleGroup = vehicleDao.getVehicleGroupById(vehicleEntity.fahrzeuggruppeId)?.toDomain()
        return vehicleEntity.toDomain(vehicleType, vehicleGroup)
    }

    override fun getVehiclesByGroup(vehicleGroupId: Int): Flow<List<Vehicle>> {
        return combine(
            vehicleDao.getVehiclesByGroupFlow(vehicleGroupId),
            vehicleDao.getAllVehicleTypesFlow(),
            vehicleDao.getAllVehicleGroupsFlow()
        ) { vehicles, vehicleTypes, vehicleGroups ->
            vehicles.map { vehicleEntity ->
                val vehicleType = vehicleTypes.find { it.id == vehicleEntity.fahrzeugtypId }?.toDomain()
                val vehicleGroup = vehicleGroups.find { it.id == vehicleEntity.fahrzeuggruppeId }?.toDomain()
                vehicleEntity.toDomain(vehicleType, vehicleGroup)
            }
        }
    }

    override suspend fun fetchVehiclesFromRemote(): Result<List<Vehicle>> {
        return try {
            // Fetch vehicle types first
            val vehicleTypes = vehicleApi.getVehicleTypes()
            vehicleDao.insertVehicleTypes(vehicleTypes.map { it.toEntity() })

            // Fetch vehicle groups
            val vehicleGroups = vehicleApi.getVehicleGroups()
            vehicleDao.insertVehicleGroups(vehicleGroups.map { it.toEntity() })

            // Fetch vehicles
            val vehiclesResponse = vehicleApi.getVehicles(page = 1, perPage = 100)
            val vehicleEntities = vehiclesResponse.items.map { it.toEntity() }
            vehicleDao.insertVehicles(vehicleEntities)

            // Convert to domain models
            val vehicles = vehicleEntities.map { vehicleEntity ->
                val vehicleType = vehicleTypes.find { it.id == vehicleEntity.fahrzeugtypId }?.let { dto ->
                    VehicleType(
                        id = dto.id,
                        name = dto.name,
                        beschreibung = dto.beschreibung,
                        aktiv = dto.aktiv,
                        createdAt = parseIsoDateTime(dto.createdAt)
                    )
                }
                val vehicleGroup = vehicleGroups.find { it.id == vehicleEntity.fahrzeuggruppeId }?.let { dto ->
                    VehicleGroup(
                        id = dto.id,
                        name = dto.name,
                        createdAt = parseIsoDateTime(dto.createdAt)
                    )
                }
                vehicleEntity.toDomain(vehicleType, vehicleGroup)
            }

            Result.success(vehicles)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncVehicles(): Result<Unit> {
        return try {
            fetchVehiclesFromRemote()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun hasActiveExecution(vehicleId: Int, checklistId: Int): Boolean {
        return false // Simplified implementation
    }

    override fun getVehicleGroups(): Flow<List<VehicleGroup>> {
        return vehicleDao.getAllVehicleGroupsFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun searchVehicles(query: String): Flow<List<Vehicle>> {
        return combine(
            vehicleDao.searchVehiclesFlow("%$query%"),
            vehicleDao.getAllVehicleTypesFlow(),
            vehicleDao.getAllVehicleGroupsFlow()
        ) { vehicles, vehicleTypes, vehicleGroups ->
            vehicles.map { vehicleEntity ->
                val vehicleType = vehicleTypes.find { it.id == vehicleEntity.fahrzeugtypId }?.toDomain()
                val vehicleGroup = vehicleGroups.find { it.id == vehicleEntity.fahrzeuggruppeId }?.toDomain()
                vehicleEntity.toDomain(vehicleType, vehicleGroup)
            }
        }
    }
    
    // Vehicle-Checklist relationship implementations
    
    override fun getChecklistsForVehicle(vehicleId: Int): Flow<List<Checklist>> {
        return vehicleDao.getChecklistsForVehicleFlow(vehicleId).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override fun getAvailableChecklistsForVehicle(vehicleId: Int): Flow<List<VehicleChecklistStatus>> {
        return vehicleDao.getAvailableChecklistsForVehicleFlow(vehicleId).map { checklistEntities ->
            checklistEntities.map { checklistEntity ->
                VehicleChecklistStatus(
                    checklist = checklistEntity.toDomain(),
                    hasActiveExecution = runBlocking { vehicleDao.hasActiveExecution(vehicleId, checklistEntity.id) },
                    activeExecutionId = runBlocking { vehicleDao.getActiveExecutionId(vehicleId, checklistEntity.id) }
                )
            }
        }
    }
    
    override suspend fun startChecklistForVehicle(vehicleId: Int, checklistId: Int): Result<ChecklistExecution> {
        return try {
            // Simplified implementation for now
            val execution = ChecklistExecution(
                id = 1,
                checklisteId = checklistId,
                fahrzeugId = vehicleId,
                benutzerId = 1,
                status = ExecutionStatus.IN_PROGRESS,
                startedAt = kotlinx.datetime.Clock.System.now(),
                completedAt = null
            )
            Result.success(execution)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun getVehiclesForChecklist(checklistId: Int): Flow<List<VehicleChecklistStatus>> {
        return combine(
            vehicleDao.getVehiclesForChecklistFlow(checklistId),
            vehicleDao.getAllVehicleTypesFlow()
        ) { vehicleEntities, vehicleTypes ->
            vehicleEntities.map { vehicleEntity ->
                val vehicleType = vehicleTypes.find { it.id == vehicleEntity.fahrzeugtypId }?.toDomain()
                VehicleChecklistStatus(
                    vehicle = vehicleEntity.toDomain(vehicleType, null),
                    checklist = Checklist(0, "", 0, null, false, kotlinx.datetime.Clock.System.now()), // Placeholder
                    hasActiveExecution = runBlocking { vehicleDao.hasActiveExecution(vehicleEntity.id, checklistId) },
                    activeExecutionId = runBlocking { vehicleDao.getActiveExecutionId(vehicleEntity.id, checklistId) }
                )
            }
        }
    }

    
    override suspend fun fetchVehicleChecklistsFromRemote(vehicleId: Int): Result<List<Checklist>> {
        return try {
            val response = vehicleApi.getVehicleChecklists(vehicleId)
            val checklists = response.checklists.map { it.toDomain() }
            Result.success(checklists)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// Helper function to parse ISO datetime - duplicate from mappers but needed here
private fun parseIsoDateTime(dateTimeString: String): kotlinx.datetime.Instant {
    return try {
        kotlinx.datetime.Instant.parse(dateTimeString)
    } catch (e: Exception) {
        kotlinx.datetime.Clock.System.now()
    }
}