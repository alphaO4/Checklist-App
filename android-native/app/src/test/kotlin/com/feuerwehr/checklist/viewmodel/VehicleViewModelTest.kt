package com.feuerwehr.checklist.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.feuerwehr.checklist.domain.repository.VehicleRepository
import com.feuerwehr.checklist.domain.model.Vehicle
import com.feuerwehr.checklist.domain.model.VehicleType
import com.feuerwehr.checklist.domain.model.VehicleGroup
import com.feuerwehr.checklist.presentation.viewmodel.VehicleViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Clock
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import org.mockito.kotlin.verify

/**
 * Unit tests for VehicleViewModel
 * Tests vehicle list management, filtering, and data loading
 */
@OptIn(ExperimentalCoroutinesApi::class)
class VehicleViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var vehicleRepository: VehicleRepository

    private lateinit var viewModel: VehicleViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        viewModel = VehicleViewModel(vehicleRepository)
    }

    @Test
    fun `initial state should be correct`() {
        val initialState = viewModel.uiState.value
        
        assertFalse(initialState.isLoading)
        assertTrue(initialState.vehicles.isEmpty())
        assertTrue(initialState.vehicleTypes.isEmpty())
        assertTrue(initialState.vehicleGroups.isEmpty())
        assertTrue(initialState.searchQuery.isEmpty())
        assertNull(initialState.selectedTypeFilter)
        assertNull(initialState.selectedGroupFilter)
        assertNull(initialState.errorMessage)
    }

    @Test
    fun `loadVehicles should update vehicles list`() = runTest {
        val testVehicles = listOf(
            createTestVehicle(id = 1, kennzeichen = "B-2183"),
            createTestVehicle(id = 2, kennzeichen = "B-2226")
        )
        
        whenever(vehicleRepository.getAllVehicles()).thenReturn(flowOf(testVehicles))
        
        viewModel.uiState.test {
            val initialState = awaitItem()
            assertTrue(initialState.vehicles.isEmpty())
            
            viewModel.loadVehicles()
            testScheduler.advanceUntilIdle()
            
            val loadedState = awaitItem()
            assertEquals(2, loadedState.vehicles.size)
            assertEquals("B-2183", loadedState.vehicles[0].kennzeichen)
            assertEquals("B-2226", loadedState.vehicles[1].kennzeichen)
        }
    }

    @Test
    fun `loadVehicleTypes should update types list`() = runTest {
        val testTypes = listOf(
            VehicleType(1, "MTF", "Mannschaftstransportfahrzeug", true, Clock.System.now()),
            VehicleType(2, "LF", "Löschfahrzeug", true, Clock.System.now())
        )
        
        whenever(vehicleRepository.getAllVehicleTypes()).thenReturn(flowOf(testTypes))
        
        viewModel.uiState.test {
            val initialState = awaitItem()
            assertTrue(initialState.vehicleTypes.isEmpty())
            
            viewModel.loadVehicleTypes()
            testScheduler.advanceUntilIdle()
            
            val loadedState = awaitItem()
            assertEquals(2, loadedState.vehicleTypes.size)
            assertEquals("MTF", loadedState.vehicleTypes[0].name)
            assertEquals("LF", loadedState.vehicleTypes[1].name)
        }
    }

    @Test
    fun `updateSearchQuery should filter vehicles`() = runTest {
        val searchQuery = "B-2183"
        
        viewModel.updateSearchQuery(searchQuery)
        testScheduler.advanceUntilIdle()
        
        assertEquals(searchQuery, viewModel.uiState.value.searchQuery)
        verify(vehicleRepository).searchVehicles("%$searchQuery%")
    }

    @Test
    fun `setTypeFilter should update filter and reload vehicles`() = runTest {
        val testType = VehicleType(1, "MTF", "Mannschaftstransportfahrzeug", true, Clock.System.now())
        
        viewModel.setTypeFilter(testType)
        testScheduler.advanceUntilIdle()
        
        assertEquals(testType, viewModel.uiState.value.selectedTypeFilter)
    }

    @Test
    fun `clearTypeFilter should remove filter`() = runTest {
        val testType = VehicleType(1, "MTF", "Mannschaftstransportfahrzeug", true, Clock.System.now())
        
        // Set filter first
        viewModel.setTypeFilter(testType)
        testScheduler.advanceUntilIdle()
        assertEquals(testType, viewModel.uiState.value.selectedTypeFilter)
        
        // Clear filter
        viewModel.clearTypeFilter()
        testScheduler.advanceUntilIdle()
        
        assertNull(viewModel.uiState.value.selectedTypeFilter)
    }

    @Test
    fun `refreshVehicles should trigger repository refresh`() = runTest {
        whenever(vehicleRepository.fetchVehiclesFromRemote())
            .thenReturn(Result.success(emptyList()))
        
        viewModel.refreshVehicles()
        testScheduler.advanceUntilIdle()
        
        verify(vehicleRepository).fetchVehiclesFromRemote()
    }

    @Test
    fun `refreshVehicles with error should set error message`() = runTest {
        val errorMessage = "Network error"
        whenever(vehicleRepository.fetchVehiclesFromRemote())
            .thenReturn(Result.failure(Exception(errorMessage)))
        
        viewModel.refreshVehicles()
        testScheduler.advanceUntilIdle()
        
        assertEquals(errorMessage, viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `clearError should remove error message`() = runTest {
        // First set an error
        whenever(vehicleRepository.fetchVehiclesFromRemote())
            .thenReturn(Result.failure(Exception("Test error")))
        
        viewModel.refreshVehicles()
        testScheduler.advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.errorMessage)
        
        // Then clear it
        viewModel.clearError()
        testScheduler.advanceUntilIdle()
        
        assertNull(viewModel.uiState.value.errorMessage)
    }

    private fun createTestVehicle(
        id: Int,
        kennzeichen: String,
        type: VehicleType? = null,
        group: VehicleGroup? = null
    ): Vehicle {
        return Vehicle(
            id = id,
            kennzeichen = kennzeichen,
            fahrzeugtypId = type?.id ?: 1,
            fahrzeuggruppeId = group?.id ?: 1,
            aktiv = true,
            createdAt = Clock.System.now(),
            vehicleType = type,
            vehicleGroup = group
        )
    }
}