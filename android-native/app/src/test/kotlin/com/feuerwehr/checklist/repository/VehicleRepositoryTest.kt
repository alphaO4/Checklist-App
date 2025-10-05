package com.feuerwehr.checklist.repository

import app.cash.turbine.test
import com.feuerwehr.checklist.data.local.dao.VehicleDao
import com.feuerwehr.checklist.data.local.entity.VehicleEntity
import com.feuerwehr.checklist.data.local.entity.VehicleTypeEntity
import com.feuerwehr.checklist.data.local.entity.VehicleGroupEntity
import com.feuerwehr.checklist.data.local.entity.SyncStatus
import com.feuerwehr.checklist.data.remote.api.VehicleApiService
import com.feuerwehr.checklist.data.remote.dto.VehicleDto
import com.feuerwehr.checklist.data.remote.dto.VehicleTypeDto
import com.feuerwehr.checklist.data.remote.dto.VehicleGroupDto
import com.feuerwehr.checklist.data.remote.dto.PaginatedResponseDto
import com.feuerwehr.checklist.data.repository.VehicleRepositoryImpl
import com.feuerwehr.checklist.data.sync.SyncManager
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*

/**
 * Unit tests for VehicleRepositoryImpl
 * Tests data layer interactions between local database, remote API, and sync
 */
class VehicleRepositoryTest {

    @Mock
    private lateinit var vehicleDao: VehicleDao

    @Mock
    private lateinit var vehicleApi: VehicleApiService

    @Mock
    private lateinit var syncManager: SyncManager

