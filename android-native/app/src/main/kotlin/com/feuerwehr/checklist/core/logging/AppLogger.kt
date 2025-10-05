package com.feuerwehr.checklist.core.logging

import android.util.Log
import com.feuerwehr.checklist.domain.exception.ChecklistException
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Centralized logging system for the Checklist application
 * Provides structured logging with different levels and contexts
 */
object AppLogger {
    
    private const val APP_TAG = "FeuerwehrChecklist"
    
    // Log levels
    enum class Level(val priority: Int) {
        VERBOSE(Log.VERBOSE),
        DEBUG(Log.DEBUG),
        INFO(Log.INFO),
        WARN(Log.WARN),
        ERROR(Log.ERROR)
    }
    
    // Log contexts for filtering
    enum class Context(val tag: String) {
        AUTH("Auth"),
        SYNC("Sync"),
        DATABASE("Database"),
        NETWORK("Network"),
        UI("UI"),
        VALIDATION("Validation"),
        BUSINESS("Business"),
        SYSTEM("System")
    }
    
    /**
     * Log a message with context
     */
    fun log(
        level: Level,
        context: Context,
        message: String,
        throwable: Throwable? = null,
        data: Map<String, Any>? = null
    ) {
        val tag = "${APP_TAG}_${context.tag}"
        val formattedMessage = formatMessage(message, data)
        
        when (level) {
            Level.VERBOSE -> Log.v(tag, formattedMessage, throwable)
            Level.DEBUG -> Log.d(tag, formattedMessage, throwable)
            Level.INFO -> Log.i(tag, formattedMessage, throwable)
            Level.WARN -> Log.w(tag, formattedMessage, throwable)
            Level.ERROR -> Log.e(tag, formattedMessage, throwable)
        }
    }
    
    /**
     * Log verbose messages
     */
    fun v(context: Context, message: String, data: Map<String, Any>? = null) {
        log(Level.VERBOSE, context, message, null, data)
    }
    
    /**
     * Log debug messages
     */
    fun d(context: Context, message: String, data: Map<String, Any>? = null) {
        log(Level.DEBUG, context, message, null, data)
    }
    
    /**
     * Log info messages
     */
    fun i(context: Context, message: String, data: Map<String, Any>? = null) {
        log(Level.INFO, context, message, null, data)
    }
    
    /**
     * Log warning messages
     */
    fun w(context: Context, message: String, throwable: Throwable? = null, data: Map<String, Any>? = null) {
        log(Level.WARN, context, message, throwable, data)
    }
    
    /**
     * Log error messages
     */
    fun e(context: Context, message: String, throwable: Throwable? = null, data: Map<String, Any>? = null) {
        log(Level.ERROR, context, message, throwable, data)
    }
    
    /**
     * Log ChecklistException with structured data
     */
    fun logException(exception: ChecklistException, context: Context? = null) {
        val logContext = context ?: when (exception) {
            is com.feuerwehr.checklist.domain.exception.AuthenticationException -> Context.AUTH
            is com.feuerwehr.checklist.domain.exception.NetworkException -> Context.NETWORK
            is com.feuerwehr.checklist.domain.exception.DatabaseException -> Context.DATABASE
            is com.feuerwehr.checklist.domain.exception.ValidationException -> Context.VALIDATION
            is com.feuerwehr.checklist.domain.exception.SyncException -> Context.SYNC
            is com.feuerwehr.checklist.domain.exception.BusinessLogicException -> Context.BUSINESS
            else -> Context.SYSTEM
        }
        
        val data = mapOf(
            "errorCode" to exception.errorCode,
            "userMessage" to exception.userMessage,
            "timestamp" to Clock.System.now().toString()
        )
        
        e(logContext, exception.message ?: "Unknown error", exception, data)
    }
    
    /**
     * Log network operations
     */
    fun logNetworkCall(
        method: String,
        url: String,
        statusCode: Int? = null,
        duration: Long? = null,
        error: Throwable? = null
    ) {
        val data = mutableMapOf<String, Any>(
            "method" to method,
            "url" to url
        )
        
        statusCode?.let { data["statusCode"] = it }
        duration?.let { data["duration"] = "${it}ms" }
        
        if (error != null) {
            e(Context.NETWORK, "Network call failed: $method $url", error, data)
        } else {
            i(Context.NETWORK, "Network call: $method $url", data)
        }
    }
    
