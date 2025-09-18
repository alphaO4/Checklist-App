package com.feuerwehr.checklist.data.repository

import com.feuerwehr.checklist.data.local.storage.TokenStorage
import com.feuerwehr.checklist.data.remote.api.AuthApiService
import com.feuerwehr.checklist.data.remote.dto.LoginRequestDto
import com.feuerwehr.checklist.domain.model.User
import com.feuerwehr.checklist.domain.repository.AuthRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Authentication repository implementation
 * Handles login/logout with JWT token management
 */

// Helper function to parse date strings from backend
// Handles both ISO format and German format (dd.MM.yyyy HH:mm:ss)
private fun parseBackendDate(dateString: String): Instant {
    return try {
        // Try ISO format first (2025-08-29T17:37:21)
        Instant.parse(dateString)
    } catch (e: Exception) {
        try {
            // Try German format (29.08.2025 17:37:21)
            val parts = dateString.split(" ")
            if (parts.size == 2) {
                val datePart = parts[0].split(".")
                val timePart = parts[1].split(":")
                if (datePart.size == 3 && timePart.size == 3) {
                    val day = datePart[0].toInt()
                    val month = datePart[1].toInt()
                    val year = datePart[2].toInt()
                    val hour = timePart[0].toInt()
                    val minute = timePart[1].toInt()
                    val second = timePart[2].toInt()
                    
                    LocalDateTime(year, month, day, hour, minute, second)
                        .toInstant(TimeZone.UTC)
                } else {
                    throw IllegalArgumentException("Invalid German date format: $dateString")
                }
            } else {
                throw IllegalArgumentException("Invalid German date format: $dateString")
            }
        } catch (e2: Exception) {
            // Fallback to current time if parsing fails
            Clock.System.now()
        }
    }
}
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApiService,
    private val tokenStorage: TokenStorage
) : AuthRepository {

    override suspend fun login(username: String, password: String): Result<User> {
        return try {
            val loginRequest = LoginRequestDto(username, password)
            val response = authApi.login(loginRequest)
            
            // Store token and user info
            tokenStorage.saveToken(response.accessToken)
            tokenStorage.saveUserInfo(
                userId = response.user.id,
                username = response.user.username,
                role = response.user.rolle
            )
            
            // Convert DTO to domain model
            val user = User(
                id = response.user.id,
                username = response.user.username,
                email = "", // TODO: Add email to backend user model
                rolle = com.feuerwehr.checklist.domain.model.UserRole.fromString(response.user.rolle),
                createdAt = parseBackendDate(response.user.createdAt)
            )
            
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCurrentUser(): Result<User> {
        return try {
            if (!tokenStorage.hasValidToken()) {
                return Result.failure(Exception("No valid token"))
            }
            
            val response = authApi.getCurrentUser()
            
            val user = User(
                id = response.id,
                username = response.username,
                email = "", // TODO: Add email to backend user model
                rolle = com.feuerwehr.checklist.domain.model.UserRole.fromString(response.rolle),
                createdAt = parseBackendDate(response.createdAt)
            )
            
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout(): Result<Unit> {
        return try {
            if (tokenStorage.hasValidToken()) {
                authApi.logout()
            }
            tokenStorage.clearAll()
            Result.success(Unit)
        } catch (e: Exception) {
            // Even if API call fails, clear local storage
            tokenStorage.clearAll()
            Result.success(Unit)
        }
    }

    override fun isLoggedIn(): Boolean {
        return tokenStorage.hasValidToken()
    }

    override fun getCurrentUserId(): Int? {
        val id = tokenStorage.getUserId()
        return if (id > 0) id else null
    }

    override fun getCurrentUsername(): String? {
        return tokenStorage.getUsername()
    }
}

