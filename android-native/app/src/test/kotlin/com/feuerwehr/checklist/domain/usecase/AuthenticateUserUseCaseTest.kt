package com.feuerwehr.checklist.domain.usecase

import com.feuerwehr.checklist.domain.model.User
import com.feuerwehr.checklist.domain.repository.AuthRepository
import com.feuerwehr.checklist.domain.exception.AuthenticationException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for AuthenticateUserUseCase
 * Tests authentication logic and error handling
 */
class AuthenticateUserUseCaseTest {

    private lateinit var authRepository: AuthRepository
    private lateinit var authenticateUserUseCase: AuthenticateUserUseCase

    private val testUser = User(
        id = 1,
        username = "testuser",
        rolle = "Benutzer",
        createdAt = Clock.System.now()
    )

    @Before
    fun setup() {
        authRepository = mockk()
        authenticateUserUseCase = AuthenticateUserUseCase(authRepository)
    }

    @Test
    fun `authenticate with valid credentials should return success`() = runTest {
        // Given
        val username = "testuser"
        val password = "password123"
        coEvery { authRepository.login(username, password) } returns Result.success(testUser)

        // When
        val result = authenticateUserUseCase(username, password)

        // Then
        assertTrue("Should return success", result.isSuccess)
        assertEquals("Should return correct user", testUser, result.getOrNull())
        coVerify { authRepository.login(username, password) }
    }

    @Test
    fun `authenticate with invalid credentials should return failure`() = runTest {
        // Given
        val username = "testuser"
        val password = "wrongpassword"
        val authException = AuthenticationException(
            message = "Invalid credentials",
            userMessage = "Benutzername oder Passwort ungültig",
            errorCode = "AUTH_INVALID_CREDENTIALS"
        )
        coEvery { authRepository.login(username, password) } returns Result.failure(authException)

        // When
        val result = authenticateUserUseCase(username, password)

        // Then
        assertTrue("Should return failure", result.isFailure)
        assertTrue("Should return AuthenticationException", result.exceptionOrNull() is AuthenticationException)
        coVerify { authRepository.login(username, password) }
    }

    @Test
    fun `authenticate with empty username should return failure`() = runTest {
        // Given
        val username = ""
        val password = "password123"
        val validationException = com.feuerwehr.checklist.domain.exception.ValidationException(
            message = "Username cannot be empty",
            userMessage = "Benutzername darf nicht leer sein",
            field = "username",
            errorCode = "VALIDATION_EMPTY_USERNAME"
        )
        coEvery { authRepository.login(username, password) } returns Result.failure(validationException)

        // When
        val result = authenticateUserUseCase(username, password)

        // Then
        assertTrue("Should return failure", result.isFailure)
        coVerify { authRepository.login(username, password) }
    }

    @Test
    fun `authenticate with empty password should return failure`() = runTest {
        // Given
        val username = "testuser"
        val password = ""
        val validationException = com.feuerwehr.checklist.domain.exception.ValidationException(
            message = "Password cannot be empty",
            userMessage = "Passwort darf nicht leer sein",
            field = "password",
            errorCode = "VALIDATION_EMPTY_PASSWORD"
        )
        coEvery { authRepository.login(username, password) } returns Result.failure(validationException)

        // When
        val result = authenticateUserUseCase(username, password)

        // Then
        assertTrue("Should return failure", result.isFailure)
        coVerify { authRepository.login(username, password) }
    }

    @Test
    fun `authenticate with network error should return failure`() = runTest {
        // Given
        val username = "testuser"
        val password = "password123"
        val networkException = com.feuerwehr.checklist.domain.exception.NetworkException(
            message = "Network connection failed",
            userMessage = "Netzwerkverbindung fehlgeschlagen",
            errorCode = "NETWORK_CONNECTION_FAILED"
        )
        coEvery { authRepository.login(username, password) } returns Result.failure(networkException)

        // When
        val result = authenticateUserUseCase(username, password)

        // Then
        assertTrue("Should return failure", result.isFailure)
        assertTrue("Should return NetworkException", result.exceptionOrNull() is com.feuerwehr.checklist.domain.exception.NetworkException)
        coVerify { authRepository.login(username, password) }
    }

    @Test
    fun `authenticate with special characters in password should work`() = runTest {
        // Given
        val username = "testuser"
        val password = "p@ssw0rd!#$"
        coEvery { authRepository.login(username, password) } returns Result.success(testUser)

        // When
        val result = authenticateUserUseCase(username, password)

        // Then
        assertTrue("Should return success", result.isSuccess)
        assertEquals("Should return correct user", testUser, result.getOrNull())
        coVerify { authRepository.login(username, password) }
    }

    @Test
    fun `authenticate with different user roles should work`() = runTest {
        // Given
        val username = "admin"
        val password = "adminpass"
        val adminUser = testUser.copy(
            id = 2,
            username = "admin",
            rolle = "Admin"
        )
        coEvery { authRepository.login(username, password) } returns Result.success(adminUser)

        // When
        val result = authenticateUserUseCase(username, password)

        // Then
        assertTrue("Should return success", result.isSuccess)
        assertEquals("Should return admin user", adminUser, result.getOrNull())
        assertEquals("Should have admin role", "Admin", result.getOrNull()?.rolle)
        coVerify { authRepository.login(username, password) }
    }
}