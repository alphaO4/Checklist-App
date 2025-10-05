package com.feuerwehr.checklist.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.feuerwehr.checklist.domain.model.Vehicle
import com.feuerwehr.checklist.domain.model.Checklist
import com.feuerwehr.checklist.domain.model.ChecklistExecution
import com.feuerwehr.checklist.domain.model.VehicleChecklistStatus
import com.feuerwehr.checklist.domain.model.TuvAppointment
import com.feuerwehr.checklist.domain.usecase.*
import com.feuerwehr.checklist.presentation.error.BaseErrorHandlingViewModel
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Vehicle-related screens
 * Manages vehicle data and UI state with enhanced error handling
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
) : BaseErrorHandlingViewModel() {

    private val _uiState = MutableStateFlow(VehicleUiState())
    val uiState: StateFlow<VehicleUiState> = _uiState.asStateFlow()
    
    private var lastFailedOperation: (() -> Unit)? = null

    init {
        loadVehicles()
    }

    fun loadVehicles() {
        lastFailedOperation = ::loadVehicles
        
        safeExecute(
            operation = {
                _uiState.value = _uiState.value.copy(isLoading = true)
                clearError()
                
                // Offline-first: load from local database
                getVehiclesUseCase().collect { vehicles ->
                    _uiState.value = _uiState.value.copy(
                        vehicles = vehicles,
                        isLoading = false
                    )
                }
            }
        ) { error ->
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun refreshVehicles() {
        lastFailedOperation = ::refreshVehicles
        
        safeExecuteResult(
            operation = {
                _uiState.value = _uiState.value.copy(isRefreshing = true)
                clearError()
                fetchVehiclesFromRemoteUseCase()
            },
            onSuccess = { vehicles ->
                _uiState.value = _uiState.value.copy(
                    isRefreshing = false,
                    lastSyncSuccess = true
                )
            }
        ) { error ->
            _uiState.value = _uiState.value.copy(
                isRefreshing = false,
                lastSyncSuccess = false
            )
        }
    }

    fun syncVehicles() {
        lastFailedOperation = ::syncVehicles
        
        safeExecuteResult(
            operation = {
                _uiState.value = _uiState.value.copy(isSyncing = true)
                clearError()
                syncVehiclesUseCase()
            },
            onSuccess = {
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    lastSyncSuccess = true
                )
            }
        ) { error ->
            _uiState.value = _uiState.value.copy(
                isSyncing = false,
                lastSyncSuccess = false
            )
        }
    }
    
    override fun retryLastOperation() {
        lastFailedOperation?.invoke()
    }

    fun selectVehicle(vehicleId: Int) {
        safeExecute(
            operation = {
                val vehicle = getVehicleByIdUseCase(vehicleId)
                _uiState.value = _uiState.value.copy(selectedVehicle = vehicle)
            }
        )
    }

    fun searchVehicles(query: String) {
        if (query.isBlank()) {
            loadVehicles()
            return
        }

        safeExecute(
            operation = {
                _uiState.value = _uiState.value.copy(isLoading = true, searchQuery = query)
                clearError()
                
                searchVehiclesUseCase(query).collect { searchResults ->
                    _uiState.value = _uiState.value.copy(
                        vehicles = searchResults,
                        isLoading = false
                    )
                }
            }
        ) { error ->
            _uiState.value = _uiState.value.copy(isLoading = false)
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

    // TÜV Management Methods
    fun loadTuvAppointments(vehicleId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingTuvAppointments = true, tuvError = null)
            
            try {
                // TODO: Implement getTuvAppointmentsUseCase when available
                // For now, create mock appointments for demonstration
                val mockAppointments = createMockTuvAppointments(vehicleId)
                
                _uiState.value = _uiState.value.copy(
                    tuvAppointments = mockAppointments,
                    isLoadingTuvAppointments = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingTuvAppointments = false,
                    tuvError = e.message
                )
            }
        }
    }

    fun scheduleTuvAppointment(vehicleId: Int, date: LocalDate, type: String) {
        viewModelScope.launch {
            try {
                // TODO: Implement scheduleTuvAppointmentUseCase when available
                // For now, add to existing list
                val newAppointment = TuvAppointment(
                    id = (1..1000).random(),
                    fahrzeugId = vehicleId,
                    ablaufDatum = date,
                    status = com.feuerwehr.checklist.domain.model.TuvStatus.CURRENT,
                    letztePruefung = null,
                    createdAt = kotlinx.datetime.Clock.System.now()
                )
                
                val updatedAppointments = _uiState.value.tuvAppointments + newAppointment
                _uiState.value = _uiState.value.copy(tuvAppointments = updatedAppointments)
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(tuvError = e.message)
            }
        }
    }

    private fun createMockTuvAppointments(vehicleId: Int): List<TuvAppointment> {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        return listOf(
            TuvAppointment(
                id = 1,
                fahrzeugId = vehicleId,
                ablaufDatum = today,
                status = com.feuerwehr.checklist.domain.model.TuvStatus.CURRENT,
                letztePruefung = today,
                createdAt = kotlinx.datetime.Clock.System.now()
            ),
            TuvAppointment(
                id = 2,
                fahrzeugId = vehicleId,
                ablaufDatum = today,
                status = com.feuerwehr.checklist.domain.model.TuvStatus.WARNING,
                letztePruefung = today,
                createdAt = kotlinx.datetime.Clock.System.now()
            )
        )
    }

    fun clearTuvError() {
        _uiState.value = _uiState.value.copy(tuvError = null)
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
    val checklistError: String? = null,
    // TÜV management state
    val tuvAppointments: List<com.feuerwehr.checklist.domain.model.TuvAppointment> = emptyList(),
    val isLoadingTuvAppointments: Boolean = false,
    val tuvError: String? = null
)