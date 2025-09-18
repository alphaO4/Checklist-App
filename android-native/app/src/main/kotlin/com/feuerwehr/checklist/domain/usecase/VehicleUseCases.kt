package com.feuerwehr.checklist.domain.usecase

import com.feuerwehr.checklist.domain.model.Vehicle
import com.feuerwehr.checklist.domain.model.Checklist
import com.feuerwehr.checklist.domain.model.ChecklistExecution
import com.feuerwehr.checklist.domain.model.VehicleChecklistStatus
import com.feuerwehr.checklist.domain.repository.VehicleRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for getting all vehicles
 */
class GetVehiclesUseCase @Inject constructor(
    private val vehicleRepository: VehicleRepository
) {
    operator fun invoke(): Flow<List<Vehicle>> {
        return vehicleRepository.getVehicles()
    }
}

/**
 * Use case for getting a vehicle by ID
 */
class GetVehicleByIdUseCase @Inject constructor(
    private val vehicleRepository: VehicleRepository
) {
    suspend operator fun invoke(id: Int): Vehicle? {
        return vehicleRepository.getVehicleById(id)
    }
}

/**
 * Use case for getting vehicles by group
 */
class GetVehiclesByGroupUseCase @Inject constructor(
    private val vehicleRepository: VehicleRepository
) {
    operator fun invoke(vehicleGroupId: Int): Flow<List<Vehicle>> {
        return vehicleRepository.getVehiclesByGroup(vehicleGroupId)
    }
}

/**
 * Use case for fetching vehicles from remote
 */
class FetchVehiclesFromRemoteUseCase @Inject constructor(
    private val vehicleRepository: VehicleRepository
) {
    suspend operator fun invoke(): Result<List<Vehicle>> {
        return vehicleRepository.fetchVehiclesFromRemote()
    }
}

/**
 * Use case for syncing vehicles
 */
class SyncVehiclesUseCase @Inject constructor(
    private val vehicleRepository: VehicleRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return vehicleRepository.syncVehicles()
    }
}

/**
 * Use case for searching vehicles
 */
class SearchVehiclesUseCase @Inject constructor(
    private val vehicleRepository: VehicleRepository
) {
    operator fun invoke(query: String): Flow<List<Vehicle>> {
        return vehicleRepository.searchVehicles(query)
    }
}

// Vehicle-Checklist relationship use cases

/**
 * Use case for getting checklists available for a specific vehicle
 */
class GetVehicleChecklistsUseCase @Inject constructor(
    private val vehicleRepository: VehicleRepository
) {
    operator fun invoke(vehicleId: Int): Flow<List<Checklist>> {
        return vehicleRepository.getChecklistsForVehicle(vehicleId)
    }
}

/**
 * Use case for getting available checklists for execution on a specific vehicle
 */
class GetAvailableChecklistsForVehicleUseCase @Inject constructor(
    private val vehicleRepository: VehicleRepository
) {
    operator fun invoke(vehicleId: Int): Flow<List<VehicleChecklistStatus>> {
        return vehicleRepository.getAvailableChecklistsForVehicle(vehicleId)
    }
}

/**
 * Use case for starting a checklist execution on a vehicle
 */
class StartChecklistForVehicleUseCase @Inject constructor(
    private val vehicleRepository: VehicleRepository
) {
    suspend operator fun invoke(vehicleId: Int, checklistId: Int): Result<ChecklistExecution> {
        return vehicleRepository.startChecklistForVehicle(vehicleId, checklistId)
    }
}

/**
 * Use case for getting vehicles that can use a specific checklist
 */
class GetVehiclesForChecklistUseCase @Inject constructor(
    private val vehicleRepository: VehicleRepository
) {
    operator fun invoke(checklistId: Int): Flow<List<VehicleChecklistStatus>> {
        return vehicleRepository.getVehiclesForChecklist(checklistId)
    }
}

/**
 * Use case for checking if a vehicle has an active checklist execution
 */
class HasActiveExecutionUseCase @Inject constructor(
    private val vehicleRepository: VehicleRepository
) {
    suspend operator fun invoke(vehicleId: Int, checklistId: Int): Boolean {
        return vehicleRepository.hasActiveExecution(vehicleId, checklistId)
    }
}