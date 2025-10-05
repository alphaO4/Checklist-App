package com.feuerwehr.checklist.domain.exception

import java.io.IOException
import kotlin.Exception

/**
 * Base exception class for the Fire Department Checklist application
 * Provides German error messages and error codes for proper handling
 */
sealed class ChecklistException(
    message: String,
    val errorCode: String,
    val userMessage: String,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * Network-related exceptions
 */
sealed class NetworkException(
    message: String,
    errorCode: String,
    userMessage: String,
    cause: Throwable? = null
) : ChecklistException(message, errorCode, userMessage, cause) {
    
    class NoInternetConnection : NetworkException(
        message = "No internet connection available",
        errorCode = "NETWORK_001",
        userMessage = "Keine Internetverbindung verfügbar. Bitte prüfen Sie Ihre Netzwerkeinstellungen."
    )
    
    class ServerUnreachable(cause: Throwable? = null) : NetworkException(
        message = "Cannot connect to server",
        errorCode = "NETWORK_002", 
        userMessage = "Server ist nicht erreichbar. Bitte versuchen Sie es später erneut.",
        cause = cause
    )
    
    class TimeoutException(cause: Throwable? = null) : NetworkException(
        message = "Request timeout",
        errorCode = "NETWORK_003",
        userMessage = "Anfrage ist abgelaufen. Bitte versuchen Sie es erneut.",
        cause = cause
    )
    
    class BadRequest(val httpCode: Int, cause: Throwable? = null) : NetworkException(
        message = "Bad request (HTTP $httpCode)",
        errorCode = "NETWORK_004",
        userMessage = "Ungültige Anfrage. Bitte überprüfen Sie Ihre Eingaben.",
        cause = cause
    )
    
    class ServerError(val httpCode: Int, cause: Throwable? = null) : NetworkException(
        message = "Server error (HTTP $httpCode)",
        errorCode = "NETWORK_005",
        userMessage = "Serverfehler. Bitte versuchen Sie es später erneut.",
        cause = cause
    )
}

/**
 * Authentication-related exceptions
 */
sealed class AuthenticationException(
    message: String,
    errorCode: String,
    userMessage: String,
    cause: Throwable? = null
) : ChecklistException(message, errorCode, userMessage, cause) {
    
    class InvalidCredentials : AuthenticationException(
        message = "Invalid username or password",
        errorCode = "AUTH_001",
        userMessage = "Benutzername oder Passwort ist ungültig."
    )
    
    class AccountLocked : AuthenticationException(
        message = "Account is locked",
        errorCode = "AUTH_002",
        userMessage = "Ihr Konto ist gesperrt. Bitte wenden Sie sich an den Administrator."
    )
    
    class SessionExpired : AuthenticationException(
        message = "Session has expired",
        errorCode = "AUTH_003",
        userMessage = "Ihre Sitzung ist abgelaufen. Bitte melden Sie sich erneut an."
    )
    
    class InsufficientPermissions(val requiredRole: String) : AuthenticationException(
        message = "Insufficient permissions for operation (requires $requiredRole)",
        errorCode = "AUTH_004",
        userMessage = "Sie haben nicht die erforderlichen Berechtigungen für diese Aktion."
    )
    
    class TokenExpired : AuthenticationException(
        message = "JWT token has expired",
        errorCode = "AUTH_005",
        userMessage = "Ihre Anmeldung ist abgelaufen. Bitte melden Sie sich erneut an."
    )
}

/**
 * Validation-related exceptions
 */
sealed class ValidationException(
    message: String,
    errorCode: String,
    userMessage: String,
    val field: String? = null,
    cause: Throwable? = null
) : ChecklistException(message, errorCode, userMessage, cause) {
    
    class RequiredFieldMissing(fieldName: String) : ValidationException(
        message = "Required field '$fieldName' is missing",
        errorCode = "VALIDATION_001",
        userMessage = "Das Feld '$fieldName' ist erforderlich.",
        field = fieldName
    )
    
    class InvalidFormat(fieldName: String, expectedFormat: String) : ValidationException(
        message = "Field '$fieldName' has invalid format (expected: $expectedFormat)",
        errorCode = "VALIDATION_002",
        userMessage = "Das Feld '$fieldName' hat ein ungültiges Format.",
        field = fieldName
    )
    
    class ValueTooLong(fieldName: String, maxLength: Int) : ValidationException(
        message = "Field '$fieldName' exceeds maximum length of $maxLength",
        errorCode = "VALIDATION_003",
        userMessage = "Das Feld '$fieldName' ist zu lang (maximal $maxLength Zeichen).",
        field = fieldName
    )
    
    class ValueTooShort(fieldName: String, minLength: Int) : ValidationException(
        message = "Field '$fieldName' is shorter than minimum length of $minLength",
        errorCode = "VALIDATION_004",
        userMessage = "Das Feld '$fieldName' ist zu kurz (mindestens $minLength Zeichen).",
        field = fieldName
    )
    
    class InvalidEmail(email: String) : ValidationException(
        message = "Invalid email format: $email",
        errorCode = "VALIDATION_005",
        userMessage = "Die E-Mail-Adresse hat ein ungültiges Format.",
        field = "email"
    )
    
    class DuplicateValue(fieldName: String, value: String) : ValidationException(
        message = "Duplicate value '$value' for field '$fieldName'",
        errorCode = "VALIDATION_006",
        userMessage = "Der Wert '$value' ist bereits vorhanden.",
        field = fieldName
    )
}

/**
 * Business logic-related exceptions
 */
sealed class BusinessLogicException(
    message: String,
    errorCode: String,
    userMessage: String,
    cause: Throwable? = null
) : ChecklistException(message, errorCode, userMessage, cause) {
    
    class VehicleNotFound(vehicleId: Int) : BusinessLogicException(
        message = "Vehicle with ID $vehicleId not found",
        errorCode = "BUSINESS_001",
        userMessage = "Das Fahrzeug wurde nicht gefunden."
    )
    
    class ChecklistNotFound(checklistId: Int) : BusinessLogicException(
        message = "Checklist with ID $checklistId not found",
        errorCode = "BUSINESS_002",
        userMessage = "Die Checkliste wurde nicht gefunden."
    )
    
    class ExecutionAlreadyExists(vehicleId: Int, checklistId: Int) : BusinessLogicException(
        message = "Active execution already exists for vehicle $vehicleId and checklist $checklistId",
        errorCode = "BUSINESS_003",
        userMessage = "Es existiert bereits eine aktive Überprüfung für dieses Fahrzeug und diese Checkliste."
    )
    
    class ExecutionNotFound(executionId: Int) : BusinessLogicException(
        message = "Execution with ID $executionId not found",
        errorCode = "BUSINESS_004",
        userMessage = "Die Überprüfung wurde nicht gefunden."
    )
    
    class TuvExpired(vehicleKennzeichen: String, expirationDate: String) : BusinessLogicException(
        message = "TÜV expired for vehicle $vehicleKennzeichen on $expirationDate",
        errorCode = "BUSINESS_005",
        userMessage = "Der TÜV für Fahrzeug $vehicleKennzeichen ist am $expirationDate abgelaufen."
    )
    
    class InvalidChecklistState(executionId: Int, currentState: String) : BusinessLogicException(
        message = "Invalid state transition for execution $executionId (current: $currentState)",
        errorCode = "BUSINESS_006",
        userMessage = "Die Überprüfung kann in diesem Zustand nicht bearbeitet werden."
    )
    
    class InsufficientVehiclePermissions(vehicleId: Int, userRole: String) : BusinessLogicException(
        message = "User with role $userRole cannot access vehicle $vehicleId",
        errorCode = "BUSINESS_007",
        userMessage = "Sie haben keine Berechtigung für dieses Fahrzeug."
    )
}

/**
 * Database-related exceptions
 */
sealed class DatabaseException(
    message: String,
    errorCode: String,
    userMessage: String,
    cause: Throwable? = null
) : ChecklistException(message, errorCode, userMessage, cause) {
    
    class ConstraintViolation(constraint: String, cause: Throwable? = null) : DatabaseException(
        message = "Database constraint violation: $constraint",
        errorCode = "DATABASE_001",
        userMessage = "Die Daten können aufgrund von Abhängigkeiten nicht gespeichert werden.",
        cause = cause
    )
    
    class DataCorruption(entity: String, cause: Throwable? = null) : DatabaseException(
        message = "Data corruption detected in $entity",
        errorCode = "DATABASE_002",
        userMessage = "Die Daten sind beschädigt. Bitte synchronisieren Sie die App.",
        cause = cause
    )
    
    class SyncConflict(entityType: String, entityId: String) : DatabaseException(
        message = "Sync conflict for $entityType with ID $entityId",
        errorCode = "DATABASE_003",
        userMessage = "Es gibt einen Synchronisierungskonflikt. Bitte lösen Sie ihn manuell auf."
    )
    
    class TransactionFailed(cause: Throwable? = null) : DatabaseException(
        message = "Database transaction failed",
        errorCode = "DATABASE_004",
        userMessage = "Die Datenbank-Operation konnte nicht abgeschlossen werden.",
        cause = cause
    )
}

/**
 * Sync-related exceptions
 */
sealed class SyncException(
    message: String,
    errorCode: String,
    userMessage: String,
    cause: Throwable? = null
) : ChecklistException(message, errorCode, userMessage, cause) {
    
    class SyncInProgress : SyncException(
        message = "Sync operation already in progress",
        errorCode = "SYNC_001",
        userMessage = "Eine Synchronisation läuft bereits."
    )
    
    class PartialSyncFailure(val failedEntities: List<String>) : SyncException(
        message = "Partial sync failure for entities: ${failedEntities.joinToString()}",
        errorCode = "SYNC_002",
        userMessage = "Einige Daten konnten nicht synchronisiert werden: ${failedEntities.joinToString(", ")}"
    )
    
    class ConflictResolutionRequired(val conflictCount: Int) : SyncException(
        message = "$conflictCount conflicts require manual resolution",
        errorCode = "SYNC_003",
        userMessage = "$conflictCount Konflikte erfordern eine manuelle Auflösung."
    )
    
    class BackgroundSyncDisabled : SyncException(
        message = "Background sync is disabled",
        errorCode = "SYNC_004", 
        userMessage = "Automatische Synchronisation ist deaktiviert."
    )
}