package com.feuerwehr.checklist.data.auth

import com.feuerwehr.checklist.data.database.UserDao
import com.feuerwehr.checklist.data.models.*
import com.feuerwehr.checklist.data.network.AuthInterceptor
import com.feuerwehr.checklist.data.network.ChecklistApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for authentication operations
 * Handles login, token management, and user data synchronization
 */
@Singleton
class AuthRepository @Inject constructor(
    private val apiService: ChecklistApiService,
    private val tokenStorage: SecureTokenStorage,
    private val userDao: UserDao,
    private val authInterceptor: AuthInterceptor
) {

    init {
        // Setup token provider for auth interceptor
        authInterceptor.setTokenProvider {
            kotlinx.coroutines.runBlocking {
                tokenStorage.getToken()
            }
        }
    }

    /**
     * Login with username and password
     */
    suspend fun login(username: String, password: String): Result<User> {
        return try {
            val response = apiService.login(LoginRequest(username, password))
            
            if (response.isSuccessful) {
                val loginResponse = response.body()!!
                
                // Store token securely
                tokenStorage.storeToken(
                    token = loginResponse.accessToken,
                    userId = loginResponse.user.id.toString(),
                    username = loginResponse.user.username
                )
                
                // Convert and store user in local database
                val user = loginResponse.user.toDomainModel()
                val userEntity = user.toEntity()
                userDao.insertUser(userEntity)
                
                Result.success(user)
            } else {
                val errorMessage = when (response.code()) {
                    401 -> "Ungültige Anmeldedaten"
                    403 -> "Zugriff verweigert"
                    404 -> "Benutzer nicht gefunden"
                    500 -> "Server-Fehler"
                    else -> "Anmeldung fehlgeschlagen: ${response.message()}"
                }
                Result.failure(AuthException(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(AuthException("Netzwerkfehler: ${e.message}"))
        }
    }

    /**
     * Logout user and clear all auth data
     */
    suspend fun logout(): Result<Unit> {
        return try {
            // Call logout endpoint if we have a token
            val token = tokenStorage.getToken()
            if (token != null) {
                apiService.logout()
            }
            
            // Clear local data
            tokenStorage.clearAuthData()
            userDao.clearUsers()
            
            Result.success(Unit)
        } catch (e: Exception) {
            // Even if API call fails, clear local data
            tokenStorage.clearAuthData()
            userDao.clearUsers()
            Result.success(Unit)
        }
    }

    /**
     * Get current user from API and update local cache
     */
    suspend fun getCurrentUser(): Result<User> {
        return try {
            val response = apiService.getCurrentUser()
            
            if (response.isSuccessful) {
                val userDto = response.body()!!
                val user = userDto.toDomainModel()
                
                // Update local cache
                val userEntity = user.toEntity()
                userDao.insertUser(userEntity)
                
                Result.success(user)
            } else {
                // Try to get user from local cache if API fails
                val userId = tokenStorage.getUserId()?.toIntOrNull()
                if (userId != null) {
                    val cachedUser = userDao.getUserById(userId)?.toDomainModel()
                    if (cachedUser != null) {
                        Result.success(cachedUser)
                    } else {
                        Result.failure(AuthException("Benutzer nicht gefunden"))
                    }
                } else {
                    Result.failure(AuthException("Nicht angemeldet"))
                }
            }
        } catch (e: Exception) {
            // Fallback to cached user
            val userId = tokenStorage.getUserId()?.toIntOrNull()
            if (userId != null) {
                val cachedUser = userDao.getUserById(userId)?.toDomainModel()
                if (cachedUser != null) {
                    Result.success(cachedUser)
                } else {
                    Result.failure(AuthException("Offline: Benutzer nicht im Cache"))
                }
            } else {
                Result.failure(AuthException("Netzwerkfehler: ${e.message}"))
            }
        }
    }

    /**
     * Check if user is currently logged in
     */
    fun isLoggedIn(): Flow<Boolean> {
        return tokenStorage.isLoggedIn()
    }

    /**
     * Get user info from token storage
     */
    fun getUserInfo(): Flow<AuthUserInfo?> {
        return tokenStorage.getUserInfo()
    }

    /**
     * Validate current token by making an API call
     */
    suspend fun validateToken(): Boolean {
        return try {
            val response = apiService.getCurrentUser()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Refresh user data from server
     */
    suspend fun refreshUserData(): Result<User> {
        return getCurrentUser()
    }
}

/**
 * Custom exception for authentication errors
 */
class AuthException(message: String) : Exception(message)

/**
 * Extension functions for data mapping
 */
private fun UserDto.toDomainModel(): User {
    return User(
        id = id,
        username = username,
        email = email,
        rolle = UserRole.fromString(rolle),
        createdAt = Instant.parse(createdAt),
        gruppeId = gruppeId,
        gruppe = gruppe?.toDomainModel()
    )
}

private fun GroupDto.toDomainModel(): Group {
    return Group(
        id = id,
        name = name,
        gruppenleiterId = gruppenleiterId,
        createdAt = Instant.parse(createdAt)
    )
}

private fun User.toEntity(): UserEntity {
    return UserEntity(
        id = id,
        username = username,
        email = email,
        rolle = rolle.value,
        createdAt = createdAt.toString(),
        gruppeId = gruppeId,
        lastSyncedAt = Clock.System.now().toEpochMilliseconds()
    )
}

private fun UserEntity.toDomainModel(): User {
    return User(
        id = id,
        username = username,
        email = email,
        rolle = UserRole.fromString(rolle),
        createdAt = Instant.parse(createdAt),
        gruppeId = gruppeId
    )
}