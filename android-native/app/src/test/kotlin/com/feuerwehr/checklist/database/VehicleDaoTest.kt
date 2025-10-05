package com.feuerwehr.checklist.database

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.feuerwehr.checklist.data.local.ChecklistDatabase
import com.feuerwehr.checklist.data.local.dao.VehicleDao
import com.feuerwehr.checklist.data.local.entity.VehicleEntity
import com.feuerwehr.checklist.data.local.entity.VehicleTypeEntity
import com.feuerwehr.checklist.data.local.entity.VehicleGroupEntity
import com.feuerwehr.checklist.data.local.entity.SyncStatus
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith

/**
 * Room database tests
 * Tests database operations, relationships, and transactions
 */
@RunWith(AndroidJUnit4::class)
class VehicleDaoTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: ChecklistDatabase
    private lateinit var vehicleDao: VehicleDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ChecklistDatabase::class.java
        )
        .allowMainThreadQueries()
        .build()

        vehicleDao = database.vehicleDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertAndGetVehicle() = runTest {
        val now = Clock.System.now()
        val vehicleType = VehicleTypeEntity(
            id = 1,
            name = "MTF",
            beschreibung = "Mannschaftstransportfahrzeug",
            aktiv = true,
            createdAt = now
        )
        val vehicleGroup = VehicleGroupEntity(
            id = 1,
            name = "Gruppe 1",
            createdAt = now
        )
        val vehicle = VehicleEntity(
            id = 1,
            kennzeichen = "B-2183",
            fahrzeugtypId = 1,
            fahrzeuggruppeId = 1,
            aktiv = true,
            createdAt = now
        )

        // Insert dependencies first
        vehicleDao.insertVehicleType(vehicleType)
        vehicleDao.insertVehicleGroup(vehicleGroup)
        vehicleDao.insertVehicle(vehicle)

        // Verify vehicle was inserted
        val retrievedVehicle = vehicleDao.getVehicleById(1)
        assertNotNull(retrievedVehicle)
        assertEquals("B-2183", retrievedVehicle?.kennzeichen)
        assertEquals(1, retrievedVehicle?.fahrzeugtypId)
        assertEquals(1, retrievedVehicle?.fahrzeuggruppeId)
    }

    @Test
    fun getAllVehiclesFlow() = runTest {
        val now = Clock.System.now()
        
        // Insert test data
        val vehicleType = VehicleTypeEntity(1, "MTF", "Test", true, now)
        val vehicleGroup = VehicleGroupEntity(1, "Gruppe 1", now)
        
        vehicleDao.insertVehicleType(vehicleType)
        vehicleDao.insertVehicleGroup(vehicleGroup)
        
        val vehicles = listOf(
            VehicleEntity(1, "B-2183", 1, 1, true, now),
            VehicleEntity(2, "B-2226", 1, 1, true, now)
        )
        
        vehicleDao.insertVehicles(vehicles)

        // Test flow emission
        vehicleDao.getAllVehiclesFlow().test {
            val result = awaitItem()
            assertEquals(2, result.size)
            assertEquals("B-2183", result[0].kennzeichen)
            assertEquals("B-2226", result[1].kennzeichen)
        }
    }

    @Test
    fun searchVehiclesFlow() = runTest {
        val now = Clock.System.now()
        
        // Insert test data
        val vehicleType = VehicleTypeEntity(1, "MTF", "Test", true, now)
        val vehicleGroup = VehicleGroupEntity(1, "Gruppe 1", now)
        
        vehicleDao.insertVehicleType(vehicleType)
        vehicleDao.insertVehicleGroup(vehicleGroup)
        
        val vehicles = listOf(
            VehicleEntity(1, "B-2183", 1, 1, true, now),
            VehicleEntity(2, "B-2226", 1, 1, true, now),
            VehicleEntity(3, "B-2231", 1, 1, true, now)
        )
        
        vehicleDao.insertVehicles(vehicles)

        // Search for specific pattern
        vehicleDao.searchVehiclesFlow("%2183%").test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("B-2183", result[0].kennzeichen)
        }

        // Search for common pattern
        vehicleDao.searchVehiclesFlow("%B-22%").test {
            val result = awaitItem()
            assertEquals(2, result.size)
            assertTrue(result.any { it.kennzeichen == "B-2226" })
            assertTrue(result.any { it.kennzeichen == "B-2231" })
        }
    }

    @Test
    fun updateVehicle() = runTest {
        val now = Clock.System.now()
        
        // Insert dependencies and vehicle
        val vehicleType = VehicleTypeEntity(1, "MTF", "Test", true, now)
        val vehicleGroup = VehicleGroupEntity(1, "Gruppe 1", now)
        val vehicle = VehicleEntity(1, "B-2183", 1, 1, true, now)
        
        vehicleDao.insertVehicleType(vehicleType)
        vehicleDao.insertVehicleGroup(vehicleGroup)
        vehicleDao.insertVehicle(vehicle)

        // Update vehicle
        val updatedVehicle = vehicle.copy(kennzeichen = "B-2183-UPDATED", aktiv = false)
        vehicleDao.updateVehicle(updatedVehicle)

        // Verify update
        val retrievedVehicle = vehicleDao.getVehicleById(1)
        assertNotNull(retrievedVehicle)
        assertEquals("B-2183-UPDATED", retrievedVehicle?.kennzeichen)
        assertFalse(retrievedVehicle?.aktiv == true)
    }

    @Test
    fun deleteVehicle() = runTest {
        val now = Clock.System.now()
        
        // Insert dependencies and vehicle
        val vehicleType = VehicleTypeEntity(1, "MTF", "Test", true, now)
        val vehicleGroup = VehicleGroupEntity(1, "Gruppe 1", now)
        val vehicle = VehicleEntity(1, "B-2183", 1, 1, true, now)
        
        vehicleDao.insertVehicleType(vehicleType)
        vehicleDao.insertVehicleGroup(vehicleGroup)
        vehicleDao.insertVehicle(vehicle)

        // Verify vehicle exists
        assertNotNull(vehicleDao.getVehicleById(1))

        // Delete vehicle
        vehicleDao.deleteVehicle(vehicle)

        // Verify vehicle is deleted
        assertNull(vehicleDao.getVehicleById(1))
    }

    @Test
    fun insertDataWithTransaction() = runTest {
        val now = Clock.System.now()
        
        val vehicleTypes = listOf(
            VehicleTypeEntity(1, "MTF", "Mannschaftstransportfahrzeug", true, now),
            VehicleTypeEntity(2, "LF", "Löschfahrzeug", true, now)
        )
        
        val vehicleGroups = listOf(
            VehicleGroupEntity(1, "Gruppe 1", now),
            VehicleGroupEntity(2, "Gruppe 2", now)
        )
        
        val vehicles = listOf(
            VehicleEntity(1, "B-2183", 1, 1, true, now),
            VehicleEntity(2, "B-2226", 2, 2, true, now)
        )

        // Insert all data in transaction
        vehicleDao.insertDataWithTransaction(vehicleTypes, vehicleGroups, vehicles)

        // Verify all data was inserted
        vehicleDao.getAllVehicleTypesFlow().test {
            val types = awaitItem()
            assertEquals(2, types.size)
            assertTrue(types.any { it.name == "MTF" })
            assertTrue(types.any { it.name == "LF" })
        }

        vehicleDao.getAllVehicleGroupsFlow().test {
            val groups = awaitItem()
            assertEquals(2, groups.size)
            assertTrue(groups.any { it.name == "Gruppe 1" })
            assertTrue(groups.any { it.name == "Gruppe 2" })
        }

        vehicleDao.getAllVehiclesFlow().test {
            val vehicleList = awaitItem()
            assertEquals(2, vehicleList.size)
            assertTrue(vehicleList.any { it.kennzeichen == "B-2183" })
            assertTrue(vehicleList.any { it.kennzeichen == "B-2226" })
        }
    }

    @Test
    fun clearAllVehicleData() = runTest {
        val now = Clock.System.now()
        
        // Insert test data
        val vehicleTypes = listOf(VehicleTypeEntity(1, "MTF", "Test", true, now))
        val vehicleGroups = listOf(VehicleGroupEntity(1, "Gruppe 1", now))
        val vehicles = listOf(VehicleEntity(1, "B-2183", 1, 1, true, now))
        
        vehicleDao.insertDataWithTransaction(vehicleTypes, vehicleGroups, vehicles)
        
        // Verify data exists
        vehicleDao.getAllVehiclesFlow().test {
            val result = awaitItem()
            assertEquals(1, result.size)
        }

        // Clear all data
        vehicleDao.clearAllVehicleData()

        // Verify all data is cleared
        vehicleDao.getAllVehiclesFlow().test {
            val vehicles = awaitItem()
            assertEquals(0, vehicles.size)
        }
        
        vehicleDao.getAllVehicleGroupsFlow().test {
            val groups = awaitItem()
            assertEquals(0, groups.size)
        }
        
        vehicleDao.getAllVehicleTypesFlow().test {
            val types = awaitItem()
            assertEquals(0, types.size)
        }
    }

    @Test
    fun foreignKeyConstraints() = runTest {
        val now = Clock.System.now()
        
        // Try to insert vehicle without vehicle type (should fail with FK constraint)
        val vehicle = VehicleEntity(1, "B-2183", 999, 999, true, now)
        
        try {
            vehicleDao.insertVehicle(vehicle)
            fail("Expected foreign key constraint violation")
        } catch (e: Exception) {
            // Expected behavior - FK constraint should prevent insertion
            assertTrue(e.message?.contains("FOREIGN KEY") == true || 
                      e.message?.contains("constraint") == true)
        }
    }

    @Test
    fun syncStatusFiltering() = runTest {
        val now = Clock.System.now()
        
        // Insert dependencies
        val vehicleType = VehicleTypeEntity(1, "MTF", "Test", true, now)
        val vehicleGroup = VehicleGroupEntity(1, "Gruppe 1", now)
        
        vehicleDao.insertVehicleType(vehicleType)
        vehicleDao.insertVehicleGroup(vehicleGroup)
        
        // Insert vehicles with different sync statuses
        val vehicles = listOf(
            VehicleEntity(1, "B-2183", 1, 1, true, now, SyncStatus.SYNCED),
            VehicleEntity(2, "B-2226", 1, 1, true, now, SyncStatus.PENDING),
            VehicleEntity(3, "B-2231", 1, 1, true, now, SyncStatus.CONFLICT)
        )
        
        vehicleDao.insertVehicles(vehicles)

        // Test sync status queries
        val pendingCount = vehicleDao.getPendingSyncCount()
        assertEquals(1, pendingCount)
        
        val conflictCount = vehicleDao.getConflictCount()
        assertEquals(1, conflictCount)
    }
}