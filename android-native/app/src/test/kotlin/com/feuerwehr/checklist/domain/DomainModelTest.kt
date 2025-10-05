package com.feuerwehr.checklist.domain

import com.feuerwehr.checklist.domain.model.UserRole
import com.feuerwehr.checklist.domain.model.Vehicle
import com.feuerwehr.checklist.domain.model.VehicleType
import kotlinx.datetime.Clock
import org.junit.Test
import org.junit.Assert.*

/**
 * Tests for domain model business logic and validation
 * Demonstrates testing of German fire department domain concepts
 */
class DomainModelTest {

    @Test
    fun `UserRole hierarchy should work correctly`() {
        // Test German fire department role hierarchy
        val benutzer = UserRole.BENUTZER
        val gruppenleiter = UserRole.GRUPPENLEITER
        val organisator = UserRole.ORGANISATOR
        val admin = UserRole.ADMIN
        
        // Test role values (not names)
        assertEquals("benutzer", benutzer.value)
        assertEquals("gruppenleiter", gruppenleiter.value)
        assertEquals("organisator", organisator.value)
        assertEquals("admin", admin.value)
        
        // Test role creation from strings
        assertEquals(UserRole.BENUTZER, UserRole.fromString("benutzer"))
        assertEquals(UserRole.GRUPPENLEITER, UserRole.fromString("gruppenleiter"))
        assertEquals(UserRole.ORGANISATOR, UserRole.fromString("organisator"))
        assertEquals(UserRole.ADMIN, UserRole.fromString("admin"))
    }

    @Test
    fun `UserRole fromString should handle invalid input`() {
        // Test invalid role defaults to BENUTZER
        assertEquals(UserRole.BENUTZER, UserRole.fromString("invalid"))
        assertEquals(UserRole.BENUTZER, UserRole.fromString(""))
        assertEquals(UserRole.BENUTZER, UserRole.fromString("user"))
        assertEquals(UserRole.BENUTZER, UserRole.fromString("ADMIN"))  // Case sensitive
    }

    @Test
    fun `Vehicle creation should work correctly`() {
        val now = Clock.System.now()
        val testVehicleType = createTestVehicleType()
        
        val vehicle = Vehicle(
            id = 1,
            kennzeichen = "B-2183",
            fahrzeugtypId = 1,
            fahrzeuggruppeId = 1,
            createdAt = now,
            fahrzeugtyp = testVehicleType
        )
        
        assertEquals(1, vehicle.id)
        assertEquals("B-2183", vehicle.kennzeichen)
        assertEquals(1, vehicle.fahrzeugtypId)
        assertEquals(1, vehicle.fahrzeuggruppeId)
        assertNotNull(vehicle.createdAt)
        assertNotNull(vehicle.fahrzeugtyp)
        assertEquals("LF", vehicle.fahrzeugtyp.name)
    }

    @Test
    fun `German vehicle identifiers should be valid`() {
        // Test common German fire department vehicle identifiers
        val validKennzeichen = listOf(
            "B-2183",      // Berlin format
            "M-2226",      // Munich format
            "HH-2231",     // Hamburg format
            "K-1234",      // Cologne format
            "DD-5678"      // Dresden format
        )
        
        validKennzeichen.forEach { kennzeichen ->
            assertTrue("$kennzeichen should contain hyphen", kennzeichen.contains("-"))
            assertTrue("$kennzeichen should have city prefix", kennzeichen.split("-")[0].isNotEmpty())
            assertTrue("$kennzeichen should have number suffix", kennzeichen.split("-")[1].isNotEmpty())
        }
    }

    @Test
    fun `VehicleType should have correct properties`() {
        val vehicleType = createTestVehicleType()
        
        assertEquals(1, vehicleType.id)
        assertEquals("LF", vehicleType.name)
        assertEquals("Löschfahrzeug", vehicleType.beschreibung)
        assertTrue("VehicleType should be active by default", vehicleType.aktiv)
        assertNotNull("VehicleType should have creation timestamp", vehicleType.createdAt)
    }

    @Test
    fun `Vehicle creation timestamps should be reasonable`() {
        val beforeCreation = Clock.System.now()
        val vehicle = createTestVehicle()
        val afterCreation = Clock.System.now()
        
        // Timestamps should be within reasonable bounds (between before and after creation)
        assertTrue("Vehicle created time should not be before creation started", 
                  vehicle.createdAt >= beforeCreation)
        assertTrue("Vehicle created time should not be too far in future", 
                  vehicle.createdAt <= afterCreation)
    }

    // Helper method for creating test vehicles
    private fun createTestVehicle(
        id: Int = 1,
        kennzeichen: String = "B-2183"
    ): Vehicle {
        return Vehicle(
            id = id,
            kennzeichen = kennzeichen,
            fahrzeugtypId = 1,
            fahrzeuggruppeId = 1,
            createdAt = Clock.System.now(),
            fahrzeugtyp = createTestVehicleType()
        )
    }
    
    // Helper method for creating test vehicle types
    private fun createTestVehicleType(
        id: Int = 1,
        name: String = "LF",
        beschreibung: String = "Löschfahrzeug"
    ): VehicleType {
        return VehicleType(
            id = id,
            name = name,
            beschreibung = beschreibung,
            aktiv = true,
            createdAt = Clock.System.now()
        )
    }
}