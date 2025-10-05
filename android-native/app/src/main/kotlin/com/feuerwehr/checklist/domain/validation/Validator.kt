package com.feuerwehr.checklist.domain.validation

import com.feuerwehr.checklist.domain.exception.ValidationException
import java.util.regex.Pattern

/**
 * Validation utilities for form fields and user input
 * Provides German fire department specific validation rules
 */
object Validator {
    
    // Regex patterns
    private val EMAIL_PATTERN = Pattern.compile(
        "[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}" +
        "\\@" +
        "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
        "(" +
        "\\." +
        "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
        ")+"
    )
    
    private val GERMAN_LICENSE_PLATE_PATTERN = Pattern.compile(
        "^[A-ZÄÖÜ]{1,3}\\s?[A-Z]{1,2}\\s?\\d{1,4}[A-Z]?\$"
    )
    
    private val USERNAME_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9_]{3,20}\$"
    )
    
    /**
     * Validates required field
     */
    @Throws(ValidationException::class)
    fun validateRequired(value: String?, fieldName: String) {
        if (value.isNullOrBlank()) {
            throw ValidationException.RequiredFieldMissing(fieldName)
        }
    }
    
    /**
     * Validates string length
     */
    @Throws(ValidationException::class)
    fun validateLength(value: String, fieldName: String, minLength: Int = 0, maxLength: Int = Int.MAX_VALUE) {
        if (value.length < minLength) {
            throw ValidationException.ValueTooShort(fieldName, minLength)
        }
        if (value.length > maxLength) {
            throw ValidationException.ValueTooLong(fieldName, maxLength)
        }
    }
    
    /**
     * Validates email format
     */
    @Throws(ValidationException::class)
    fun validateEmail(email: String) {
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw ValidationException.InvalidEmail(email)
        }
    }
    
    /**
     * Validates username format
     */
    @Throws(ValidationException::class)
    fun validateUsername(username: String) {
        validateRequired(username, "Benutzername")
        validateLength(username, "Benutzername", 3, 20)
        
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw ValidationException.InvalidFormat(
                fieldName = "Benutzername",
                expectedFormat = "3-20 Zeichen, nur Buchstaben, Zahlen und Unterstriche"
            )
        }
    }
    
    /**
     * Validates password strength
     */
    @Throws(ValidationException::class)
    fun validatePassword(password: String) {
        validateRequired(password, "Passwort")
        validateLength(password, "Passwort", 8, 100)
        
        // Check for at least one digit, one letter
        val hasDigit = password.any { it.isDigit() }
        val hasLetter = password.any { it.isLetter() }
        
        if (!hasDigit || !hasLetter) {
            throw ValidationException.InvalidFormat(
                fieldName = "Passwort",
                expectedFormat = "mindestens 8 Zeichen mit Buchstaben und Zahlen"
            )
        }
    }
    
    /**
     * Validates German vehicle license plate (Kennzeichen)
     */
    @Throws(ValidationException::class)
    fun validateKennzeichen(kennzeichen: String) {
        validateRequired(kennzeichen, "Kennzeichen")
        
        val normalizedKennzeichen = kennzeichen.uppercase().replace("\\s".toRegex(), " ")
        
        if (!GERMAN_LICENSE_PLATE_PATTERN.matcher(normalizedKennzeichen).matches()) {
            throw ValidationException.InvalidFormat(
                fieldName = "Kennzeichen",
                expectedFormat = "deutsches Kfz-Kennzeichen (z.B. B AB 123)"
            )
        }
    }
    
    /**
     * Validates vehicle type name
     */
    @Throws(ValidationException::class)
    fun validateFahrzeugtyp(fahrzeugtyp: String) {
        validateRequired(fahrzeugtyp, "Fahrzeugtyp")
        validateLength(fahrzeugtyp, "Fahrzeugtyp", 2, 20)
        
        // Common German fire department vehicle types
        val validTypes = listOf("MTF", "LF", "DLK", "RTB", "RTW", "KTW", "GW", "TLF", "SW")
        val isValidType = validTypes.any { fahrzeugtyp.uppercase().startsWith(it) }
        
        if (!isValidType && !fahrzeugtyp.matches(Regex("^[A-Z]{2,6}\\s?\\d{0,2}\$"))) {
            throw ValidationException.InvalidFormat(
                fieldName = "Fahrzeugtyp",
                expectedFormat = "Feuerwehr-Fahrzeugtyp (z.B. MTF, LF 10, TLF 16/25)"
            )
        }
    }
    
    /**
     * Validates checklist name
     */
    @Throws(ValidationException::class)
    fun validateChecklistName(name: String) {
        validateRequired(name, "Checklisten-Name")
        validateLength(name, "Checklisten-Name", 3, 100)
        
        // Must not contain special characters that could cause issues
        if (name.contains(Regex("[<>\"'&]"))) {
            throw ValidationException.InvalidFormat(
                fieldName = "Checklisten-Name",
                expectedFormat = "keine Sonderzeichen < > \" ' &"
            )
        }
    }
    
    /**
     * Validates TÜV date (must be future date)
     */
    @Throws(ValidationException::class)
    fun validateTuvDate(tuvDate: kotlinx.datetime.LocalDate) {
        val today = kotlinx.datetime.Clock.System.todayIn(kotlinx.datetime.TimeZone.currentSystemDefault())
        
        if (tuvDate < today) {
            throw ValidationException.InvalidFormat(
                fieldName = "TÜV-Datum",
                expectedFormat = "Datum in der Zukunft"
            )
        }
        
        // TÜV should not be more than 3 years in the future
        val maxDate = today.plus(kotlinx.datetime.DatePeriod(years = 3))
        if (tuvDate > maxDate) {
            throw ValidationException.InvalidFormat(
                fieldName = "TÜV-Datum",
                expectedFormat = "maximal 3 Jahre in der Zukunft"
            )
        }
    }
    
    /**
     * Validates group name
     */
    @Throws(ValidationException::class)
    fun validateGroupName(name: String) {
        validateRequired(name, "Gruppen-Name")
        validateLength(name, "Gruppen-Name", 2, 50)
        
        // Should be reasonable fire department group name
        if (!name.matches(Regex("^[A-Za-zÄÖÜäöüß0-9\\s\\-/.]+\$"))) {
            throw ValidationException.InvalidFormat(
                fieldName = "Gruppen-Name",
                expectedFormat = "Buchstaben, Zahlen, Leerzeichen, Bindestriche und Punkte"
            )
        }
    }
}

