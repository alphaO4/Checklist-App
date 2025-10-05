package com.feuerwehr.checklist.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.feuerwehr.checklist.domain.repository.AuthRepository
import com.feuerwehr.checklist.domain.model.User
import com.feuerwehr.checklist.domain.model.UserRole
import com.feuerwehr.checklist.presentation.viewmodel.LoginViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Clock
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import org.mockito.kotlin.verify
import org.mockito.kotlin.any

/**
 * Unit tests for LoginViewModel
 * Tests authentication flow, UI state management, and error handling
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var authRepository: AuthRepository

    private lateinit var viewModel: LoginViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        viewModel = LoginViewModel(authRepository)
    }

    @Test
    fun `initial state should be correct`() {
        val initialState = viewModel.uiState.value
        
        assertFalse(initialState.isLoading)
        assertTrue(initialState.username.isEmpty())
        assertTrue(initialState.password.isEmpty())
        assertFalse(initialState.rememberMe)
        assertNull(initialState.errorMessage)
    }

    @Test
    fun `updateUsername should update state correctly`() = runTest {
        val testUsername = "testUser"
        
        viewModel.updateUsername(testUsername)
        testScheduler.advanceUntilIdle()
        
        assertEquals(testUsername, viewModel.uiState.value.username)
    }

    @Test
    fun `updatePassword should update state correctly`() = runTest {
        val testPassword = "testPassword"
        
        viewModel.updatePassword(testPassword)
        testScheduler.advanceUntilIdle()
        
        assertEquals(testPassword, viewModel.uiState.value.password)
    }

    @Test
    fun `toggleRememberMe should update state correctly`() = runTest {
        val initialRememberMe = viewModel.uiState.value.rememberMe
        
        viewModel.toggleRememberMe()
        testScheduler.advanceUntilIdle()
        
        assertEquals(!initialRememberMe, viewModel.uiState.value.rememberMe)
    }

    @Test
    fun `login with valid credentials should succeed`() = runTest {
        val username = "admin"
        val password = "admin"
        val testUser = User(
            id = 1,
            username = username,
            email = "admin@test.com",
            rolle = UserRole.ADMIN,
            createdAt = Clock.System.now()
        )
        
        whenever(authRepository.login(username, password, false))
            .thenReturn(Result.success(testUser))
        
        viewModel.updateUsername(username)
        viewModel.updatePassword(password)
        viewModel.login()
        
        testScheduler.advanceUntilIdle()
        
        verify(authRepository).login(username, password, false)
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `login with invalid credentials should show error`() = runTest {
        val username = "invalid"
        val password = "invalid"
        val errorMessage = "Invalid credentials"
        
        whenever(authRepository.login(username, password, false))
            .thenReturn(Result.failure(Exception(errorMessage)))
        
        viewModel.updateUsername(username)
        viewModel.updatePassword(password)
        viewModel.login()
        
        testScheduler.advanceUntilIdle()
        
        verify(authRepository).login(username, password, false)
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(errorMessage, viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `login with remember me should pass correct parameter`() = runTest {
        val username = "admin"
        val password = "admin"
        val testUser = User(
            id = 1,
            username = username,
            email = "admin@test.com",
            rolle = UserRole.ADMIN,
            createdAt = Clock.System.now()
        )
        
        whenever(authRepository.login(username, password, true))
            .thenReturn(Result.success(testUser))
        
        viewModel.updateUsername(username)
        viewModel.updatePassword(password)
        viewModel.toggleRememberMe() // Enable remember me
        viewModel.login()
        
        testScheduler.advanceUntilIdle()
        
        verify(authRepository).login(username, password, true)
    }

    @Test
    fun `clearError should remove error message`() = runTest {
        // First set an error
        val errorMessage = "Test error"
        whenever(authRepository.login(any(), any(), any()))
            .thenReturn(Result.failure(Exception(errorMessage)))
        
        viewModel.updateUsername("test")
        viewModel.updatePassword("test")
        viewModel.login()
        testScheduler.advanceUntilIdle()
        
        assertEquals(errorMessage, viewModel.uiState.value.errorMessage)
        
        // Then clear the error
        viewModel.clearError()
        testScheduler.advanceUntilIdle()
        
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `login should set loading state correctly`() = runTest {
        val username = "admin"
        val password = "admin"
        
        whenever(authRepository.login(username, password, false))
            .thenReturn(Result.success(User(1, username, "", UserRole.ADMIN, Clock.System.now())))
        
        viewModel.updateUsername(username)
        viewModel.updatePassword(password)
        
        // Start login
        viewModel.login()
        
        // Should be loading initially
        assertTrue(viewModel.uiState.value.isLoading)
        
        // Complete the coroutine
        testScheduler.advanceUntilIdle()
        
        // Should not be loading after completion
        assertFalse(viewModel.uiState.value.isLoading)
    }
}