package com.feuerwehr.checklist.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.feuerwehr.checklist.domain.model.Checklist
import com.feuerwehr.checklist.domain.repository.ChecklistRepository
import com.feuerwehr.checklist.domain.usecase.SyncTemplatesUseCase
import com.feuerwehr.checklist.domain.usecase.SyncStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing checklist templates with timestamp-based sync
 */
@HiltViewModel
class TemplateViewModel @Inject constructor(
    private val checklistRepository: ChecklistRepository,
    private val syncTemplatesUseCase: SyncTemplatesUseCase
) : ViewModel() {

    // UI State
    private val _uiState = MutableStateFlow(TemplateUiState())
    val uiState = _uiState.asStateFlow()

    // Templates from local database (offline-first)
    val templates: StateFlow<List<Checklist>> = checklistRepository.getTemplates()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Sync status
    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus = _syncStatus.asStateFlow()

    init {
        // Auto-sync templates when ViewModel is initialized
        syncTemplates()
    }

    /**
     * Sync templates from remote with timestamp-based conflict resolution
     * This will reflect the backend template cleanup (removed test templates)
     */
    fun syncTemplates() {
        viewModelScope.launch {
            _syncStatus.value = SyncStatus.Syncing
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val result = syncTemplatesUseCase.syncTemplates()
                
                if (result.isSuccess) {
                    _syncStatus.value = SyncStatus.Success
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = null,
                        lastSyncTime = System.currentTimeMillis()
                    )
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Sync failed"
                    _syncStatus.value = SyncStatus.Error(error)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error
                    )
                }
            } catch (e: Exception) {
                val error = e.message ?: "Unknown error"
                _syncStatus.value = SyncStatus.Error(error)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = error
                )
            }
        }
    }

    /**
     * Refresh templates - convenience method
     */
    fun refresh() {
        syncTemplates()
    }

    /**
     * Clear error state
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

/**
 * UI state for template screen
 */
data class TemplateUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val lastSyncTime: Long? = null
)