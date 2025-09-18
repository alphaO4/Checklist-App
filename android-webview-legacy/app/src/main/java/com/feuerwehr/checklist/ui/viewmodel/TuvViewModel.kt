package com.feuerwehr.checklist.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.feuerwehr.checklist.data.models.*
import com.feuerwehr.checklist.data.repository.TuvRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * ViewModel for TÜV inspection management
 */
@HiltViewModel
class TuvViewModel @Inject constructor(
    private val tuvRepository: TuvRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TuvUiState())
    val uiState: StateFlow<TuvUiState> = _uiState.asStateFlow()

    // TÜV termine flow from repository
    private val _tuvTermineFlow = MutableStateFlow(false) // Trigger for refresh
    val tuvTermine: StateFlow<List<TuvTermin>> = _tuvTermineFlow
        .flatMapLatest { forceRefresh ->
            tuvRepository.getAllTuvTermine(forceRefresh)
                .map { result ->
                    result.getOrElse { emptyList() }
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Upcoming deadlines flow
    val upcomingDeadlines: StateFlow<List<TuvTermin>> = tuvTermine
        .map { termine ->
            val today = LocalDate.now()
            val warningDate = today.plusDays(30) // 30 days warning
            
            termine
                .filter { it.ablaufDatum >= today }
                .sortedBy { it.ablaufDatum }
                .filter { it.ablaufDatum <= warningDate || it.status == TuvStatus.WARNING }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Expired inspections flow
    val expiredInspections: StateFlow<List<TuvTermin>> = tuvTermine
        .map { termine ->
            val today = LocalDate.now()
            termine
                .filter { it.ablaufDatum < today || it.status == TuvStatus.EXPIRED }
                .sortedBy { it.ablaufDatum }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        loadTuvTermine()
    }

    fun loadTuvTermine(forceRefresh: Boolean = false) {
        _tuvTermineFlow.value = forceRefresh
    }

    fun getTuvTerminById(id: Int) {
        viewModelScope.launch {
            tuvRepository.getTuvTerminById(id)
                .onSuccess { tuvTermin ->
                    _uiState.update {
                        it.copy(selectedTuvTermin = tuvTermin)
                    }
                }
                .onFailure { exception ->
                    _uiState.update {
                        it.copy(error = "TÜV-Termin nicht gefunden: ${exception.message}")
                    }
                }
        }
    }

    fun getTuvTermineByVehicle(fahrzeugId: Int) {
        viewModelScope.launch {
            tuvRepository.getTuvTermineByVehicle(fahrzeugId)
                .onSuccess { termine ->
                    _uiState.update {
                        it.copy(vehicleTuvTermine = termine)
                    }
                }
                .onFailure { exception ->
                    _uiState.update {
                        it.copy(error = "TÜV-Termine konnten nicht geladen werden: ${exception.message}")
                    }
                }
        }
    }

    fun createTuvTermin(
        fahrzeugId: Int,
        ablaufDatum: LocalDate,
        letztePruefung: LocalDate? = null
    ) {
        if (_uiState.value.isCreating) return

        _uiState.update { it.copy(isCreating = true, error = null) }

        viewModelScope.launch {
            tuvRepository.createTuvTermin(fahrzeugId, ablaufDatum, letztePruefung)
                .onSuccess { tuvTermin ->
                    _uiState.update {
                        it.copy(
                            isCreating = false,
                            selectedTuvTermin = tuvTermin
                        )
                    }
                    // Refresh termine list
                    loadTuvTermine(forceRefresh = true)
                }
                .onFailure { exception ->
                    _uiState.update {
                        it.copy(
                            isCreating = false,
                            error = exception.message ?: "TÜV-Termin konnte nicht erstellt werden"
                        )
                    }
                }
        }
    }

    fun updateTuvTermin(
        id: Int,
        ablaufDatum: LocalDate? = null,
        letztePruefung: LocalDate? = null,
        status: TuvStatus? = null
    ) {
        if (_uiState.value.isUpdating) return

        _uiState.update { it.copy(isUpdating = true, error = null) }

        viewModelScope.launch {
            tuvRepository.updateTuvTermin(id, ablaufDatum, letztePruefung, status)
                .onSuccess { tuvTermin ->
                    _uiState.update {
                        it.copy(
                            isUpdating = false,
                            selectedTuvTermin = tuvTermin
                        )
                    }
                    // Refresh termine list
                    loadTuvTermine(forceRefresh = true)
                }
                .onFailure { exception ->
                    _uiState.update {
                        it.copy(
                            isUpdating = false,
                            error = exception.message ?: "TÜV-Termin konnte nicht aktualisiert werden"
                        )
                    }
                }
        }
    }

    fun deleteTuvTermin(id: Int) {
        if (_uiState.value.isDeleting) return

        _uiState.update { it.copy(isDeleting = true, error = null) }

        viewModelScope.launch {
            tuvRepository.deleteTuvTermin(id)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isDeleting = false,
                            selectedTuvTermin = null
                        )
                    }
                    // Refresh termine list
                    loadTuvTermine(forceRefresh = true)
                }
                .onFailure { exception ->
                    _uiState.update {
                        it.copy(
                            isDeleting = false,
                            error = exception.message ?: "TÜV-Termin konnte nicht gelöscht werden"
                        )
                    }
                }
        }
    }

    fun markInspectionComplete(id: Int, pruefungsDatum: LocalDate) {
        updateTuvTermin(
            id = id,
            letztePruefung = pruefungsDatum,
            status = TuvStatus.CURRENT
        )
    }

    fun extendInspection(id: Int, neuesAblaufDatum: LocalDate) {
        updateTuvTermin(
            id = id,
            ablaufDatum = neuesAblaufDatum,
            status = TuvStatus.CURRENT
        )
    }

    fun filterByStatus(status: TuvStatus) {
        _uiState.update { 
            it.copy(filterStatus = status)
        }
    }

    fun clearStatusFilter() {
        _uiState.update { 
            it.copy(filterStatus = null)
        }
    }

    fun getStatusSummary(): StateFlow<TuvStatusSummary> = tuvTermine
        .map { termine ->
            val today = LocalDate.now()
            val warningDate = today.plusDays(30)
            
            val current = termine.count { 
                it.status == TuvStatus.CURRENT && it.ablaufDatum > warningDate 
            }
            val warning = termine.count { 
                it.status == TuvStatus.WARNING || 
                (it.status == TuvStatus.CURRENT && it.ablaufDatum in today..warningDate)
            }
            val expired = termine.count { 
                it.status == TuvStatus.EXPIRED || it.ablaufDatum < today 
            }
            
            TuvStatusSummary(
                current = current,
                warning = warning,
                expired = expired,
                total = termine.size
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TuvStatusSummary()
        )

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearSelectedTuvTermin() {
        _uiState.update { it.copy(selectedTuvTermin = null) }
    }

    fun clearVehicleTuvTermine() {
        _uiState.update { it.copy(vehicleTuvTermine = null) }
    }
}

data class TuvUiState(
    val isCreating: Boolean = false,
    val isUpdating: Boolean = false,
    val isDeleting: Boolean = false,
    val selectedTuvTermin: TuvTermin? = null,
    val vehicleTuvTermine: List<TuvTermin>? = null,
    val filterStatus: TuvStatus? = null,
    val error: String? = null
)

data class TuvStatusSummary(
    val current: Int = 0,
    val warning: Int = 0,
    val expired: Int = 0,
    val total: Int = 0
)