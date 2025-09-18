package com.feuerwehr.checklist.domain.repository

import com.feuerwehr.checklist.domain.model.User

/**
 * Repository interface for Authentication operations
 */
interface AuthRepository {
    suspend fun login(username: String, password: String): Result<User>
    suspend fun getCurrentUser(): Result<User>
    suspend fun logout(): Result<Unit>
    fun isLoggedIn(): Boolean
    fun getCurrentUserId(): Int?
    fun getCurrentUsername(): String?
}