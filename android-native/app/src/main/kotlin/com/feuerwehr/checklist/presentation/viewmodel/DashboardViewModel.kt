package com.feuerwehr.checklist.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for dashboard functionality
 * Aggregates data from various repositories for overview display
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getChecklistsUseCase: com.feuerwehr.checklist.domain.usecase.GetChecklistsUseCase,
    // TODO: Inject other repositories when created
    // private val vehicleRepository: VehicleRepository,
    // private val tuvRepository: TuvRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    
    fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                // Load checklist count from repository
                getChecklistsUseCase().collect { checklists ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        vehicleCount = 8, // TODO: Load from vehicle repository
                        checklistCount = checklists.size,
                        tuvExpiringCount = 2, // TODO: Load from TÜV repository
                        groupCount = 4, // TODO: Load from group repository
                        recentActivities = listOf(
                            "MTF B-2031 Checkliste durchgeführt",
                            "TÜV für LF B-2184 erneuert", 
                            "Neue Checkliste für RTB erstellt",
                            "Fahrzeug HLF B-2229 hinzugefügt"
                        )
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Fehler beim Laden der Dashboard-Daten: ${e.message}"
                )
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}

data class DashboardUiState(
    val isLoading: Boolean = false,
    val vehicleCount: Int = 0,
    val checklistCount: Int = 0,
    val tuvExpiringCount: Int = 0,
    val groupCount: Int = 0,
    val recentActivities: List<String> = emptyList(),
    val errorMessage: String? = null
)