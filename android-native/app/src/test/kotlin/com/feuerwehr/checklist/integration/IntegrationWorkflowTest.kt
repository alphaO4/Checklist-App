package com.feuerwehr.checklist.integration

import com.feuerwehr.checklist.domain.model.UserRole
import com.feuerwehr.checklist.domain.model.Vehicle
import com.feuerwehr.checklist.domain.model.VehicleType
import kotlinx.datetime.Clock
import org.junit.Test
import org.junit.Assert.*

/**
 * Integration tests demonstrating complete workflow testing
 * Shows how different domain models work together in realistic scenarios
 */
class IntegrationWorkflowTest {

    @Test
    fun `Complete fire department vehicle workflow should work`() {
        // 1. Create German fire department vehicle type
        val vehicleType = VehicleType(
            id = 1,
            name = "LF 20",
            beschreibung = "Löschfahrzeug mit 2000L Wassertank",
            aktiv = true,
            createdAt = Clock.System.now()
        )

        // 2. Create vehicle with German license plate format
        val vehicle = Vehicle(
            id = 1,
            kennzeichen = "B-2183",  // Berlin fire department format
            fahrzeugtypId = vehicleType.id,
            fahrzeuggruppeId = 1,
            createdAt = Clock.System.now(),
            fahrzeugtyp = vehicleType
        )

        // 3. Verify vehicle creation
        assertEquals("B-2183", vehicle.kennzeichen)
        assertEquals("LF 20", vehicle.fahrzeugtyp.name)
        assertTrue("Vehicle type should be active", vehicle.fahrzeugtyp.aktiv)
        
        // 4. Test German fire department license plate pattern
        assertTrue("License plate should follow German format", 
                  isValidGermanFireDepartmentLicensePlate(vehicle.kennzeichen))
    }

    @Test
    fun `User role hierarchy should support fire department structure`() {
        // Test complete German fire department role hierarchy
        val roles = listOf(
            UserRole.BENUTZER,
            UserRole.GRUPPENLEITER,
            UserRole.ORGANISATOR,
            UserRole.ADMIN
        )

        // Verify all roles are distinct
        assertEquals(4, roles.distinct().size)
        
        // Verify role values match German terms
        assertEquals("benutzer", UserRole.BENUTZER.value)
        assertEquals("gruppenleiter", UserRole.GRUPPENLEITER.value)
        assertEquals("organisator", UserRole.ORGANISATOR.value)
        assertEquals("admin", UserRole.ADMIN.value)
        
        // Test role parsing from backend data
        roles.forEach { expectedRole ->
            val parsedRole = UserRole.fromString(expectedRole.value)
            assertEquals("Role parsing should be consistent", expectedRole, parsedRole)
        }
    }

    @Test
    fun `Fire department vehicle types should match German standards`() {
        val germanVehicleTypes = listOf(
            createVehicleType("LF", "Löschfahrzeug"),
            createVehicleType("TLF", "Tanklöschfahrzeug"),
            createVehicleType("DLK", "Drehleiter mit Korb"),
            createVehicleType("RTB", "Rüstwagen Technische Beistellung"),
            createVehicleType("MTF", "Mannschaftstransportfahrzeug"),
            createVehicleType("ELW", "Einsatzleitwagen")
        )

        // Verify all vehicle types are created properly
        germanVehicleTypes.forEach { vehicleType ->
            assertNotNull("Vehicle type should have ID", vehicleType.id)
            assertFalse("Vehicle type name should not be empty", vehicleType.name.isEmpty())
            assertNotNull("Vehicle type should have description", vehicleType.beschreibung)
            assertTrue("Vehicle type should be active by default", vehicleType.aktiv)
            assertNotNull("Vehicle type should have creation timestamp", vehicleType.createdAt)
        }
        
        // Test specific German vehicle types
        val lf = germanVehicleTypes.find { it.name == "LF" }
        assertNotNull("LF (Löschfahrzeug) should exist", lf)
        assertTrue("LF description should contain 'Löschfahrzeug'", 
                  lf!!.beschreibung!!.contains("Löschfahrzeug"))
    }

    @Test
    fun `Complete vehicle management workflow should work`() {
        val now = Clock.System.now()
        
        // 1. Create vehicle type
        val vehicleType = createVehicleType("LF 10", "Löschfahrzeug 10/6")
        
        // 2. Create multiple vehicles of same type
        val vehicles = listOf(
            createVehicle(1, "B-2183", vehicleType),
            createVehicle(2, "B-2184", vehicleType),
            createVehicle(3, "B-2185", vehicleType)
        )
        
        // 3. Verify vehicle fleet
        assertEquals(3, vehicles.size)
        vehicles.forEach { vehicle ->
            assertEquals(vehicleType.name, vehicle.fahrzeugtyp.name)
            assertTrue("Vehicle should have valid Berlin license plate", 
                      vehicle.kennzeichen.startsWith("B-"))
            assertTrue("Vehicle creation should be recent", 
                      vehicle.createdAt >= now)
        }
        
        // 4. Test vehicle identification
        val vehiclesByLicense = vehicles.associateBy { it.kennzeichen }
        assertNotNull("Should find vehicle B-2183", vehiclesByLicense["B-2183"])
        assertNotNull("Should find vehicle B-2184", vehiclesByLicense["B-2184"])
        assertNotNull("Should find vehicle B-2185", vehiclesByLicense["B-2185"])
    }

    @Test
    fun `Data validation should work for all domain models`() {
        // Test UserRole validation
        val validRole = UserRole.fromString("admin")
        val invalidRole = UserRole.fromString("invalid")
        
        assertEquals(UserRole.ADMIN, validRole)
        assertEquals(UserRole.BENUTZER, invalidRole) // Should default to BENUTZER
        
        // Test VehicleType validation
        val vehicleType = createVehicleType("", "Empty name test")
        assertTrue("Even empty vehicle type name should be handled", vehicleType.name.isEmpty())
        
        // Test Vehicle validation  
        val vehicle = createVehicle(999, "INVALID-FORMAT", vehicleType)
        assertEquals(999, vehicle.id)
        assertEquals("INVALID-FORMAT", vehicle.kennzeichen) // Should accept any format for flexibility
    }

    // Helper methods for test data creation
    private fun createVehicleType(
        name: String, 
        beschreibung: String,
        id: Int = 1
    ): VehicleType {
        return VehicleType(
            id = id,
            name = name,
            beschreibung = beschreibung,
            aktiv = true,
            createdAt = Clock.System.now()
        )
    }

    private fun createVehicle(
        id: Int,
        kennzeichen: String,
        vehicleType: VehicleType
    ): Vehicle {
        return Vehicle(
            id = id,
            kennzeichen = kennzeichen,
            fahrzeugtypId = vehicleType.id,
            fahrzeuggruppeId = 1,
            createdAt = Clock.System.now(),
            fahrzeugtyp = vehicleType
        )
    }

    private fun isValidGermanFireDepartmentLicensePlate(kennzeichen: String): Boolean {
        // German fire department vehicles typically follow format: [City]-[Number]
        val parts = kennzeichen.split("-")
        return parts.size == 2 && 
               parts[0].isNotEmpty() && 
               parts[1].isNotEmpty() &&
               parts[1].all { it.isDigit() }
    }
}