/**
 * Result class for validation operations
 */
sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val exception: ValidationException) : ValidationResult()
}

/**
 * Extension function for safe validation
 */
fun <T> validate(operation: () -> T): ValidationResult {
    return try {
        operation()
        ValidationResult.Valid
    } catch (e: ValidationException) {
        ValidationResult.Invalid(e)
    }
}

/**
 * Extension function to validate multiple fields
 */
fun validateAll(vararg validations: () -> Unit): List<ValidationException> {
    val errors = mutableListOf<ValidationException>()
    
    for (validation in validations) {
        try {
            validation()
        } catch (e: ValidationException) {
            errors.add(e)
        }
    }
    
    return errors
}

/**
 * Extension function for form validation result
 */
data class FormValidationResult(
    val isValid: Boolean,
    val errors: Map<String, ValidationException>
) {
    fun getErrorMessage(field: String): String? {
        return errors[field]?.userMessage
    }
    
    fun hasError(field: String): Boolean {
        return errors.containsKey(field)
    }
}

/**
 * Validates a complete form with field mapping
 */
fun validateForm(validations: Map<String, () -> Unit>): FormValidationResult {
    val errors = mutableMapOf<String, ValidationException>()
    
    for ((field, validation) in validations) {
        try {
            validation()
        } catch (e: ValidationException) {
            errors[field] = e
        }
    }
    
    return FormValidationResult(
        isValid = errors.isEmpty(),
        errors = errors
    )
}