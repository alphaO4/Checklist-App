package com.feuerwehr.checklist.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.feuerwehr.checklist.data.local.ChecklistDatabase
import com.feuerwehr.checklist.data.local.entity.*
import com.feuerwehr.checklist.data.remote.api.AuthApiService
import com.feuerwehr.checklist.data.error.RepositoryErrorHandler
import com.feuerwehr.checklist.domain.exception.AuthenticationException
import com.feuerwehr.checklist.domain.model.User
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

/**
 * Integration tests for AuthRepositoryImpl
 * Tests database integration and API interaction
 */
@RunWith(AndroidJUnit4::class)
class AuthRepositoryImplTest {

    private lateinit var database: ChecklistDatabase
    private lateinit var authApiService: AuthApiService
    private lateinit var errorHandler: RepositoryErrorHandler
    private lateinit var authRepository: AuthRepositoryImpl

    private val testUser = User(
        id = 1,
        username = "testuser",
        rolle = "Benutzer",
        createdAt = Clock.System.now()
    )

    private val testUserEntity = UserEntity(
        id = 1,
        username = "testuser",
        rolle = "Benutzer",
        createdAt = Clock.System.now(),
        syncStatus = SyncStatus.SYNCED,
        lastModified = Clock.System.now(),
        version = 1
    )

    @Before
    fun setup() {
        // Create in-memory database for testing
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ChecklistDatabase::class.java
        ).allowMainThreadQueries().build()

        // Mock API service
        authApiService = mockk()
        errorHandler = RepositoryErrorHandler()

