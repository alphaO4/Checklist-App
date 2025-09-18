package com.feuerwehr.checklist.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.feuerwehr.checklist.data.auth.AuthRepository
import com.feuerwehr.checklist.data.models.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for authentication screen and user session management
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    // UI State
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // Authentication state from repository
    val isLoggedIn: StateFlow<Boolean> = authRepository.isLoggedIn()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    // Current user info
    val userInfo = authRepository.getUserInfo()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    init {
        // Try to get current user if logged in
        viewModelScope.launch {
            isLoggedIn.collect { loggedIn ->
                if (loggedIn && _uiState.value.currentUser == null) {
                    getCurrentUser()
                }
            }
        }
    }

    fun login(username: String, password: String) {
        if (_uiState.value.isLoading) return

        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            authRepository.login(username, password)
                .onSuccess { user ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            currentUser = user,
                            error = null
                        )
                    }
                }
                .onFailure { exception ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "Anmeldung fehlgeschlagen"
                        )
                    }
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _uiState.update { 
                AuthUiState() // Reset to initial state
            }
        }
    }

    fun getCurrentUser() {
        viewModelScope.launch {
            authRepository.getCurrentUser()
                .onSuccess { user ->
                    _uiState.update { it.copy(currentUser = user) }
                }
                .onFailure { exception ->
                    _uiState.update { 
                        it.copy(error = "Benutzerdaten nicht verfügbar: ${exception.message}")
                    }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun validateToken() {
        viewModelScope.launch {
            val isValid = authRepository.validateToken()
            if (!isValid) {
                // Token is invalid, logout
                logout()
            }
        }
    }
}

data class AuthUiState(
    val isLoading: Boolean = false,
    val currentUser: User? = null,
    val error: String? = null
) {
    val isAdmin: Boolean get() = currentUser?.rolle?.hasPermissionFor(com.feuerwehr.checklist.data.models.UserRole.ADMIN) == true
    val canEditChecklists: Boolean get() = currentUser?.rolle?.hasPermissionFor(com.feuerwehr.checklist.data.models.UserRole.ORGANISATOR) == true
}