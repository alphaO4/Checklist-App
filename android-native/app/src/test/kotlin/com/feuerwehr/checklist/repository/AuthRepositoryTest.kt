package com.feuerwehr.checklist.repository

import com.feuerwehr.checklist.data.local.storage.TokenStorage
import com.feuerwehr.checklist.data.local.storage.SecureStorage
import com.feuerwehr.checklist.data.remote.api.AuthApiService
import com.feuerwehr.checklist.data.remote.dto.LoginRequestDto
import com.feuerwehr.checklist.data.remote.dto.LoginResponseDto
import com.feuerwehr.checklist.data.remote.dto.UserDto
import com.feuerwehr.checklist.data.repository.AuthRepositoryImpl
import com.feuerwehr.checklist.domain.model.UserRole
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*

/**
 * Unit tests for AuthRepositoryImpl
 * Tests authentication, credential management, and token handling
 */
class AuthRepositoryTest {

    @Mock
    private lateinit var authApi: AuthApiService

    @Mock
    private lateinit var tokenStorage: TokenStorage

    @Mock
    private lateinit var secureStorage: SecureStorage

    private lateinit var repository: AuthRepositoryImpl

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        repository = AuthRepositoryImpl(authApi, tokenStorage, secureStorage)
    }

    @Test
    fun `login with valid credentials should succeed`() = runTest {
        val username = "admin"
        val password = "admin123"
        val rememberMe = true
        
        val mockUserDto = UserDto(
            id = 1,
            username = username,
            rolle = "admin",
            createdAt = "2025-09-19T10:00:00Z"
        )
        val mockResponse = LoginResponseDto(
            accessToken = "mock_jwt_token",
            tokenType = "bearer",
            user = mockUserDto
        )

        whenever(authApi.login(any<LoginRequestDto>())).thenReturn(mockResponse)

        val result = repository.login(username, password, rememberMe)

        assertTrue(result.isSuccess)
        val user = result.getOrNull()
        assertNotNull(user)
        assertEquals(username, user?.username)
        assertEquals(UserRole.ADMIN, user?.rolle)

        // Verify API call
        verify(authApi).login(LoginRequestDto(username, password))
        
        // Verify token storage
        verify(tokenStorage).saveToken("mock_jwt_token")
        verify(tokenStorage).saveUserInfo(1, username, "admin")
        
        // Verify credential storage when remember me is enabled
        verify(secureStorage).saveCredentials(username, password)
        verify(secureStorage).saveToken("mock_jwt_token")
    }

    @Test
    fun `login without remember me should not save credentials`() = runTest {
        val username = "admin"
        val password = "admin123"
        val rememberMe = false
        
        val mockUserDto = UserDto(
            id = 1,
            username = username,
            rolle = "admin",
            createdAt = "2025-09-19T10:00:00Z"
        )
        val mockResponse = LoginResponseDto(
            accessToken = "mock_jwt_token",
            tokenType = "bearer",
            user = mockUserDto
        )

        whenever(authApi.login(any<LoginRequestDto>())).thenReturn(mockResponse)

        val result = repository.login(username, password, rememberMe)

        assertTrue(result.isSuccess)

        // Verify credentials are NOT saved when remember me is false
        verify(secureStorage, never()).saveCredentials(any(), any())
        
        // But token should still be saved in secure storage
        verify(secureStorage).saveToken("mock_jwt_token")
    }

    @Test
    fun `login with invalid credentials should fail`() = runTest {
        val username = "invalid"
        val password = "invalid"
        val errorMessage = "Invalid credentials"

        whenever(authApi.login(any<LoginRequestDto>()))
            .thenThrow(RuntimeException(errorMessage))

        val result = repository.login(username, password, false)

        assertTrue(result.isFailure)
        assertEquals(errorMessage, result.exceptionOrNull()?.message)

        // Verify no tokens are saved on failure
        verify(tokenStorage, never()).saveToken(any())
        verify(secureStorage, never()).saveToken(any())
        verify(secureStorage, never()).saveCredentials(any(), any())
    }

    @Test
    fun `getCurrentUser should return user when token is valid`() = runTest {
        val mockUserDto = UserDto(
            id = 1,
            username = "admin",
            rolle = "admin",
            createdAt = "2025-09-19T10:00:00Z"
        )

        whenever(tokenStorage.hasValidToken()).thenReturn(true)
        whenever(authApi.getCurrentUser()).thenReturn(mockUserDto)

        val result = repository.getCurrentUser()

        assertTrue(result.isSuccess)
        val user = result.getOrNull()
        assertEquals("admin", user?.username)
        assertEquals(UserRole.ADMIN, user?.rolle)
    }

    @Test
    fun `getCurrentUser should fail when no valid token`() = runTest {
        whenever(tokenStorage.hasValidToken()).thenReturn(false)

        val result = repository.getCurrentUser()

        assertTrue(result.isFailure)
        assertEquals("No valid token", result.exceptionOrNull()?.message)
        
        // Verify API is not called when no token
        verify(authApi, never()).getCurrentUser()
    }

    @Test
    fun `logout should clear all storage`() = runTest {
        whenever(tokenStorage.hasValidToken()).thenReturn(true)

        val result = repository.logout()

        assertTrue(result.isSuccess)
        
        // Verify API logout call
        verify(authApi).logout()
        
        // Verify local storage is cleared
        verify(tokenStorage).clearAll()
        verify(secureStorage).clearCredentials()
    }

    @Test
    fun `logout should clear storage even if API call fails`() = runTest {
        whenever(tokenStorage.hasValidToken()).thenReturn(true)
        whenever(authApi.logout()).thenThrow(RuntimeException("Network error"))

        val result = repository.logout()

        // Should still succeed even if API fails
        assertTrue(result.isSuccess)
        
        // Storage should be cleared regardless of API failure
        verify(tokenStorage).clearAll()
        verify(secureStorage).clearCredentials()
    }

    @Test
    fun `isLoggedIn should delegate to token storage`() = runTest {
        whenever(tokenStorage.hasValidToken()).thenReturn(true)
        
        val result = repository.isLoggedIn()
        
        assertTrue(result)
        verify(tokenStorage).hasValidToken()
    }

    @Test
    fun `getCurrentUserId should return user ID from token storage`() = runTest {
        val mockUserId = 123
        whenever(tokenStorage.getUserId()).thenReturn(mockUserId)
        
        val result = repository.getCurrentUserId()
        
        assertEquals(mockUserId, result)
        verify(tokenStorage).getUserId()
    }

    @Test
    fun `getCurrentUserId should return null for invalid ID`() = runTest {
        whenever(tokenStorage.getUserId()).thenReturn(-1)
        
        val result = repository.getCurrentUserId()
        
        assertNull(result)
    }

    @Test
    fun `autoLogin should attempt login with stored credentials`() = runTest {
        val storedUsername = "admin"
        val storedPassword = "admin123"
        
        val mockUserDto = UserDto(
            id = 1,
            username = storedUsername,
            rolle = "admin",
            createdAt = "2025-09-19T10:00:00Z"
        )
        val mockResponse = LoginResponseDto(
            accessToken = "mock_jwt_token",
            tokenType = "bearer",
            user = mockUserDto
        )

        // Mock successful auto-login
        whenever(secureStorage.autoLogin<Any>(any())).thenAnswer { invocation ->
            val loginFunction = invocation.getArgument<suspend (String, String) -> Any?>(0)
            // Simulate calling the login function with stored credentials
            loginFunction(storedUsername, storedPassword)
        }
        
        whenever(authApi.login(any<LoginRequestDto>())).thenReturn(mockResponse)

        val result = repository.autoLogin()

        assertNotNull(result)
        assertEquals(storedUsername, result?.username)
        assertEquals(UserRole.ADMIN, result?.rolle)
    }

    @Test
    fun `autoLogin should return null when no stored credentials`() = runTest {
        whenever(secureStorage.autoLogin<Any>(any())).thenReturn(null)

        val result = repository.autoLogin()

        assertNull(result)
        
        // Verify no API call is made when no credentials
        verify(authApi, never()).login(any<LoginRequestDto>())
    }

    @Test
    fun `hasValidToken should delegate to secure storage`() = runTest {
        whenever(secureStorage.hasValidToken()).thenReturn(true)
        
        val result = repository.hasValidToken()
        
        assertTrue(result)
        verify(secureStorage).hasValidToken()
    }

    @Test
    fun `isTokenExpired should delegate to secure storage`() = runTest {
        whenever(secureStorage.isTokenExpired()).thenReturn(false)
        
        val result = repository.isTokenExpired()
        
        assertFalse(result)
        verify(secureStorage).isTokenExpired()
    }

    @Test
    fun `refreshTokenIfNeeded should auto-login when token expired`() = runTest {
        val mockUser = com.feuerwehr.checklist.domain.model.User(
            id = 1,
            username = "admin",
            email = "",
            rolle = UserRole.ADMIN,
            createdAt = kotlinx.datetime.Clock.System.now()
        )

        whenever(secureStorage.isTokenExpired()).thenReturn(true)
        whenever(secureStorage.autoLogin<Any>(any())).thenReturn(mockUser)

        val result = repository.refreshTokenIfNeeded()

        assertTrue(result)
        verify(secureStorage).isTokenExpired()
    }

    @Test
    fun `refreshTokenIfNeeded should return true when token not expired`() = runTest {
        whenever(secureStorage.isTokenExpired()).thenReturn(false)

        val result = repository.refreshTokenIfNeeded()

        assertTrue(result)
        
        // Auto-login should not be called when token is valid
        verify(secureStorage, never()).autoLogin<Any>(any())
    }
}