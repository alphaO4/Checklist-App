package com.feuerwehr.checklist.domain.exception

import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.net.ConnectException
import java.sql.SQLException
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteException

/**
 * Converts system exceptions to domain-specific ChecklistExceptions
 * Provides consistent error handling across the application
 */
object ErrorMapper {

    /**
     * Maps a generic throwable to a specific ChecklistException
     */
    fun mapException(throwable: Throwable): ChecklistException {
        return when (throwable) {
            // Already mapped exceptions
            is ChecklistException -> throwable
            
            // Network exceptions
            is UnknownHostException -> NetworkException.NoInternetConnection()
            is ConnectException -> NetworkException.ServerUnreachable(throwable)
            is SocketTimeoutException -> NetworkException.TimeoutException(throwable)
            is IOException -> mapIOException(throwable)
            
            // HTTP exceptions (Retrofit)
            is HttpException -> mapHttpException(throwable)
            
            // Database exceptions
            is SQLiteConstraintException -> DatabaseException.ConstraintViolation(
                constraint = throwable.message ?: "Unknown constraint",
                cause = throwable
            )
            is SQLiteException -> DatabaseException.TransactionFailed(throwable)
            is SQLException -> DatabaseException.TransactionFailed(throwable)
            
            // Authentication exceptions (JWT or auth-related)
            else -> mapByMessage(throwable)
        }
    }
    
    /**
     * Maps IOException to specific network exceptions
     */
    private fun mapIOException(exception: IOException): NetworkException {
        val message = exception.message?.lowercase() ?: ""
        return when {
            message.contains("timeout") -> NetworkException.TimeoutException(exception)
            message.contains("connection") -> NetworkException.ServerUnreachable(exception)
            message.contains("network") -> NetworkException.NoInternetConnection()
            else -> NetworkException.ServerUnreachable(exception)
        }
    }
    
    /**
     * Maps HTTP status codes to specific exceptions
     */
    private fun mapHttpException(exception: HttpException): ChecklistException {
        return when (exception.code()) {
            400 -> ValidationException.InvalidFormat("request", "valid JSON")
            401 -> AuthenticationException.InvalidCredentials()
            403 -> AuthenticationException.InsufficientPermissions("unknown")
            404 -> BusinessLogicException.ChecklistNotFound(-1) // Generic not found
            409 -> ValidationException.DuplicateValue("resource", "unknown")
            422 -> ValidationException.RequiredFieldMissing("unknown")
            500, 502, 503, 504 -> NetworkException.ServerError(exception.code(), exception)
            else -> NetworkException.BadRequest(exception.code(), exception)
        }
    }
    
    /**
     * Maps exceptions based on their message content
     */
    private fun mapByMessage(throwable: Throwable): ChecklistException {
        val message = throwable.message?.lowercase() ?: ""
        return when {
            // Authentication messages
            message.contains("unauthorized") || message.contains("401") -> 
                AuthenticationException.InvalidCredentials()
            message.contains("forbidden") || message.contains("403") -> 
                AuthenticationException.InsufficientPermissions("unknown")
            message.contains("token expired") || message.contains("jwt") -> 
                AuthenticationException.TokenExpired()
            message.contains("session expired") -> 
                AuthenticationException.SessionExpired()
            
            // Network messages
            message.contains("connection refused") -> 
                NetworkException.ServerUnreachable(throwable)
            message.contains("timeout") -> 
                NetworkException.TimeoutException(throwable)
            message.contains("network") -> 
                NetworkException.NoInternetConnection()
                
            // Validation messages
            message.contains("required") -> 
                ValidationException.RequiredFieldMissing("unknown")
            message.contains("invalid format") || message.contains("format") -> 
                ValidationException.InvalidFormat("unknown", "valid format")
            message.contains("duplicate") || message.contains("already exists") -> 
                ValidationException.DuplicateValue("unknown", "unknown")
            
            // Business logic messages
            message.contains("not found") -> 
                BusinessLogicException.ChecklistNotFound(-1)
            message.contains("already active") || message.contains("execution exists") -> 
                BusinessLogicException.ExecutionAlreadyExists(-1, -1)
            message.contains("tüv") && message.contains("expired") -> 
                BusinessLogicException.TuvExpired("unknown", "unknown")
                
            // Sync messages
            message.contains("sync") && message.contains("progress") -> 
                SyncException.SyncInProgress()
            message.contains("conflict") -> 
                DatabaseException.SyncConflict("unknown", "unknown")
                
            // Default fallback
            else -> ChecklistException(
                message = throwable.message ?: "Unknown error",
                errorCode = "UNKNOWN_001",
                userMessage = "Ein unbekannter Fehler ist aufgetreten. Bitte versuchen Sie es erneut.",
                cause = throwable
            ) {
                // Anonymous inner class to make sealed class instantiable
            }
        }
    }
    
    /**
     * Maps specific API error responses to exceptions
     */
    fun mapApiError(errorCode: String?, errorMessage: String?): ChecklistException {
        return when (errorCode) {
            "INVALID_CREDENTIALS" -> AuthenticationException.InvalidCredentials()
            "ACCOUNT_LOCKED" -> AuthenticationException.AccountLocked()
            "SESSION_EXPIRED" -> AuthenticationException.SessionExpired()
            "INSUFFICIENT_PERMISSIONS" -> AuthenticationException.InsufficientPermissions("unknown")
            "TOKEN_EXPIRED" -> AuthenticationException.TokenExpired()
            
            "VEHICLE_NOT_FOUND" -> BusinessLogicException.VehicleNotFound(-1)
            "CHECKLIST_NOT_FOUND" -> BusinessLogicException.ChecklistNotFound(-1)
            "EXECUTION_EXISTS" -> BusinessLogicException.ExecutionAlreadyExists(-1, -1)
            "EXECUTION_NOT_FOUND" -> BusinessLogicException.ExecutionNotFound(-1)
            "TUV_EXPIRED" -> BusinessLogicException.TuvExpired("unknown", "unknown")
            
            "REQUIRED_FIELD_MISSING" -> ValidationException.RequiredFieldMissing("unknown")
            "INVALID_FORMAT" -> ValidationException.InvalidFormat("unknown", "valid format")
            "DUPLICATE_VALUE" -> ValidationException.DuplicateValue("unknown", "unknown")
            
            "SYNC_IN_PROGRESS" -> SyncException.SyncInProgress()
            "SYNC_CONFLICT" -> DatabaseException.SyncConflict("unknown", "unknown")
            
            else -> ChecklistException(
                message = errorMessage ?: "API error",
                errorCode = errorCode ?: "API_ERROR",
                userMessage = errorMessage ?: "Ein Fehler ist aufgetreten.",
                cause = null
            ) {
                // Anonymous inner class
            }
        }
    }
}

/**
 * Extension function to safely map exceptions
 */
fun Throwable.toChecklistException(): ChecklistException {
    return ErrorMapper.mapException(this)
}

/**
 * Extension function for Result types
 */
fun <T> Result<T>.mapError(): Result<T> {
    return this.onFailure { throwable ->
        throw ErrorMapper.mapException(throwable)
    }
}