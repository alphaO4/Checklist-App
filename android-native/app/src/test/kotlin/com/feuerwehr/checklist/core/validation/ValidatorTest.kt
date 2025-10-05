package com.feuerwehr.checklist.core.validation

import com.feuerwehr.checklist.domain.exception.ValidationException
import org.junit.Test
import org.junit.Assert.*
import kotlinx.datetime.LocalDate

/**
 * Unit tests for Validator
 * Tests German fire department specific validation rules
 */
class ValidatorTest {

    @Test
    fun `validateKennzeichen should accept valid German fire department plates`() {
        val validKennzeichen = listOf(
            "B-FW 1234",     // Berlin
            "M-FW 5678",     // Munich
            "HH-FW 9012",    // Hamburg
            "K-FW 3456",     // Cologne
            "S-FW 7890",     // Stuttgart
            "F-FW 1111",     // Frankfurt
            "D-FW 2222",     // Düsseldorf
            "H-FW 3333",     // Hannover
            "BR-FW 4444",    // Brandenburg
            "KS-FW 5555"     // Kassel
        )

        validKennzeichen.forEach { kennzeichen ->
            val result = Validator.validateKennzeichen(kennzeichen)
            assertTrue("$kennzeichen should be valid", result.isSuccess)
        }
    }

    @Test
    fun `validateKennzeichen should reject invalid formats`() {
        val invalidKennzeichen = listOf(
            "",                    // Empty
            "B-1234",             // Missing FW
            "B-FW-1234",          // Wrong separator
            "B FW 1234",          // Missing dash
            "B-FW 12345",         // Too many numbers
            "B-FW 12",            // Too few numbers
            "BB-FW 1234",         // Too many city letters for most cases
            "1-FW 1234",          // Number as city code
            "B-fw 1234",          // Lowercase
            "B-FW1234",           // Missing space
            "B-XX 1234"           // Wrong department code
        )

        invalidKennzeichen.forEach { kennzeichen ->
            val result = Validator.validateKennzeichen(kennzeichen)
            assertTrue("$kennzeichen should be invalid", result.isFailure)
            assertTrue("Should return ValidationException", 
                result.exceptionOrNull() is ValidationException)
        }
    }

    @Test
    fun `validateFahrzeugtyp should accept valid fire department vehicle types`() {
        val validFahrzeugtypen = listOf(
            "LHF",          // Löschgruppenfahrzeug
            "TLF",          // Tanklöschfahrzeug
            "DLK",          // Drehleiter
            "MTF",          // Mannschaftstransportfahrzeug
            "RTB",          // Rüstwagen
            "GW-L",         // Gerätewagen Logistik
            "ELW",          // Einsatzleitwagen
            "TSF",          // Tragkraftspritzenfahrzeug
            "LF",           // Löschfahrzeug
            "HLF"           // Hilfeleistungslöschfahrzeug
        )

        validFahrzeugtypen.forEach { typ ->
            val result = Validator.validateFahrzeugtyp(typ)
            assertTrue("$typ should be valid", result.isSuccess)
        }
    }

    @Test
    fun `validateFahrzeugtyp should reject invalid vehicle types`() {
        val invalidFahrzeugtypen = listOf(
            "",             // Empty
            "ABC",          // Unknown type
            "lhf",          // Lowercase
            "LHF-123",      // With numbers
            "L H F",        // With spaces
            "PKW",          // Regular car
            "LKW",          // Regular truck
            "123",          // Only numbers
            "!@#"           // Special characters
        )

        invalidFahrzeugtypen.forEach { typ ->
            val result = Validator.validateFahrzeugtyp(typ)
            assertTrue("$typ should be invalid", result.isFailure)
            assertTrue("Should return ValidationException", 
                result.exceptionOrNull() is ValidationException)
        }
    }

    @Test
    fun `validateTuvDate should accept future dates`() {
        val today = LocalDate(2024, 1, 15)
        val futureDates = listOf(
            LocalDate(2024, 6, 15),   // 5 months later
            LocalDate(2024, 12, 31),  // End of year
            LocalDate(2025, 1, 1),    // Next year
            LocalDate(2026, 12, 31)   // Far future
        )

        futureDates.forEach { date ->
            val result = Validator.validateTuvDate(date, today)
            assertTrue("$date should be valid", result.isSuccess)
        }
    }

    @Test
    fun `validateTuvDate should reject past dates`() {
        val today = LocalDate(2024, 1, 15)
        val pastDates = listOf(
            LocalDate(2023, 12, 31),  // Last year
            LocalDate(2024, 1, 1),    // Beginning of month
            LocalDate(2024, 1, 14),   // Yesterday
            LocalDate(2020, 1, 1)     // Far past
        )

        pastDates.forEach { date ->
            val result = Validator.validateTuvDate(date, today)
            assertTrue("$date should be invalid", result.isFailure)
            assertTrue("Should return ValidationException", 
                result.exceptionOrNull() is ValidationException)
            
            val exception = result.exceptionOrNull() as ValidationException
            assertTrue("Should mention past date in German", 
                exception.userMessage.contains("Vergangenheit"))
        }
    }

