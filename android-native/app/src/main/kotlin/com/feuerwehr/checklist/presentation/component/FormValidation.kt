package com.feuerwehr.checklist.presentation.component

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Form validation utilities and validated input components
 * Provides consistent validation patterns across the app
 */

/**
 * Validation result for form fields
 */
sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val message: String) : ValidationResult()
}

/**
 * Common validation functions for German fire department context
 */
object ValidationRules {
    
    fun validateUsername(username: String): ValidationResult {
        return when {
            username.isBlank() -> ValidationResult.Invalid("Benutzername ist erforderlich")
            username.length < 3 -> ValidationResult.Invalid("Benutzername muss mindestens 3 Zeichen lang sein")
            username.length > 50 -> ValidationResult.Invalid("Benutzername darf maximal 50 Zeichen lang sein")
            !username.matches(Regex("^[a-zA-Z0-9._-]+$")) -> ValidationResult.Invalid("Benutzername darf nur Buchstaben, Zahlen und ._- enthalten")
            else -> ValidationResult.Valid
        }
    }
    
    fun validatePassword(password: String): ValidationResult {
        return when {
            password.isBlank() -> ValidationResult.Invalid("Passwort ist erforderlich")
            password.length < 6 -> ValidationResult.Invalid("Passwort muss mindestens 6 Zeichen lang sein")
            password.length > 128 -> ValidationResult.Invalid("Passwort darf maximal 128 Zeichen lang sein")
            else -> ValidationResult.Valid
        }
    }
    
    fun validateKennzeichen(kennzeichen: String): ValidationResult {
        return when {
            kennzeichen.isBlank() -> ValidationResult.Invalid("Kennzeichen ist erforderlich")
            kennzeichen.length < 2 -> ValidationResult.Invalid("Kennzeichen zu kurz")
            kennzeichen.length > 20 -> ValidationResult.Invalid("Kennzeichen zu lang")
            !kennzeichen.matches(Regex("^[A-Z0-9-]+$")) -> ValidationResult.Invalid("Kennzeichen darf nur Großbuchstaben, Zahlen und Bindestriche enthalten")
            else -> ValidationResult.Valid
        }
    }
    
    fun validateBaujahr(baujahr: String): ValidationResult {
        if (baujahr.isBlank()) return ValidationResult.Valid // Optional field
        
        return when {
            !baujahr.matches(Regex("^[0-9]+$")) -> ValidationResult.Invalid("Baujahr muss eine Zahl sein")
            else -> {
                val jahr = baujahr.toIntOrNull()
                when {
                    jahr == null -> ValidationResult.Invalid("Ungültiges Baujahr")
                    jahr < 1950 -> ValidationResult.Invalid("Baujahr muss nach 1950 liegen")
                    jahr > 2030 -> ValidationResult.Invalid("Baujahr kann nicht in der Zukunft liegen")
                    else -> ValidationResult.Valid
                }
            }
        }
    }
    
    fun validateChecklistName(name: String): ValidationResult {
        return when {
            name.isBlank() -> ValidationResult.Invalid("Name der Checkliste ist erforderlich")
            name.length < 3 -> ValidationResult.Invalid("Name muss mindestens 3 Zeichen lang sein")
            name.length > 100 -> ValidationResult.Invalid("Name darf maximal 100 Zeichen lang sein")
            else -> ValidationResult.Valid
        }
    }
    
    fun validateChecklistDescription(description: String): ValidationResult {
        return when {
            description.length > 500 -> ValidationResult.Invalid("Beschreibung darf maximal 500 Zeichen lang sein")
            else -> ValidationResult.Valid
        }
    }
    
    fun validateItemName(name: String): ValidationResult {
        return when {
            name.isBlank() -> ValidationResult.Invalid("Item-Name ist erforderlich")
            name.length < 2 -> ValidationResult.Invalid("Item-Name muss mindestens 2 Zeichen lang sein")
            name.length > 200 -> ValidationResult.Invalid("Item-Name darf maximal 200 Zeichen lang sein")
            else -> ValidationResult.Valid
        }
    }
    
    fun validateMenge(menge: String): ValidationResult {
        if (menge.isBlank()) return ValidationResult.Valid // Optional field
        
        return when {
            !menge.matches(Regex("^[0-9]+$")) -> ValidationResult.Invalid("Menge muss eine Zahl sein")
            else -> {
                val mengeInt = menge.toIntOrNull()
                when {
                    mengeInt == null -> ValidationResult.Invalid("Ungültige Menge")
                    mengeInt < 0 -> ValidationResult.Invalid("Menge kann nicht negativ sein")
                    mengeInt > 99999 -> ValidationResult.Invalid("Menge zu groß")
                    else -> ValidationResult.Valid
                }
            }
        }
    }
    
