package com.feuerwehr.checklist.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.feuerwehr.checklist.data.models.*
import com.feuerwehr.checklist.data.repository.VehicleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for vehicle management
 */
@HiltViewModel
class VehicleViewModel @Inject constructor(
    private val vehicleRepository: VehicleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VehicleUiState())
    val uiState: StateFlow<VehicleUiState> = _uiState.asStateFlow()

    // Vehicles flow from repository
    private val _vehiclesFlow = MutableStateFlow(false) // Trigger for refresh
    val vehicles: StateFlow<List<Vehicle>> = _vehiclesFlow
        .flatMapLatest { forceRefresh ->
            vehicleRepository.getVehicles(forceRefresh)
                .map { result ->
                    result.getOrElse { emptyList() }
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        loadVehicles()
    }

    fun loadVehicles(forceRefresh: Boolean = false) {
        _vehiclesFlow.value = forceRefresh
    }

    fun getVehicleById(id: Int) {
        viewModelScope.launch {
            vehicleRepository.getVehicleById(id)
                .onSuccess { vehicle ->
                    _uiState.update {
                        it.copy(selectedVehicle = vehicle)
                    }
                }
                .onFailure { exception ->
                    _uiState.update {
                        it.copy(error = "Fahrzeug nicht gefunden: ${exception.message}")
                    }
                }
        }
    }

    fun searchVehicles(query: String) {
        if (query.isBlank()) {
            loadVehicles()
            return
        }

        viewModelScope.launch {
            vehicleRepository.searchVehicles(query)
                .onSuccess { searchResults ->
                    _uiState.update {
                        it.copy(searchResults = searchResults)
                    }
                }
                .onFailure { exception ->
                    _uiState.update {
                        it.copy(error = "Suche fehlgeschlagen: ${exception.message}")
                    }
                }
        }
    }

    fun createVehicle(kennzeichen: String, fahrzeugtypId: Int, fahrzeuggruppeId: Int) {
        if (_uiState.value.isCreating) return

        _uiState.update { it.copy(isCreating = true, error = null) }

        viewModelScope.launch {
            vehicleRepository.createVehicle(kennzeichen, fahrzeugtypId, fahrzeuggruppeId)
                .onSuccess { vehicle ->
                    _uiState.update {
                        it.copy(
                            isCreating = false,
                            selectedVehicle = vehicle
                        )
                    }
                    // Refresh vehicles list
                    loadVehicles(forceRefresh = true)
                }
                .onFailure { exception ->
                    _uiState.update {
                        it.copy(
                            isCreating = false,
                            error = exception.message ?: "Fahrzeug konnte nicht erstellt werden"
                        )
                    }
                }
        }
    }

    fun updateVehicle(
        id: Int,
        kennzeichen: String? = null,
        fahrzeugtypId: Int? = null,
        fahrzeuggruppeId: Int? = null
    ) {
        if (_uiState.value.isUpdating) return

        _uiState.update { it.copy(isUpdating = true, error = null) }

        viewModelScope.launch {
            vehicleRepository.updateVehicle(id, kennzeichen, fahrzeugtypId, fahrzeuggruppeId)
                .onSuccess { vehicle ->
                    _uiState.update {
                        it.copy(
                            isUpdating = false,
                            selectedVehicle = vehicle
                        )
                    }
                    // Refresh vehicles list
                    loadVehicles(forceRefresh = true)
                }
                .onFailure { exception ->
                    _uiState.update {
                        it.copy(
                            isUpdating = false,
                            error = exception.message ?: "Fahrzeug konnte nicht aktualisiert werden"
                        )
                    }
                }
        }
    }

    fun deleteVehicle(id: Int) {
        if (_uiState.value.isDeleting) return

        _uiState.update { it.copy(isDeleting = true, error = null) }

        viewModelScope.launch {
            vehicleRepository.deleteVehicle(id)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isDeleting = false,
                            selectedVehicle = null
                        )
                    }
                    // Refresh vehicles list
                    loadVehicles(forceRefresh = true)
                }
                .onFailure { exception ->
                    _uiState.update {
                        it.copy(
                            isDeleting = false,
                            error = exception.message ?: "Fahrzeug konnte nicht gelöscht werden"
                        )
                    }
                }
        }
    }

    fun getVehiclesByType(fahrzeugtypId: Int) {
        viewModelScope.launch {
            vehicleRepository.getVehiclesByType(fahrzeugtypId)
                .onSuccess { vehiclesOfType ->
                    _uiState.update {
                        it.copy(filteredVehicles = vehiclesOfType)
                    }
                }
                .onFailure { exception ->
                    _uiState.update {
                        it.copy(error = "Fahrzeuge konnten nicht geladen werden: ${exception.message}")
                    }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearSelectedVehicle() {
        _uiState.update { it.copy(selectedVehicle = null) }
    }

    fun clearSearchResults() {
        _uiState.update { it.copy(searchResults = null) }
    }

    fun clearFilters() {
        _uiState.update { it.copy(filteredVehicles = null) }
    }
}

data class VehicleUiState(
    val isCreating: Boolean = false,
    val isUpdating: Boolean = false,
    val isDeleting: Boolean = false,
    val selectedVehicle: Vehicle? = null,
    val searchResults: List<Vehicle>? = null,
    val filteredVehicles: List<Vehicle>? = null,
    val error: String? = null
)