    @Test
    fun `validateTuvDate should warn for dates expiring soon`() {
        val today = LocalDate(2024, 1, 15)
        val soonExpiringDates = listOf(
            LocalDate(2024, 1, 16),   // Tomorrow
            LocalDate(2024, 1, 30),   // Two weeks
            LocalDate(2024, 2, 14)    // One month
        )

        soonExpiringDates.forEach { date ->
            val result = Validator.validateTuvDate(date, today)
            assertTrue("$date should be valid but with warning", result.isSuccess)
            // Note: Warning handling would be in UI layer
        }
    }

    @Test
    fun `validateBaujahr should accept reasonable years for fire vehicles`() {
        val validYears = listOf(
            1990,   // Oldest reasonable
            2000,   // Y2K era
            2010,   // Modern
            2020,   // Recent
            2024    // Current year
        )

        validYears.forEach { year ->
            val result = Validator.validateBaujahr(year)
            assertTrue("$year should be valid", result.isSuccess)
        }
    }

    @Test
    fun `validateBaujahr should reject unreasonable years`() {
        val invalidYears = listOf(
            1800,   // Too old
            1950,   // Before modern fire trucks
            2030,   // Future
            0,      // Invalid
            -1      // Negative
        )

        invalidYears.forEach { year ->
            val result = Validator.validateBaujahr(year)
            assertTrue("$year should be invalid", result.isFailure)
        }
    }

    @Test
    fun `validateKilometerstand should accept reasonable values`() {
        val validKilometerstände = listOf(
            0,          // New vehicle
            1000,       // Low usage
            50000,      // Normal usage
            150000,     // High usage
            300000      // Very high but acceptable
        )

        validKilometerstände.forEach { km ->
            val result = Validator.validateKilometerstand(km)
            assertTrue("$km km should be valid", result.isSuccess)
        }
    }

    @Test
    fun `validateKilometerstand should reject unreasonable values`() {
        val invalidKilometerstände = listOf(
            -1,         // Negative
            1000000,    // Too high
            5000000     // Unrealistic
        )

        invalidKilometerstände.forEach { km ->
            val result = Validator.validateKilometerstand(km)
            assertTrue("$km km should be invalid", result.isFailure)
        }
    }

    @Test
    fun `validateForm should validate complete vehicle data`() {
        val validData = mapOf(
            "kennzeichen" to "B-FW 1234",
            "fahrzeugtyp" to "LHF",
            "baujahr" to "2020",
            "kilometerstand" to "25000"
        )

        val result = Validator.validateForm(validData, listOf("kennzeichen", "fahrzeugtyp", "baujahr"))
        assertTrue("Valid form should pass validation", result.isSuccess)
    }

    @Test
    fun `validateForm should fail on missing required fields`() {
        val incompleteData = mapOf(
            "kennzeichen" to "B-FW 1234"
            // Missing fahrzeugtyp
        )

        val result = Validator.validateForm(incompleteData, listOf("kennzeichen", "fahrzeugtyp"))
        assertTrue("Form with missing fields should fail", result.isFailure)
        
        val exception = result.exceptionOrNull() as ValidationException
        assertEquals("Should identify missing field", "fahrzeugtyp", exception.field)
    }

    @Test
    fun `validateForm should fail on invalid field values`() {
        val invalidData = mapOf(
            "kennzeichen" to "INVALID",
            "fahrzeugtyp" to "LHF"
        )

        val result = Validator.validateForm(invalidData, listOf("kennzeichen", "fahrzeugtyp"))
        assertTrue("Form with invalid data should fail", result.isFailure)
        
        val exception = result.exceptionOrNull() as ValidationException
        assertEquals("Should identify invalid field", "kennzeichen", exception.field)
    }

    @Test
    fun `validation error messages should be in German`() {
        val testCases = listOf(
            { Validator.validateKennzeichen("") },
            { Validator.validateFahrzeugtyp("") },
            { Validator.validateBaujahr(-1) },
            { Validator.validateKilometerstand(-1) }
        )

        testCases.forEach { testCase ->
            val result = testCase()
            assertTrue("Should fail validation", result.isFailure)
            
            val exception = result.exceptionOrNull() as ValidationException
            assertFalse("Error message should not be empty", exception.userMessage.isEmpty())
            // Check for common German validation terms
            val germanTermsPresent = listOf("ungültig", "fehlt", "darf nicht", "muss", "Format")
                .any { term -> exception.userMessage.contains(term, ignoreCase = true) }
            assertTrue("Should contain German validation terms", germanTermsPresent)
        }
    }

    @Test
    fun `validateUsername should handle German characters`() {
        val validUsernames = listOf(
            "müller",
            "björn.schneider",
            "übungsleiter",
            "wehrführer"
        )

        validUsernames.forEach { username ->
            val result = Validator.validateUsername(username)
            assertTrue("$username should be valid", result.isSuccess)
        }
    }

    @Test
    fun `validateGruppenname should accept German fire department group names`() {
        val validGroupNames = listOf(
            "Löschzug 1",
            "Hilfeleistungszug",
            "Bereitschaftszug Nord",
            "Einsatzabteilung Mitte",
            "Freiwillige Feuerwehr Ortsteil"
        )

        validGroupNames.forEach { name ->
            val result = Validator.validateGruppenname(name)
            assertTrue("$name should be valid", result.isSuccess)
        }
    }
}