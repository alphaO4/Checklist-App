package com.feuerwehr.checklist.presentation.error

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.feuerwehr.checklist.domain.exception.*
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

/**
 * Base error handling for ViewModels
 * Provides standardized error management and user feedback
 */
abstract class BaseErrorHandlingViewModel : ViewModel() {
    
    private val _errorState = MutableStateFlow<ErrorState?>(null)
    val errorState: StateFlow<ErrorState?> = _errorState.asStateFlow()
    
    /**
     * Coroutine exception handler for the ViewModel
     */
    protected val errorHandler = CoroutineExceptionHandler { _, throwable ->
        handleError(throwable)
    }
    
    /**
     * Handles errors and updates error state
     */
    protected fun handleError(throwable: Throwable) {
        val checklistException = ErrorMapper.mapException(throwable)
        
        _errorState.value = ErrorState(
            exception = checklistException,
            userMessage = checklistException.userMessage,
            errorCode = checklistException.errorCode,
            isRetryable = isRetryable(checklistException),
            timestamp = System.currentTimeMillis()
        )
        
        // Log error for debugging
        logError(checklistException)
    }
    
    /**
     * Clears the current error state
     */
    fun clearError() {
        _errorState.value = null
    }
    
    /**
     * Executes a suspending operation with error handling
     */
    protected fun safeExecute(
        operation: suspend () -> Unit,
        onError: ((ChecklistException) -> Unit)? = null
    ) {
        viewModelScope.launch(errorHandler) {
            try {
                operation()
            } catch (e: Throwable) {
                val checklistException = ErrorMapper.mapException(e)
                onError?.invoke(checklistException) ?: handleError(checklistException)
            }
        }
    }
    
    /**
     * Executes an operation that returns a Result with error handling
     */
    protected fun <T> safeExecuteResult(
        operation: suspend () -> Result<T>,
        onSuccess: (T) -> Unit,
        onError: ((ChecklistException) -> Unit)? = null
    ) {
        viewModelScope.launch(errorHandler) {
            try {
                operation().fold(
                    onSuccess = onSuccess,
                    onFailure = { throwable ->
                        val checklistException = ErrorMapper.mapException(throwable)
                        onError?.invoke(checklistException) ?: handleError(checklistException)
                    }
                )
            } catch (e: Throwable) {
                val checklistException = ErrorMapper.mapException(e)
                onError?.invoke(checklistException) ?: handleError(checklistException)
            }
        }
    }
    
    /**
     * Retries the last failed operation if retryable
     */
    open fun retryLastOperation() {
        // Override in subclasses to implement retry logic
    }
    
    /**
     * Determines if an exception is retryable
     */
    private fun isRetryable(exception: ChecklistException): Boolean {
        return when (exception) {
            is NetworkException.TimeoutException,
            is NetworkException.ServerUnreachable,
            is NetworkException.ServerError -> true
            
            is DatabaseException.TransactionFailed -> true
            
            is SyncException.PartialSyncFailure -> true
            
            else -> false
        }
    }
    
    /**
     * Logs errors for debugging
     */
    private fun logError(exception: ChecklistException) {
        android.util.Log.e(
            "ErrorHandler",
            "Error [${exception.errorCode}]: ${exception.message}",
            exception
        )
    }
}

/**
 * Error state data class
 */
data class ErrorState(
    val exception: ChecklistException,
    val userMessage: String,
    val errorCode: String,
    val isRetryable: Boolean,
    val timestamp: Long
)

/**
 * Error severity levels for UI display
 */
enum class ErrorSeverity {
    INFO,       // Informational messages
    WARNING,    // Non-critical issues
    ERROR,      // Critical errors requiring user attention
    CRITICAL    // System errors requiring immediate action
}

/**
 * Extension functions for error categorization
 */
fun ChecklistException.getSeverity(): ErrorSeverity {
    return when (this) {
        is NetworkException.NoInternetConnection -> ErrorSeverity.WARNING
        is NetworkException.TimeoutException -> ErrorSeverity.WARNING
        
        is AuthenticationException.SessionExpired -> ErrorSeverity.INFO
        is AuthenticationException.InvalidCredentials -> ErrorSeverity.ERROR
        is AuthenticationException.InsufficientPermissions -> ErrorSeverity.ERROR
        
        is ValidationException -> ErrorSeverity.ERROR
        
        is BusinessLogicException -> ErrorSeverity.ERROR
        
        is DatabaseException.SyncConflict -> ErrorSeverity.WARNING
        is DatabaseException.ConstraintViolation -> ErrorSeverity.ERROR
        is DatabaseException.DataCorruption -> ErrorSeverity.CRITICAL
        
        is SyncException.ConflictResolutionRequired -> ErrorSeverity.WARNING
        is SyncException.PartialSyncFailure -> ErrorSeverity.WARNING
        
        else -> ErrorSeverity.ERROR
    }
}

/**
 * Extension function to get appropriate icon for error type
 */
fun ChecklistException.getIconResource(): String {
    return when (this) {
        is NetworkException -> "network_error"
        is AuthenticationException -> "security"
        is ValidationException -> "warning"
        is BusinessLogicException -> "error"
        is DatabaseException -> "storage"
        is SyncException -> "sync_problem"
        else -> "error"
    }
}

/**
 * Extension function to check if error requires immediate user action
 */
fun ChecklistException.requiresImmediateAction(): Boolean {
    return when (this) {
        is AuthenticationException.SessionExpired -> true
        is DatabaseException.DataCorruption -> true
        is SyncException.ConflictResolutionRequired -> true
        is BusinessLogicException.TuvExpired -> true
        else -> false
    }
}