        // Create repository instance
        authRepository = AuthRepositoryImpl(
            userDao = database.userDao(),
            authApi = authApiService,
            errorHandler = errorHandler
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `login with valid credentials should save user to database`() = runTest {
        // Given
        val username = "testuser"
        val password = "password123"
        val loginResponse = com.feuerwehr.checklist.data.remote.dto.LoginResponse(
            accessToken = "test_token",
            user = com.feuerwehr.checklist.data.remote.dto.UserDto(
                id = 1,
                username = "testuser",
                rolle = "Benutzer",
                createdAt = Clock.System.now().toString()
            )
        )

        coEvery { authApiService.login(any()) } returns loginResponse

        // When
        val result = authRepository.login(username, password)

        // Then
        assertTrue("Login should succeed", result.isSuccess)
        
        // Verify user is saved in database
        val savedUser = database.userDao().getUserById(1)
        assertNotNull("User should be saved in database", savedUser)
        assertEquals("Saved user should match", testUserEntity.username, savedUser?.username)
    }

    @Test
    fun `login with invalid credentials should return failure`() = runTest {
        // Given
        val username = "testuser"
        val password = "wrongpassword"
        val authException = AuthenticationException(
            message = "Invalid credentials",
            userMessage = "Benutzername oder Passwort ungültig",
            errorCode = "AUTH_INVALID_CREDENTIALS"
        )

        coEvery { authApiService.login(any()) } throws authException

        // When
        val result = authRepository.login(username, password)

        // Then
        assertTrue("Login should fail", result.isFailure)
        assertTrue("Should return AuthenticationException", 
            result.exceptionOrNull() is AuthenticationException)
        
        // Verify no user is saved in database
        val savedUser = database.userDao().getUserById(1)
        assertNull("No user should be saved on failed login", savedUser)
    }

    @Test
    fun `getCurrentUser should return user from database if exists`() = runTest {
        // Given - Save user in database first
        database.userDao().insertUser(testUserEntity)

        // When
        val result = authRepository.getCurrentUser()

        // Then
        assertNotNull("Should return user", result)
        assertEquals("Should return correct user", testUser.username, result?.username)
    }

    @Test
    fun `getCurrentUser should return null if no user in database`() = runTest {
        // Given - Empty database

        // When
        val result = authRepository.getCurrentUser()

        // Then
        assertNull("Should return null when no user exists", result)
    }

    @Test
    fun `logout should clear user from database`() = runTest {
        // Given - Save user in database first
        database.userDao().insertUser(testUserEntity)
        
        // Verify user exists
        val userBeforeLogout = database.userDao().getUserById(1)
        assertNotNull("User should exist before logout", userBeforeLogout)

        // When
        authRepository.logout()

        // Then
        val userAfterLogout = database.userDao().getUserById(1)
        assertNull("User should be removed after logout", userAfterLogout)
    }

    @Test
    fun `isLoggedIn should return true when user exists in database`() = runTest {
        // Given - Save user in database
        database.userDao().insertUser(testUserEntity)

        // When
        val result = authRepository.isLoggedIn()

        // Then
        assertTrue("Should return true when user exists", result)
    }

    @Test
    fun `isLoggedIn should return false when no user in database`() = runTest {
        // Given - Empty database

        // When
        val result = authRepository.isLoggedIn()

        // Then
        assertFalse("Should return false when no user exists", result)
    }

    @Test
    fun `login should handle German fire department roles correctly`() = runTest {
        // Test different German roles
        val testRoles = listOf(
            "Benutzer",
            "Gruppenleiter", 
            "Organisator",
            "Admin"
        )

        testRoles.forEachIndexed { index, rolle ->
            // Given
            val username = "user$index"
            val password = "password"
            val loginResponse = com.feuerwehr.checklist.data.remote.dto.LoginResponse(
                accessToken = "token_$index",
                user = com.feuerwehr.checklist.data.remote.dto.UserDto(
                    id = index + 1,
                    username = username,
                    rolle = rolle,
                    createdAt = Clock.System.now().toString()
                )
            )

            coEvery { authApiService.login(any()) } returns loginResponse

            // When
            val result = authRepository.login(username, password)

            // Then
            assertTrue("Login should succeed for role $rolle", result.isSuccess)
            assertEquals("Should return correct role", rolle, result.getOrNull()?.rolle)
            
            // Verify role is saved correctly in database
            val savedUser = database.userDao().getUserById(index + 1)
            assertEquals("Role should be saved correctly", rolle, savedUser?.rolle)
        }
    }

    @Test
    fun `login should handle special characters in username`() = runTest {
        // Given - Username with German characters
        val username = "müller.björn@feuerwehr"
        val password = "sicheresPasswort123!"
        val loginResponse = com.feuerwehr.checklist.data.remote.dto.LoginResponse(
            accessToken = "special_token",
            user = com.feuerwehr.checklist.data.remote.dto.UserDto(
                id = 1,
                username = username,
                rolle = "Gruppenleiter",
                createdAt = Clock.System.now().toString()
            )
        )

        coEvery { authApiService.login(any()) } returns loginResponse

        // When
        val result = authRepository.login(username, password)

        // Then
        assertTrue("Login should succeed with special characters", result.isSuccess)
        assertEquals("Should preserve special characters", username, result.getOrNull()?.username)
    }

    @Test
    fun `login should handle network errors gracefully`() = runTest {
        // Given
        val username = "testuser"
        val password = "password"
        val networkException = com.feuerwehr.checklist.domain.exception.NetworkException(
            message = "Network error",
            userMessage = "Netzwerkfehler - Überprüfen Sie Ihre Internetverbindung",
            errorCode = "NETWORK_IO_ERROR"
        )

        coEvery { authApiService.login(any()) } throws networkException

        // When
        val result = authRepository.login(username, password)

        // Then
        assertTrue("Should return failure on network error", result.isFailure)
        assertTrue("Should return NetworkException", 
            result.exceptionOrNull() is com.feuerwehr.checklist.domain.exception.NetworkException)
        
        // Verify German error message
        val exception = result.exceptionOrNull()!!
        assertTrue("Should have German error message", 
            exception.message?.contains("Netzwerkfehler") ?: false)
    }

    @Test
    fun `database operations should maintain data integrity`() = runTest {
        // Given - Multiple users with different roles
        val users = listOf(
            testUserEntity.copy(id = 1, username = "benutzer1", rolle = "Benutzer"),
            testUserEntity.copy(id = 2, username = "leiter1", rolle = "Gruppenleiter"),
            testUserEntity.copy(id = 3, username = "org1", rolle = "Organisator"),
            testUserEntity.copy(id = 4, username = "admin1", rolle = "Admin")
        )

        // When - Insert all users
        users.forEach { user ->
            database.userDao().insertUser(user)
        }

        // Then - Verify all users are saved correctly
        users.forEach { expectedUser ->
            val savedUser = database.userDao().getUserById(expectedUser.id)
            assertNotNull("User ${expectedUser.username} should be saved", savedUser)
            assertEquals("Username should match", expectedUser.username, savedUser?.username)
            assertEquals("Role should match", expectedUser.rolle, savedUser?.rolle)
            assertEquals("Sync status should be preserved", expectedUser.syncStatus, savedUser?.syncStatus)
        }
    }
}