    fun validateKommentar(kommentar: String): ValidationResult {
        return when {
            kommentar.length > 1000 -> ValidationResult.Invalid("Kommentar darf maximal 1000 Zeichen lang sein")
            else -> ValidationResult.Valid
        }
    }
}

/**
 * Validated text field that shows validation errors
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ValidatedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    validator: (String) -> ValidationResult = { ValidationResult.Valid },
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    placeholder: String? = null,
    supportingText: String? = null
) {
    val validationResult = validator(value)
    val isError = validationResult is ValidationResult.Invalid
    
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier,
        isError = isError,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = singleLine,
        enabled = enabled,
        placeholder = placeholder?.let { { Text(it) } },
        supportingText = {
            when {
                isError -> Text((validationResult as ValidationResult.Invalid).message)
                supportingText != null -> Text(supportingText)
                else -> null
            }
        }
    )
}

/**
 * State class for managing form validation
 */
class FormValidationState {
    private val fieldStates = mutableMapOf<String, ValidationResult>()
    
    fun setFieldValidation(fieldName: String, result: ValidationResult) {
        fieldStates[fieldName] = result
    }
    
    fun getFieldValidation(fieldName: String): ValidationResult {
        return fieldStates[fieldName] ?: ValidationResult.Valid
    }
    
    fun isFormValid(): Boolean {
        return fieldStates.values.all { it is ValidationResult.Valid }
    }
    
    fun getErrorMessage(fieldName: String): String? {
        return (fieldStates[fieldName] as? ValidationResult.Invalid)?.message
    }
    
    fun hasErrors(): Boolean {
        return fieldStates.values.any { it is ValidationResult.Invalid }
    }
    
    fun clearField(fieldName: String) {
        fieldStates.remove(fieldName)
    }
    
    fun clearAll() {
        fieldStates.clear()
    }
}

/**
 * Composable function to create and remember a form validation state
 */
@Composable
fun rememberFormValidationState(): FormValidationState {
    return remember { FormValidationState() }
}

/**
 * Validated username field specifically for login/registration
 */
@Composable
fun UsernameField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String = "Benutzername"
) {
    ValidatedTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        validator = ValidationRules::validateUsername,
        keyboardType = KeyboardType.Text,
        enabled = enabled,
        placeholder = "z.B. max.mustermann"
    )
}

/**
 * Validated password field
 */
@Composable
fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String = "Passwort"
) {
    ValidatedTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        validator = ValidationRules::validatePassword,
        isPassword = true,
        keyboardType = KeyboardType.Password,
        enabled = enabled
    )
}

/**
 * Validated Kennzeichen field for vehicle license plates
 */
@Composable
fun KennzeichenField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String = "Kennzeichen"
) {
    ValidatedTextField(
        value = value.uppercase(), // Auto-uppercase for German license plates
        onValueChange = { onValueChange(it.uppercase()) },
        label = label,
        modifier = modifier,
        validator = ValidationRules::validateKennzeichen,
        keyboardType = KeyboardType.Text,
        enabled = enabled,
        placeholder = "z.B. B-2184"
    )
}

/**
 * Validated Baujahr field for vehicle manufacture year
 */
@Composable
fun BaujahrField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String = "Baujahr (optional)"
) {
    ValidatedTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        validator = ValidationRules::validateBaujahr,
        keyboardType = KeyboardType.Number,
        enabled = enabled,
        placeholder = "z.B. 2020"
    )
}

/**
 * Validated checklist name field
 */
@Composable
fun ChecklistNameField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String = "Checklist-Name"
) {
    ValidatedTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        validator = ValidationRules::validateChecklistName,
        keyboardType = KeyboardType.Text,
        enabled = enabled,
        placeholder = "z.B. TLF Wöchentliche Prüfung"
    )
}

/**
 * Validated menge (quantity) field for checklist items
 */
@Composable
fun MengeField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String = "Menge (optional)"
) {
    ValidatedTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        validator = ValidationRules::validateMenge,
        keyboardType = KeyboardType.Number,
        enabled = enabled,
        placeholder = "z.B. 5"
    )
}

/**
 * Validated comment field
 */
@Composable
fun KommentarField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String = "Kommentar (optional)",
    maxLines: Int = 3
) {
    ValidatedTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        validator = ValidationRules::validateKommentar,
        keyboardType = KeyboardType.Text,
        singleLine = false,
        enabled = enabled,
        placeholder = "Zusätzliche Bemerkungen..."
    )
}