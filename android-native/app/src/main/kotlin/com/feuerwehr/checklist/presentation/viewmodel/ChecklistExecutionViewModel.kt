package com.feuerwehr.checklist.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.feuerwehr.checklist.domain.model.*
import com.feuerwehr.checklist.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Checklist Execution screen
 * Manages checklist execution flow and item results
 */
@HiltViewModel
class ChecklistExecutionViewModel @Inject constructor(
    private val startChecklistExecutionUseCase: StartChecklistExecutionUseCase,
    private val completeExecutionUseCase: CompleteExecutionUseCase,
    private val submitItemResultUseCase: SubmitItemResultUseCase,
    private val getChecklistByIdUseCase: GetChecklistByIdUseCase,
    private val checklistRepository: com.feuerwehr.checklist.domain.repository.ChecklistRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChecklistExecutionUiState())
    val uiState: StateFlow<ChecklistExecutionUiState> = _uiState.asStateFlow()

    fun startExecution(checklistId: Int, vehicleId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Get the checklist with items
                val checklist = getChecklistByIdUseCase(checklistId)
                if (checklist == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Checkliste nicht gefunden"
                    )
                    return@launch
                }

                // Start the execution
                startChecklistExecutionUseCase(checklistId, vehicleId).fold(
                    onSuccess = { execution ->
                        // Load checklist items
                        checklistRepository.getChecklistItems(checklistId).collect { items ->
                            _uiState.value = _uiState.value.copy(
                                checklist = checklist,
                                execution = execution,
                                checklistItems = items,
                                currentItemIndex = 0,
                                isLoading = false
                            )
                        }
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = error.message
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun nextItem() {
        val currentState = _uiState.value
        if (currentState.currentItemIndex < currentState.checklistItems.size - 1) {
            _uiState.value = currentState.copy(
                currentItemIndex = currentState.currentItemIndex + 1
            )
        }
    }

    fun previousItem() {
        val currentState = _uiState.value
        if (currentState.currentItemIndex > 0) {
            _uiState.value = currentState.copy(
                currentItemIndex = currentState.currentItemIndex - 1
            )
        }
    }

    fun submitItemResult(
        itemId: Int,
        status: ItemStatus,
        wert: Map<String, Any>? = null,
        vorhanden: Boolean? = null,
        tuvDatum: kotlinx.datetime.LocalDate? = null,
        tuvStatus: String? = null,
        menge: Int? = null,
        kommentar: String? = null
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, error = null)
            
            val execution = _uiState.value.execution
            if (execution == null) {
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    error = "Keine aktive Ausführung"
                )
                return@launch
            }

            val result = ItemResult(
                id = 0, // Will be assigned by backend
                ausfuehrungId = execution.id,
                itemId = itemId,
                status = status,
                wert = wert,
                vorhanden = vorhanden,
                tuvDatum = tuvDatum,
                tuvStatus = tuvStatus,
                menge = menge,
                kommentar = kommentar,
                createdAt = kotlinx.datetime.Clock.System.now()
            )

            submitItemResultUseCase(result).fold(
                onSuccess = { submittedResult ->
                    // Add result to current state
                    val updatedResults = _uiState.value.itemResults + submittedResult
                    _uiState.value = _uiState.value.copy(
                        itemResults = updatedResults,
                        isSubmitting = false
                    )
                    
                    // Automatically move to next item
                    nextItem()
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        error = error.message
                    )
                }
            )
        }
    }

    fun completeExecution() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCompleting = true, error = null)
            
            val execution = _uiState.value.execution
            if (execution == null) {
                _uiState.value = _uiState.value.copy(
                    isCompleting = false,
                    error = "Keine aktive Ausführung"
                )
                return@launch
            }

            completeExecutionUseCase(execution.id).fold(
                onSuccess = { completedExecution ->
                    _uiState.value = _uiState.value.copy(
                        execution = completedExecution,
                        isCompleting = false,
                        isCompleted = true
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isCompleting = false,
                        error = error.message
                    )
                }
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun getCurrentItem(): ChecklistItem? {
        val state = _uiState.value
        return if (state.currentItemIndex < state.checklistItems.size) {
            state.checklistItems[state.currentItemIndex]
        } else null
    }

    fun getItemResult(itemId: Int): ItemResult? {
        return _uiState.value.itemResults.find { it.itemId == itemId }
    }

    fun isLastItem(): Boolean {
        val state = _uiState.value
        return state.currentItemIndex >= state.checklistItems.size - 1
    }
}

data class ChecklistExecutionUiState(
    val checklist: Checklist? = null,
    val execution: ChecklistExecution? = null,
    val checklistItems: List<ChecklistItem> = emptyList(),
    val itemResults: List<ItemResult> = emptyList(),
    val currentItemIndex: Int = 0,
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val isCompleting: Boolean = false,
    val isCompleted: Boolean = false,
    val error: String? = null
)