package com.feuerwehr.checklist.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.feuerwehr.checklist.domain.model.Vehicle
import com.feuerwehr.checklist.domain.model.Checklist
import com.feuerwehr.checklist.domain.model.ChecklistExecution
import com.feuerwehr.checklist.domain.model.VehicleChecklistStatus
import com.feuerwehr.checklist.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Vehicle-related screens
 * Manages vehicle data and UI state
 */
@HiltViewModel
class VehicleViewModel @Inject constructor(
    private val getVehiclesUseCase: GetVehiclesUseCase,
    private val getVehicleByIdUseCase: GetVehicleByIdUseCase,
    private val fetchVehiclesFromRemoteUseCase: FetchVehiclesFromRemoteUseCase,
    private val syncVehiclesUseCase: SyncVehiclesUseCase,
    private val searchVehiclesUseCase: SearchVehiclesUseCase,
    // New vehicle-checklist use cases
    private val getVehicleChecklistsUseCase: GetVehicleChecklistsUseCase,
    private val getAvailableChecklistsForVehicleUseCase: GetAvailableChecklistsForVehicleUseCase,
    private val startChecklistForVehicleUseCase: StartChecklistForVehicleUseCase,
    private val getVehiclesForChecklistUseCase: GetVehiclesForChecklistUseCase,
    private val hasActiveExecutionUseCase: HasActiveExecutionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(VehicleUiState())
    val uiState: StateFlow<VehicleUiState> = _uiState.asStateFlow()

    init {
        loadVehicles()
    }

    fun loadVehicles() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Offline-first: load from local database
                getVehiclesUseCase().collect { vehicles ->
                    _uiState.value = _uiState.value.copy(
                        vehicles = vehicles,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun refreshVehicles() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)
            
            try {
                // Fetch from remote and sync to local
                fetchVehiclesFromRemoteUseCase().fold(
                    onSuccess = { vehicles ->
                        // Local data will be updated automatically via Flow
                        _uiState.value = _uiState.value.copy(
                            isRefreshing = false,
                            lastSyncSuccess = true
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isRefreshing = false,
                            error = error.message,
                            lastSyncSuccess = false
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isRefreshing = false,
                    error = e.message,
                    lastSyncSuccess = false
                )
            }
        }
    }

    fun syncVehicles() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true, error = null)
            
            syncVehiclesUseCase().fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isSyncing = false,
                        lastSyncSuccess = true
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isSyncing = false,
                        error = error.message,
                        lastSyncSuccess = false
                    )
                }
            )
        }
    }

    fun selectVehicle(vehicleId: Int) {
        viewModelScope.launch {
            try {
                val vehicle = getVehicleByIdUseCase(vehicleId)
                _uiState.value = _uiState.value.copy(selectedVehicle = vehicle)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun searchVehicles(query: String) {
        if (query.isBlank()) {
            loadVehicles()
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, searchQuery = query)
            
            try {
                searchVehiclesUseCase(query).collect { searchResults ->
                    _uiState.value = _uiState.value.copy(
                        vehicles = searchResults,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun clearSearch() {
        _uiState.value = _uiState.value.copy(searchQuery = "")
        loadVehicles()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(selectedVehicle = null)
    }

    // Vehicle-Checklist functionality
    
    fun loadVehicleChecklists(vehicleId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingChecklists = true, checklistError = null)
            
            try {
                getVehicleChecklistsUseCase(vehicleId).collect { checklists ->
                    // Convert checklists to VehicleChecklistStatus
                    val checklistStatuses = checklists.map { checklist ->
                        VehicleChecklistStatus(
                            checklist = checklist,
                            hasActiveExecution = false, // TODO: check actual status
                            activeExecutionId = null,
                            latestExecution = null
                        )
                    }
                    _uiState.value = _uiState.value.copy(
                        vehicleChecklists = checklistStatuses,
                        isLoadingChecklists = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingChecklists = false,
                    checklistError = e.message
                )
            }
        }
    }

    fun loadAvailableChecklistsForVehicle(vehicleId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingAvailableChecklists = true, checklistError = null)
            
            try {
                getAvailableChecklistsForVehicleUseCase(vehicleId).collect { checklistStatuses ->
                    // Extract checklists from the status objects
                    val checklists = checklistStatuses.map { it.checklist }
                    _uiState.value = _uiState.value.copy(
                        availableChecklists = checklists,
                        isLoadingAvailableChecklists = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingAvailableChecklists = false,
                    checklistError = e.message
                )
            }
        }
    }

    fun startChecklistForVehicle(vehicleId: Int, checklistId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isStartingChecklist = true, checklistError = null)
            
            try {
                startChecklistForVehicleUseCase(vehicleId, checklistId).fold(
                    onSuccess = { execution ->
                        _uiState.value = _uiState.value.copy(
                            activeExecution = execution,
                            isStartingChecklist = false
                        )
                        // Reload checklists to update status
                        loadVehicleChecklists(vehicleId)
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isStartingChecklist = false,
                            checklistError = error.message
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isStartingChecklist = false,
                    checklistError = e.message
                )
            }
        }
    }

    fun loadVehiclesForChecklist(checklistId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingVehiclesForChecklist = true, checklistError = null)
            
            try {
                getVehiclesForChecklistUseCase(checklistId).collect { checklistStatuses ->
                    // Extract vehicles from the status objects
                    val vehicles = checklistStatuses.mapNotNull { it.vehicle }
                    _uiState.value = _uiState.value.copy(
                        vehiclesForChecklist = vehicles,
                        isLoadingVehiclesForChecklist = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingVehiclesForChecklist = false,
                    checklistError = e.message
                )
            }
        }
    }

    fun checkActiveExecution(vehicleId: Int, checklistId: Int) {
        viewModelScope.launch {
            try {
                val hasActive = hasActiveExecutionUseCase(vehicleId, checklistId)
                _uiState.value = _uiState.value.copy(
                    hasActiveExecution = hasActive
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    checklistError = e.message
                )
            }
        }
    }

    fun clearChecklistError() {
        _uiState.value = _uiState.value.copy(checklistError = null)
    }

    fun clearActiveExecution() {
        _uiState.value = _uiState.value.copy(activeExecution = null)
    }
}

data class VehicleUiState(
    val vehicles: List<Vehicle> = emptyList(),
    val selectedVehicle: Vehicle? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isSyncing: Boolean = false,
    val error: String? = null,
    val lastSyncSuccess: Boolean = true,
    // Vehicle-checklist related state
    val vehicleChecklists: List<VehicleChecklistStatus> = emptyList(),
    val availableChecklists: List<Checklist> = emptyList(),
    val vehiclesForChecklist: List<Vehicle> = emptyList(),
    val activeExecution: ChecklistExecution? = null,
    val hasActiveExecution: Boolean = false,
    val isLoadingChecklists: Boolean = false,
    val isLoadingAvailableChecklists: Boolean = false,
    val isLoadingVehiclesForChecklist: Boolean = false,
    val isStartingChecklist: Boolean = false,
    val checklistError: String? = null
)