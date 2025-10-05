package com.feuerwehr.checklist.domain.usecase

import com.feuerwehr.checklist.domain.model.Vehicle
import com.feuerwehr.checklist.domain.model.VehicleType
import com.feuerwehr.checklist.domain.model.VehicleGroup
import com.feuerwehr.checklist.domain.repository.VehicleRepository
import com.feuerwehr.checklist.domain.exception.NetworkException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for Vehicle use cases
 * Tests vehicle operations and error handling
 */
class VehicleUseCasesTest {

    private lateinit var vehicleRepository: VehicleRepository
    private lateinit var getVehiclesUseCase: GetVehiclesUseCase
    private lateinit var getVehicleByIdUseCase: GetVehicleByIdUseCase
    private lateinit var fetchVehiclesFromRemoteUseCase: FetchVehiclesFromRemoteUseCase
    private lateinit var syncVehiclesUseCase: SyncVehiclesUseCase

    private val testVehicleType = VehicleType(
        id = 1,
        name = "LHF",
        beschreibung = "Löschgruppenfahrzeug",
        aktiv = true,
        createdAt = Clock.System.now()
    )

    private val testVehicleGroup = VehicleGroup(
        id = 1,
        name = "Gruppe 1",
        createdAt = Clock.System.now()
    )

    private val testVehicle = Vehicle(
        id = 1,
        kennzeichen = "B-FW 1234",
        fahrzeugtyp = testVehicleType,
        fahrzeuggruppe = testVehicleGroup,
        baujahr = 2020,
        kilometerstand = 15000,
        naechsteHu = null,
        naechsteSp = null,
        createdAt = Clock.System.now()
    )

    @Before
    fun setup() {
        vehicleRepository = mockk()
        getVehiclesUseCase = GetVehiclesUseCase(vehicleRepository)
        getVehicleByIdUseCase = GetVehicleByIdUseCase(vehicleRepository)
        fetchVehiclesFromRemoteUseCase = FetchVehiclesFromRemoteUseCase(vehicleRepository)
        syncVehiclesUseCase = SyncVehiclesUseCase(vehicleRepository)
    }

    // GetVehiclesUseCase Tests
    @Test
    fun `getVehicles should return flow of vehicles`() = runTest {
        // Given
        val expectedVehicles = listOf(testVehicle)
        coEvery { vehicleRepository.getVehicles() } returns flowOf(expectedVehicles)

        // When
        val result = getVehiclesUseCase()

        // Then
        result.collect { vehicles ->
            assertEquals("Should return expected vehicles", expectedVehicles, vehicles)
        }
        coVerify { vehicleRepository.getVehicles() }
    }

    @Test
    fun `getVehicles should return empty list when no vehicles exist`() = runTest {
        // Given
        coEvery { vehicleRepository.getVehicles() } returns flowOf(emptyList())

        // When
        val result = getVehiclesUseCase()

        // Then
        result.collect { vehicles ->
            assertTrue("Should return empty list", vehicles.isEmpty())
        }
        coVerify { vehicleRepository.getVehicles() }
    }

    // GetVehicleByIdUseCase Tests
    @Test
    fun `getVehicleById should return vehicle when exists`() = runTest {
        // Given
        val vehicleId = 1
        coEvery { vehicleRepository.getVehicleById(vehicleId) } returns testVehicle

        // When
        val result = getVehicleByIdUseCase(vehicleId)

        // Then
        assertEquals("Should return expected vehicle", testVehicle, result)
        coVerify { vehicleRepository.getVehicleById(vehicleId) }
    }

    @Test
    fun `getVehicleById should return null when vehicle does not exist`() = runTest {
        // Given
        val vehicleId = 999
        coEvery { vehicleRepository.getVehicleById(vehicleId) } returns null

        // When
        val result = getVehicleByIdUseCase(vehicleId)

        // Then
        assertNull("Should return null for non-existent vehicle", result)
        coVerify { vehicleRepository.getVehicleById(vehicleId) }
    }