    private lateinit var repository: VehicleRepositoryImpl

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        repository = VehicleRepositoryImpl(vehicleDao, vehicleApi, syncManager)
    }

    @Test
    fun `getAllVehicles should return flow from dao`() = runTest {
        val testEntities = listOf(
            createTestVehicleEntity(id = 1, kennzeichen = "B-2183"),
            createTestVehicleEntity(id = 2, kennzeichen = "B-2226")
        )
        
        whenever(vehicleDao.getAllVehiclesFlow()).thenReturn(flowOf(testEntities))
        
        repository.getAllVehicles().test {
            val vehicles = awaitItem()
            
            assertEquals(2, vehicles.size)
            assertEquals("B-2183", vehicles[0].kennzeichen)
            assertEquals("B-2226", vehicles[1].kennzeichen)
            awaitComplete()
        }
        
        verify(vehicleDao).getAllVehiclesFlow()
    }

    @Test
    fun `getVehicleById should return single vehicle from dao`() = runTest {
        val testEntity = createTestVehicleEntity(id = 1, kennzeichen = "B-2183")
        
        whenever(vehicleDao.getVehicleById(1)).thenReturn(testEntity)
        
        val result = repository.getVehicleById(1)
        
        assertNotNull(result)
        assertEquals(1, result?.id)
        assertEquals("B-2183", result?.kennzeichen)
        
        verify(vehicleDao).getVehicleById(1)
    }

    @Test
    fun `searchVehicles should call dao with formatted query`() = runTest {
        val searchQuery = "B-2183"
        val testEntities = listOf(createTestVehicleEntity(id = 1, kennzeichen = "B-2183"))
        
        whenever(vehicleDao.searchVehiclesFlow("%$searchQuery%")).thenReturn(flowOf(testEntities))
        
        repository.searchVehicles(searchQuery).test {
            val vehicles = awaitItem()
            
            assertEquals(1, vehicles.size)
            assertEquals("B-2183", vehicles[0].kennzeichen)
            awaitComplete()
        }
        
        verify(vehicleDao).searchVehiclesFlow("%$searchQuery%")
    }

    @Test
    fun `fetchVehiclesFromRemote should success when API returns data`() = runTest {
        // Mock API responses
        val vehicleTypes = listOf(createTestVehicleTypeDto(id = 1, name = "MTF"))
        val vehicleGroups = listOf(createTestVehicleGroupDto(id = 1, name = "Gruppe 1"))
        val vehicles = listOf(createTestVehicleDto(id = 1, kennzeichen = "B-2183"))
        val paginatedResponse = PaginatedResponseDto(
            items = vehicles,
            total = 1,
            page = 1,
            size = 10,
            pages = 1
        )
        
        whenever(vehicleApi.getVehicleTypes()).thenReturn(vehicleTypes)
        whenever(vehicleApi.getVehicleGroups()).thenReturn(vehicleGroups)
        whenever(vehicleApi.getVehicles()).thenReturn(paginatedResponse)
        
        val result = repository.fetchVehiclesFromRemote()
        
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
        
        // Verify DAO transaction was called
        verify(vehicleDao).insertDataWithTransaction(
            vehicleTypes = any(),
            vehicleGroups = any(),
            vehicles = any()
        )
    }

    @Test
    fun `fetchVehiclesFromRemote should fail when API throws exception`() = runTest {
        val errorMessage = "Network error"
        whenever(vehicleApi.getVehicleTypes()).thenThrow(RuntimeException(errorMessage))
        
        val result = repository.fetchVehiclesFromRemote()
        
        assertTrue(result.isFailure)
        assertEquals(errorMessage, result.exceptionOrNull()?.message)
    }

    @Test
    fun `insertVehicle should call dao insert`() = runTest {
        val testEntity = createTestVehicleEntity(id = 1, kennzeichen = "B-2183")
        whenever(vehicleDao.insertVehicle(testEntity)).thenReturn(1L)
        
        val vehicle = testEntity.toDomain(null, null)
        val result = repository.insertVehicle(vehicle)
        
        assertTrue(result.isSuccess)
        assertEquals(1L, result.getOrNull())
        
        verify(vehicleDao).insertVehicle(any())
    }

    @Test
    fun `updateVehicle should call dao update`() = runTest {
        val testEntity = createTestVehicleEntity(id = 1, kennzeichen = "B-2183-UPDATED")
        
        val vehicle = testEntity.toDomain(null, null)
        repository.updateVehicle(vehicle)
        
        verify(vehicleDao).updateVehicle(any())
    }

    @Test
    fun `deleteVehicle should call dao delete`() = runTest {
        val testEntity = createTestVehicleEntity(id = 1, kennzeichen = "B-2183")
        
        val vehicle = testEntity.toDomain(null, null)
        repository.deleteVehicle(vehicle)
        
        verify(vehicleDao).deleteVehicle(any())
    }

    @Test
    fun `syncVehicles should trigger sync manager`() = runTest {
        val result = repository.syncVehicles()
        
        assertTrue(result.isSuccess)
        verify(syncManager).triggerImmediateSync()
    }

    // Helper methods for creating test data
    private fun createTestVehicleEntity(
        id: Int,
        kennzeichen: String,
        fahrzeugtypId: Int = 1,
        fahrzeuggruppeId: Int = 1
    ): VehicleEntity {
        val now = Clock.System.now()
        return VehicleEntity(
            id = id,
            kennzeichen = kennzeichen,
            fahrzeugtypId = fahrzeugtypId,
            fahrzeuggruppeId = fahrzeuggruppeId,
            aktiv = true,
            createdAt = now,
            syncStatus = SyncStatus.SYNCED,
            lastModified = now,
            version = 1
        )
    }

    private fun createTestVehicleTypeDto(id: Int, name: String): VehicleTypeDto {
        return VehicleTypeDto(
            id = id,
            name = name,
            beschreibung = "Test description",
            aktiv = true,
            createdAt = "2025-09-19T10:00:00Z"
        )
    }

    private fun createTestVehicleGroupDto(id: Int, name: String): VehicleGroupDto {
        return VehicleGroupDto(
            id = id,
            name = name,
            createdAt = "2025-09-19T10:00:00Z"
        )
    }

    private fun createTestVehicleDto(
        id: Int,
        kennzeichen: String,
        fahrzeugtypId: Int = 1,
        fahrzeuggruppeId: Int = 1
    ): VehicleDto {
        return VehicleDto(
            id = id,
            kennzeichen = kennzeichen,
            fahrzeugtypId = fahrzeugtypId,
            fahrzeuggruppeId = fahrzeuggruppeId,
            aktiv = true,
            createdAt = "2025-09-19T10:00:00Z"
        )
    }
}