    /**
     * Log authentication events
     */
    fun logAuth(event: String, username: String? = null, success: Boolean? = null, error: Throwable? = null) {
        val data = mutableMapOf<String, Any>("event" to event)
        username?.let { data["username"] = it }
        success?.let { data["success"] = it }
        
        val message = "Auth event: $event"
        
        if (error != null) {
            e(Context.AUTH, message, error, data)
        } else {
            i(Context.AUTH, message, data)
        }
    }
    
    /**
     * Log sync operations
     */
    fun logSync(
        operation: String,
        entityType: String? = null,
        count: Int? = null,
        conflicts: Int? = null,
        success: Boolean? = null,
        error: Throwable? = null
    ) {
        val data = mutableMapOf<String, Any>("operation" to operation)
        entityType?.let { data["entityType"] = it }
        count?.let { data["count"] = it }
        conflicts?.let { data["conflicts"] = it }
        success?.let { data["success"] = it }
        
        val message = "Sync operation: $operation"
        
        if (error != null) {
            e(Context.SYNC, message, error, data)
        } else {
            i(Context.SYNC, message, data)
        }
    }
    
    /**
     * Log database operations
     */
    fun logDatabase(
        operation: String,
        table: String? = null,
        recordId: String? = null,
        success: Boolean? = null,
        error: Throwable? = null
    ) {
        val data = mutableMapOf<String, Any>("operation" to operation)
        table?.let { data["table"] = it }
        recordId?.let { data["recordId"] = it }
        success?.let { data["success"] = it }
        
        val message = "Database operation: $operation"
        
        if (error != null) {
            e(Context.DATABASE, message, error, data)
        } else {
            d(Context.DATABASE, message, data)
        }
    }
    
    /**
     * Log user actions for analytics
     */
    fun logUserAction(
        action: String,
        screen: String? = null,
        data: Map<String, Any>? = null
    ) {
        val actionData = mutableMapOf<String, Any>("action" to action)
        screen?.let { actionData["screen"] = it }
        data?.let { actionData.putAll(it) }
        
        i(Context.UI, "User action: $action", actionData)
    }
    
    /**
     * Log validation errors
     */
    fun logValidation(
        field: String,
        value: String?,
        error: String,
        context: String? = null
    ) {
        val data = mapOf(
            "field" to field,
            "value" to (value ?: "null"),
            "error" to error,
            "context" to (context ?: "unknown")
        )
        
        w(Context.VALIDATION, "Validation failed for field: $field", null, data)
    }
    
    /**
     * Format message with additional data
     */
    private fun formatMessage(message: String, data: Map<String, Any>?): String {
        if (data.isNullOrEmpty()) return message
        
        val dataString = data.map { "${it.key}=${it.value}" }.joinToString(", ")
        return "$message [$dataString]"
    }
    
    /**
     * Get stack trace as string
     */
    private fun getStackTrace(throwable: Throwable): String {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        throwable.printStackTrace(pw)
        return sw.toString()
    }
}

/**
 * Extension functions for easier logging from different components
 */

// ViewModel extensions
fun androidx.lifecycle.ViewModel.logUserAction(action: String, data: Map<String, Any>? = null) {
    AppLogger.logUserAction(action, this::class.simpleName, data)
}

fun androidx.lifecycle.ViewModel.logError(message: String, error: Throwable) {
    AppLogger.e(AppLogger.Context.UI, message, error)
}

// Repository extensions  
fun Any.logNetworkCall(method: String, url: String, statusCode: Int? = null, duration: Long? = null, error: Throwable? = null) {
    AppLogger.logNetworkCall(method, url, statusCode, duration, error)
}

fun Any.logDatabaseOperation(operation: String, table: String? = null, recordId: String? = null, success: Boolean? = null, error: Throwable? = null) {
    AppLogger.logDatabase(operation, table, recordId, success, error)
}

// Worker extensions
fun androidx.work.Worker.logSync(operation: String, entityType: String? = null, count: Int? = null, conflicts: Int? = null, success: Boolean? = null, error: Throwable? = null) {
    AppLogger.logSync(operation, entityType, count, conflicts, success, error)
}

// General extension for any class
fun Any.logInfo(context: AppLogger.Context, message: String, data: Map<String, Any>? = null) {
    AppLogger.i(context, message, data)
}

fun Any.logWarning(context: AppLogger.Context, message: String, error: Throwable? = null, data: Map<String, Any>? = null) {
    AppLogger.w(context, message, error, data)
}

fun Any.logError(context: AppLogger.Context, message: String, error: Throwable? = null, data: Map<String, Any>? = null) {
    AppLogger.e(context, message, error, data)
}