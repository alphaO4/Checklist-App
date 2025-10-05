package com.feuerwehr.checklist.data.error

import android.util.Log
import com.feuerwehr.checklist.domain.exception.*
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Repository-level error handler
 * Provides standardized error handling for data layer operations
 */
object RepositoryErrorHandler {
    
    /**
     * Executes a repository operation with comprehensive error handling
     */
    suspend fun <T> safeApiCall(
        tag: String,
        operation: suspend () -> T
    ): Result<T> {
        return try {
            val result = operation()
            Result.success(result)
        } catch (e: CancellationException) {
            // Don't catch cancellation exceptions - let them propagate
            throw e
        } catch (e: Throwable) {
            logError(tag, e)
            val checklistException = ErrorMapper.mapException(e)
            Result.failure(checklistException)
        }
    }
    
    /**
     * Executes a database operation with error handling
     */
    suspend fun <T> safeDatabaseCall(
        tag: String,
        operation: suspend () -> T
    ): Result<T> {
        return try {
            val result = operation()
            Result.success(result)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logError(tag, e)
            val checklistException = when (e) {
                is android.database.sqlite.SQLiteConstraintException -> 
                    DatabaseException.ConstraintViolation(e.message ?: "Constraint violation", e)
                is android.database.sqlite.SQLiteException -> 
                    DatabaseException.TransactionFailed(e)
                else -> ErrorMapper.mapException(e)
            }
            Result.failure(checklistException)
        }
    }
    
    /**
     * Executes a sync operation with specialized error handling
     */
    suspend fun <T> safeSyncCall(
        tag: String,
        operation: suspend () -> T
    ): Result<T> {
        return try {
            val result = operation()
            Result.success(result)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            logError(tag, e)
            val checklistException = when (e) {
                is UnknownHostException, is IOException -> 
                    NetworkException.NoInternetConnection()
                is SocketTimeoutException -> 
                    NetworkException.TimeoutException(e)
                is HttpException -> when (e.code()) {
                    409 -> SyncException.ConflictResolutionRequired(1)
                    else -> ErrorMapper.mapException(e)
                }
                else -> ErrorMapper.mapException(e)
            }
            Result.failure(checklistException)
        }
    }
    
    /**
     * Handles authentication-specific operations
     */
    suspend fun <T> safeAuthCall(
        tag: String,
        operation: suspend () -> T
    ): Result<T> {
        return try {
            val result = operation()
            Result.success(result)
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            logError(tag, e)
            val authException = when (e.code()) {
                401 -> AuthenticationException.InvalidCredentials()
                403 -> AuthenticationException.InsufficientPermissions("unknown")
                423 -> AuthenticationException.AccountLocked()
                else -> ErrorMapper.mapException(e)
            }
            Result.failure(authException)
        } catch (e: Throwable) {
            logError(tag, e)
            val checklistException = ErrorMapper.mapException(e)
            Result.failure(checklistException)
        }
    }
    
    /**
     * Handles validation operations
     */
    fun <T> safeValidation(
        tag: String,
        operation: () -> T
    ): Result<T> {
        return try {
            val result = operation()
            Result.success(result)
        } catch (e: IllegalArgumentException) {
            logError(tag, e)
            val validationException = ValidationException.InvalidFormat(
                fieldName = "unknown",
                expectedFormat = e.message ?: "valid format"
            )
            Result.failure(validationException)
        } catch (e: Exception) {
            logError(tag, e)
            val checklistException = ErrorMapper.mapException(e)
            Result.failure(checklistException)
        }
    }
    
    /**
     * Logs errors with structured information
     */
    private fun logError(tag: String, throwable: Throwable) {
        when (throwable) {
            is ChecklistException -> {
                Log.w(tag, "ChecklistException [${throwable.errorCode}]: ${throwable.message}", throwable)
            }
            is HttpException -> {
                Log.e(tag, "HTTP ${throwable.code()}: ${throwable.message()}", throwable)
            }
            is IOException -> {
                Log.w(tag, "Network error: ${throwable.message}", throwable)
            }
            else -> {
                Log.e(tag, "Unexpected error: ${throwable.message}", throwable)
            }
        }
    }
}

/**
 * Extension functions for cleaner usage in repositories
 */
suspend fun <T> safeApiCall(
    tag: String = "Repository",
    operation: suspend () -> T
): Result<T> = RepositoryErrorHandler.safeApiCall(tag, operation)

suspend fun <T> safeDatabaseCall(
    tag: String = "Repository",
    operation: suspend () -> T
): Result<T> = RepositoryErrorHandler.safeDatabaseCall(tag, operation)

suspend fun <T> safeSyncCall(
    tag: String = "Repository", 
    operation: suspend () -> T
): Result<T> = RepositoryErrorHandler.safeSyncCall(tag, operation)

suspend fun <T> safeAuthCall(
    tag: String = "Repository",
    operation: suspend () -> T
): Result<T> = RepositoryErrorHandler.safeAuthCall(tag, operation)

fun <T> safeValidation(
    tag: String = "Repository",
    operation: () -> T
): Result<T> = RepositoryErrorHandler.safeValidation(tag, operation)