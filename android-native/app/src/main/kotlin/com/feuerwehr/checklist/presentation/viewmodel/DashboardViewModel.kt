package com.feuerwehr.checklist.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.feuerwehr.checklist.presentation.error.BaseErrorHandlingViewModel
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
    private val getVehiclesUseCase: com.feuerwehr.checklist.domain.usecase.GetVehiclesUseCase,
    private val authRepository: com.feuerwehr.checklist.domain.repository.AuthRepository
) : BaseErrorHandlingViewModel() {
    
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    
    fun loadDashboardData() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        
        safeExecute("loadDashboardData") {
            // Load vehicle count (just take first emission for dashboard summary)
            getVehiclesUseCase().collect { vehicles ->
                val vehicleCount = vehicles.size
                val tuvExpiringCount = vehicles.count { vehicle ->
                    // Simple heuristic: assume some vehicles need TÜV soon
                    // In real implementation, this would check actual TÜV dates from TUV repository
                    vehicle.id % 3 == 0 // Every 3rd vehicle "needs" TÜV (demo data)
                }
                
                // Load checklist count
                getChecklistsUseCase().collect { checklists ->
                    val checklistCount = checklists.size
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        vehicleCount = vehicleCount,
                        checklistCount = checklistCount,
                        tuvExpiringCount = tuvExpiringCount,
                        recentActivities = listOf(
                            "Dashboard geladen mit $vehicleCount Fahrzeugen",
                            "Checklisten verfügbar: $checklistCount",
                            "TÜV-Termine: $tuvExpiringCount bald fällig",
                            "System bereit für Fahrzeugprüfung"
                        )
                    )
                }
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
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