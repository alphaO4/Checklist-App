package com.feuerwehr.checklist.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.feuerwehr.checklist.domain.model.Checklist
import com.feuerwehr.checklist.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Checklist screen
 * Manages checklist data and UI state
 */
@HiltViewModel
class ChecklistViewModel @Inject constructor(
    private val getChecklistsUseCase: GetChecklistsUseCase,
    private val fetchChecklistsFromRemoteUseCase: FetchChecklistsFromRemoteUseCase,
    private val syncChecklistsUseCase: SyncChecklistsUseCase,
    private val getChecklistByIdUseCase: GetChecklistByIdUseCase,
    private val getTemplatesUseCase: GetTemplatesUseCase,
    private val searchChecklistsUseCase: SearchChecklistsUseCase,
    private val getChecklistsByVehicleIdUseCase: GetChecklistsByVehicleIdUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChecklistUiState())
    val uiState: StateFlow<ChecklistUiState> = _uiState.asStateFlow()

    init {
        loadChecklists()
    }

    fun loadChecklists() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // First load from local database (offline-first)
                getChecklistsUseCase().collect { checklists ->
                    _uiState.value = _uiState.value.copy(
                        checklists = checklists,
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

    fun refreshChecklists() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)
            
            try {
                // Fetch from remote and sync to local
                fetchChecklistsFromRemoteUseCase().fold(
                    onSuccess = { checklistPage ->
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

    fun syncChecklists() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true, error = null)
            
            syncChecklistsUseCase().fold(
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

    fun selectChecklist(checklistId: Int) {
        viewModelScope.launch {
            try {
                val checklist = getChecklistByIdUseCase(checklistId)
                _uiState.value = _uiState.value.copy(selectedChecklist = checklist)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(selectedChecklist = null)
    }

    fun loadTemplates() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                getTemplatesUseCase().collect { templates ->
                    _uiState.value = _uiState.value.copy(
                        templates = templates,
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

    fun searchChecklists(query: String) {
        if (query.isBlank()) {
            loadChecklists()
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, searchQuery = query)
            
            try {
                searchChecklistsUseCase(query).collect { searchResults ->
                    _uiState.value = _uiState.value.copy(
                        checklists = searchResults,
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
        loadChecklists()
    }

    fun loadChecklistsForVehicle(vehicleId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Load checklists for the specific vehicle's group
                getChecklistsByVehicleIdUseCase(vehicleId).collect { checklists ->
                    _uiState.value = _uiState.value.copy(
                        checklists = checklists,
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
}

data class ChecklistUiState(
    val checklists: List<Checklist> = emptyList(),
    val templates: List<Checklist> = emptyList(),
    val selectedChecklist: Checklist? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isSyncing: Boolean = false,
    val error: String? = null,
    val lastSyncSuccess: Boolean = true
)