package com.feuerwehr.checklist.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.feuerwehr.checklist.data.models.*
import com.feuerwehr.checklist.data.repository.ChecklistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for checklist management and admin editing
 */
@HiltViewModel
class ChecklistViewModel @Inject constructor(
    private val checklistRepository: ChecklistRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChecklistUiState())
    val uiState: StateFlow<ChecklistUiState> = _uiState.asStateFlow()

    // Templates flow from repository
    private val _templatesFlow = MutableStateFlow(false) // Trigger for refresh
    val templates: StateFlow<List<Checklist>> = _templatesFlow
        .flatMapLatest { forceRefresh ->
            checklistRepository.getTemplates(forceRefresh)
                .map { result ->
                    result.getOrElse { emptyList() }
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // CSV summary
    private val _csvSummary = MutableStateFlow<Map<String, Any>?>(null)
    val csvSummary: StateFlow<Map<String, Any>?> = _csvSummary.asStateFlow()

    init {
        loadTemplates()
        loadCsvSummary()
    }

    fun loadTemplates(forceRefresh: Boolean = false) {
        _templatesFlow.value = forceRefresh
    }

    fun loadCsvSummary() {
        viewModelScope.launch {
            checklistRepository.getCsvSummary()
                .onSuccess { summary ->
                    _csvSummary.value = summary
                }
                .onFailure { exception ->
                    _uiState.update {
                        it.copy(error = "CSV-Übersicht nicht verfügbar: ${exception.message}")
                    }
                }
        }
    }

    fun importCsvTemplates() {
        if (_uiState.value.isImporting) return

        _uiState.update { it.copy(isImporting = true, error = null) }

        viewModelScope.launch {
            checklistRepository.importCsvTemplates()
                .onSuccess { message ->
                    _uiState.update {
                        it.copy(
                            isImporting = false,
                            importResult = message
                        )
                    }
                    // Refresh templates after import
                    loadTemplates(forceRefresh = true)
                }
                .onFailure { exception ->
                    _uiState.update {
                        it.copy(
                            isImporting = false,
                            error = exception.message ?: "Import fehlgeschlagen"
                        )
                    }
                }
        }
    }

    fun getChecklistWithItems(checklistId: Int) {
        if (_uiState.value.isLoading) return

        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            checklistRepository.getChecklistWithItems(checklistId)
                .onSuccess { checklist ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            selectedChecklist = checklist
                        )
                    }
                }
                .onFailure { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Checkliste konnte nicht geladen werden: ${exception.message}"
                        )
                    }
                }
        }
    }

    fun startChecklistExecution(checklistId: Int, fahrzeugId: Int) {
        if (_uiState.value.isStarting) return

        _uiState.update { it.copy(isStarting = true, error = null) }

        viewModelScope.launch {
            checklistRepository.startChecklistExecution(checklistId, fahrzeugId)
                .onSuccess { execution ->
                    _uiState.update {
                        it.copy(
                            isStarting = false,
                            currentExecution = execution
                        )
                    }
                }
                .onFailure { exception ->
                    _uiState.update {
                        it.copy(
                            isStarting = false,
                            error = "Checkliste konnte nicht gestartet werden: ${exception.message}"
                        )
                    }
                }
        }
    }

    fun updateItemResult(executionId: Int, itemId: Int, status: ItemStatus, kommentar: String? = null) {
        viewModelScope.launch {
            checklistRepository.updateItemResult(executionId, itemId, status, kommentar)
                .onFailure { exception ->
                    _uiState.update {
                        it.copy(error = "Ergebnis konnte nicht gespeichert werden: ${exception.message}")
                    }
                }
        }
    }

    fun completeChecklistExecution(executionId: Int) {
        viewModelScope.launch {
            checklistRepository.completeChecklistExecution(executionId)
                .onSuccess { execution ->
                    _uiState.update {
                        it.copy(currentExecution = execution)
                    }
                }
                .onFailure { exception ->
                    _uiState.update {
                        it.copy(error = "Checkliste konnte nicht abgeschlossen werden: ${exception.message}")
                    }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearImportResult() {
        _uiState.update { it.copy(importResult = null) }
    }

    fun clearSelectedChecklist() {
        _uiState.update { it.copy(selectedChecklist = null) }
    }

    fun clearCurrentExecution() {
        _uiState.update { it.copy(currentExecution = null) }
    }
}

data class ChecklistUiState(
    val isLoading: Boolean = false,
    val isImporting: Boolean = false,
    val isStarting: Boolean = false,
    val selectedChecklist: Checklist? = null,
    val currentExecution: ChecklistAusfuehrung? = null,
    val error: String? = null,
    val importResult: String? = null
)