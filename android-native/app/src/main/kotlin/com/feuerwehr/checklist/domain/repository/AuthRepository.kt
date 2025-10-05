package com.feuerwehr.checklist.domain.repository

import com.feuerwehr.checklist.domain.model.User

/**
 * Repository interface for Authentication operations
 */
interface AuthRepository {
    suspend fun login(username: String, password: String, rememberMe: Boolean = true): Result<User>
    suspend fun getCurrentUser(): Result<User>
    suspend fun logout(): Result<Unit>
    suspend fun autoLogin(): User?
    fun isLoggedIn(): Boolean
    suspend fun hasValidToken(): Boolean
    suspend fun isTokenExpired(): Boolean
    suspend fun refreshTokenIfNeeded(): Boolean
    fun getCurrentUserId(): Int?
    fun getCurrentUsername(): String?
}