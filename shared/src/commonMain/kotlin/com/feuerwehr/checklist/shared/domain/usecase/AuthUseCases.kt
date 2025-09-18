package com.feuerwehr.checklist.shared.domain.usecase

import com.feuerwehr.checklist.shared.domain.model.User
import com.feuerwehr.checklist.shared.domain.repository.UserRepository

/**
 * Use case for user login
 * Encapsulates business logic for authentication
 */
class LoginUseCase(
    private val userRepository: UserRepository
) {
    
    suspend operator fun invoke(username: String, password: String): Result<User> {
        if (username.isBlank()) {
            return Result.failure(IllegalArgumentException("Benutzername darf nicht leer sein"))
        }
        
        if (password.isBlank()) {
            return Result.failure(IllegalArgumentException("Passwort darf nicht leer sein"))
        }
        
        return try {
            val result = userRepository.login(username.trim(), password)
            if (result.isSuccess) {
                result.getOrNull()?.let { user ->
                    // Store authentication state
                    // Token storage is handled by repository implementation
                    Result.success(user)
                } ?: Result.failure(Exception("Login erfolgreich, aber Benutzer ist null"))
            } else {
                result
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Use case for user logout
 */
class LogoutUseCase(
    private val userRepository: UserRepository
) {
    
    suspend operator fun invoke(): Result<Unit> {
        return try {
            userRepository.logout()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Use case to get current authenticated user
 */
class GetCurrentUserUseCase(
    private val userRepository: UserRepository
) {
    
    suspend operator fun invoke(): Result<User?> {
        return try {
            val user = userRepository.getCurrentUser()
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}