    // FetchVehiclesFromRemoteUseCase Tests
    @Test
    fun `fetchVehiclesFromRemote should return success with vehicles`() = runTest {
        // Given
        val expectedVehicles = listOf(testVehicle)
        coEvery { vehicleRepository.fetchVehiclesFromRemote() } returns Result.success(expectedVehicles)

        // When
        val result = fetchVehiclesFromRemoteUseCase()

        // Then
        assertTrue("Should return success", result.isSuccess)
        assertEquals("Should return expected vehicles", expectedVehicles, result.getOrNull())
        coVerify { vehicleRepository.fetchVehiclesFromRemote() }
    }

    @Test
    fun `fetchVehiclesFromRemote should return failure on network error`() = runTest {
        // Given
        val networkException = NetworkException(
            message = "Network connection failed",
            userMessage = "Netzwerkverbindung fehlgeschlagen",
            errorCode = "NETWORK_CONNECTION_FAILED"
        )
        coEvery { vehicleRepository.fetchVehiclesFromRemote() } returns Result.failure(networkException)

        // When
        val result = fetchVehiclesFromRemoteUseCase()

        // Then
        assertTrue("Should return failure", result.isFailure)
        assertTrue("Should return NetworkException", result.exceptionOrNull() is NetworkException)
        coVerify { vehicleRepository.fetchVehiclesFromRemote() }
    }

    // SyncVehiclesUseCase Tests
    @Test
    fun `syncVehicles should return success when sync completes`() = runTest {
        // Given
        coEvery { vehicleRepository.syncVehicles() } returns Result.success(Unit)

        // When
        val result = syncVehiclesUseCase()

        // Then
        assertTrue("Should return success", result.isSuccess)
        coVerify { vehicleRepository.syncVehicles() }
    }

    @Test
    fun `syncVehicles should return failure on sync error`() = runTest {
        // Given
        val syncException = com.feuerwehr.checklist.domain.exception.SyncException(
            message = "Sync conflict detected",
            userMessage = "Synchronisation fehlgeschlagen - Konflikte erkannt",
            errorCode = "SYNC_CONFLICT_DETECTED"
        )
        coEvery { vehicleRepository.syncVehicles() } returns Result.failure(syncException)

        // When
        val result = syncVehiclesUseCase()

        // Then
        assertTrue("Should return failure", result.isFailure)
        assertTrue("Should return SyncException", 
            result.exceptionOrNull() is com.feuerwehr.checklist.domain.exception.SyncException)
        coVerify { vehicleRepository.syncVehicles() }
    }

    // Vehicle validation tests
    @Test
    fun `vehicle with valid German license plate should be processed correctly`() {
        // Test various German license plate formats
        val validKennzeichen = listOf(
            "B-FW 1234",    // Berlin Fire Department
            "M-FW 5678",    // Munich Fire Department  
            "HH-FW 9012",   // Hamburg Fire Department
            "K-FW 3456",    // Cologne Fire Department
            "S-FW 7890"     // Stuttgart Fire Department
        )

        validKennzeichen.forEach { kennzeichen ->
            val vehicle = testVehicle.copy(kennzeichen = kennzeichen)
            
            // Verify kennzeichen format is preserved
            assertTrue("Kennzeichen should contain '-'", vehicle.kennzeichen.contains("-"))
            assertTrue("Kennzeichen should contain 'FW'", vehicle.kennzeichen.contains("FW"))
            assertTrue("Kennzeichen should match German format", 
                vehicle.kennzeichen.matches(Regex("[A-Z]{1,3}-FW \\d{3,4}")))
        }
    }

    @Test
    fun `vehicle with fire department specific attributes should be handled correctly`() {
        val vehicle = testVehicle.copy(
            fahrzeugtyp = testVehicleType.copy(name = "TLF", beschreibung = "Tanklöschfahrzeug"),
            baujahr = 2018,
            kilometerstand = 25000
        )

        // Verify fire department specific properties
        assertEquals("Should have correct vehicle type", "TLF", vehicle.fahrzeugtyp?.name)
        assertEquals("Should have German description", "Tanklöschfahrzeug", vehicle.fahrzeugtyp?.beschreibung)
        assertTrue("Should have reasonable mileage for fire truck", vehicle.kilometerstand in 0..200000)
        assertTrue("Should have reasonable age for fire truck", vehicle.baujahr in 1990..2024)
    }
}