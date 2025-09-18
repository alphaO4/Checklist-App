package com.feuerwehr.checklist.domain.usecase

import com.feuerwehr.checklist.domain.model.Checklist
import com.feuerwehr.checklist.domain.model.ChecklistExecution
import com.feuerwehr.checklist.domain.model.ItemResult
import com.feuerwehr.checklist.domain.repository.ChecklistRepository
import com.feuerwehr.checklist.data.mapper.ChecklistPage
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use cases for Checklist operations
 * Contains business logic for the checklist feature
 */

@Singleton
class GetChecklistsUseCase @Inject constructor(
    private val checklistRepository: ChecklistRepository
) {
    operator fun invoke(): Flow<List<Checklist>> {
        return checklistRepository.getAllChecklists()
    }
}

@Singleton
class GetChecklistsByVehicleGroupUseCase @Inject constructor(
    private val checklistRepository: ChecklistRepository
) {
    operator fun invoke(groupId: Int): Flow<List<Checklist>> {
        return checklistRepository.getChecklistsByVehicleGroup(groupId)
    }
}

@Singleton
class FetchChecklistsFromRemoteUseCase @Inject constructor(
    private val checklistRepository: ChecklistRepository
) {
    suspend operator fun invoke(
        page: Int = 1,
        perPage: Int = 50,
        vehicleGroupId: Int? = null,
        template: Boolean? = null,
        name: String? = null
    ): Result<ChecklistPage> {
        return checklistRepository.fetchChecklistsFromRemote(
            page, perPage, vehicleGroupId, template, name
        )
    }
}

@Singleton
class GetChecklistByIdUseCase @Inject constructor(
    private val checklistRepository: ChecklistRepository
) {
    suspend operator fun invoke(id: Int): Checklist? {
        return checklistRepository.getChecklistById(id)
    }
}

@Singleton
class CreateChecklistUseCase @Inject constructor(
    private val checklistRepository: ChecklistRepository
) {
    suspend operator fun invoke(checklist: Checklist): Result<Checklist> {
        return checklistRepository.createChecklist(checklist)
    }
}

@Singleton
class SyncChecklistsUseCase @Inject constructor(
    private val checklistRepository: ChecklistRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return checklistRepository.syncChecklists()
    }
}

// Template use cases
@Singleton
class GetTemplatesUseCase @Inject constructor(
    private val checklistRepository: ChecklistRepository
) {
    operator fun invoke(): Flow<List<Checklist>> {
        return checklistRepository.getTemplates()
    }
}

@Singleton
class SearchChecklistsUseCase @Inject constructor(
    private val checklistRepository: ChecklistRepository
) {
    operator fun invoke(nameFilter: String): Flow<List<Checklist>> {
        return checklistRepository.searchChecklistsByName(nameFilter)
    }
}

// Checklist execution use cases
@Singleton
class StartChecklistExecutionUseCase @Inject constructor(
    private val checklistRepository: ChecklistRepository
) {
    suspend operator fun invoke(checklistId: Int, vehicleId: Int): Result<ChecklistExecution> {
        return checklistRepository.startExecution(checklistId, vehicleId)
    }
}

@Singleton
class CompleteExecutionUseCase @Inject constructor(
    private val checklistRepository: ChecklistRepository
) {
    suspend operator fun invoke(executionId: Int): Result<ChecklistExecution> {
        return checklistRepository.completeExecution(executionId)
    }
}

@Singleton
class SubmitItemResultUseCase @Inject constructor(
    private val checklistRepository: ChecklistRepository
) {
    suspend operator fun invoke(result: ItemResult): Result<ItemResult> {
        return checklistRepository.submitItemResult(result)
    }
}

@Singleton 
class GetChecklistsByVehicleIdUseCase @Inject constructor(
    private val checklistRepository: ChecklistRepository,
    private val vehicleRepository: com.feuerwehr.checklist.domain.repository.VehicleRepository
) {
    suspend operator fun invoke(vehicleId: Int): Flow<List<Checklist>> {
        // Get the vehicle to find its group ID
        val vehicle = vehicleRepository.getVehicleById(vehicleId)
        return if (vehicle != null) {
            // Get checklists for the vehicle's group
            checklistRepository.getChecklistsByVehicleGroup(vehicle.fahrzeuggruppeId)
        } else {
            // Return empty flow if vehicle not found
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }
}