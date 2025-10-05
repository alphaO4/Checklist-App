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
 * ViewModel for login functionality
 * Handles authentication with enhanced error handling
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: com.feuerwehr.checklist.domain.repository.AuthRepository
) : BaseErrorHandlingViewModel() {
    
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()
    
    private var lastLoginAttempt: (() -> Unit)? = null
    
    fun login(username: String, password: String, rememberMe: Boolean = true) {
        lastLoginAttempt = { login(username, password, rememberMe) }
        
        safeExecuteResult(
            operation = {
                _uiState.value = _uiState.value.copy(isLoading = true)
                clearError()
                authRepository.login(username, password, rememberMe)
            },
            onSuccess = { user ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoginSuccess = true
                )
            }
        ) { error ->
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun tryAutoLogin() {
        safeExecute(
            operation = {
                val user = authRepository.autoLogin()
                if (user != null) {
                    _uiState.value = _uiState.value.copy(
                        isLoginSuccess = true
                    )
                }
            }
        ) { error ->
            // Silently fail auto-login, user can login manually
            // Don't show error for auto-login failures
        }
    }
    
    override fun retryLastOperation() {
        lastLoginAttempt?.invoke()
    }
}

data class LoginUiState(
    val isLoading: Boolean = false,
    val isLoginSuccess: Boolean = false
)