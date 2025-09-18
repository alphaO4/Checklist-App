package com.feuerwehr.checklist.shared.domain.repository

import com.feuerwehr.checklist.shared.domain.model.User
import com.feuerwehr.checklist.shared.domain.model.UserRole
import kotlinx.coroutines.flow.Flow

/**
 * Shared repository interface for User operations
 * Platform-specific implementations will handle local/remote data sources
 */
interface UserRepository {
    
    suspend fun login(username: String, password: String): Result<User>
    suspend fun logout()
    
    fun getCurrentUserFlow(): Flow<User?>
    suspend fun getCurrentUser(): User?
    
    suspend fun getUserById(id: Int): Result<User>
    suspend fun getUserByUsername(username: String): Result<User>
    
    fun getAllUsersFlow(): Flow<List<User>>
    fun getUsersByRoleFlow(role: UserRole): Flow<List<User>>
    
    suspend fun updateUser(user: User): Result<User>
    suspend fun deleteUser(userId: Int): Result<Unit>
    
    // Authentication state
    suspend fun isLoggedIn(): Boolean
    suspend fun getStoredToken(): String?
    suspend fun storeToken(token: String)
    suspend fun clearToken()
}