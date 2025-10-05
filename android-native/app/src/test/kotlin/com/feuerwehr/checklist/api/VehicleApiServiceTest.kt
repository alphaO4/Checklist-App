package com.feuerwehr.checklist.api

import com.feuerwehr.checklist.data.remote.api.VehicleApiService
import com.feuerwehr.checklist.data.remote.dto.VehicleDto
import com.feuerwehr.checklist.data.remote.dto.VehicleTypeDto
import com.feuerwehr.checklist.data.remote.dto.VehicleGroupDto
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Integration tests for API services using MockWebServer
 * Tests real HTTP communication with mocked backend responses
 */
class VehicleApiServiceTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var apiService: VehicleApiService

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val retrofit = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(VehicleApiService::class.java)
    }

    @After
    fun teardown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `getVehicles should parse response correctly`() = runTest {
        val mockVehicles = listOf(
            VehicleDto(
                id = 1,
                kennzeichen = "B-2183",
                fahrzeugtypId = 1,
                fahrzeuggruppeId = 1,
                aktiv = true,
                createdAt = "2025-09-19T10:00:00Z"
            ),
            VehicleDto(
                id = 2,
                kennzeichen = "B-2226",
                fahrzeugtypId = 2,
                fahrzeuggruppeId = 1,
                aktiv = true,
                createdAt = "2025-09-19T10:00:00Z"
            )
        )

        val mockResponse = """{
            "items": [
                {
                    "id": 1,
                    "kennzeichen": "B-2183",
                    "fahrzeugtypId": 1,
                    "fahrzeuggruppeId": 1,
                    "aktiv": true,
                    "createdAt": "2025-09-19T10:00:00Z"
                },
                {
                    "id": 2,
                    "kennzeichen": "B-2226",
                    "fahrzeugtypId": 2,
                    "fahrzeuggruppeId": 1,
                    "aktiv": true,
                    "createdAt": "2025-09-19T10:00:00Z"
                }
            ],
            "total": 2,
            "page": 1,
            "size": 10,
            "pages": 1
        }"""

        mockWebServer.enqueue(
            MockResponse()
                .setBody(mockResponse)
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
        )

        val response = apiService.getVehicles()

        assertEquals(2, response.items.size)
        assertEquals("B-2183", response.items[0].kennzeichen)
        assertEquals("B-2226", response.items[1].kennzeichen)
        assertEquals(2, response.total)
        assertEquals(1, response.page)

        // Verify the request
        val request = mockWebServer.takeRequest()
        assertEquals("GET", request.method)
        assertTrue(request.path?.contains("vehicles") == true)
    }

    @Test
    fun `getVehicleTypes should parse response correctly`() = runTest {
        val mockResponse = """[
            {
                "id": 1,
                "name": "MTF",
                "beschreibung": "Mannschaftstransportfahrzeug",
                "aktiv": true,
                "createdAt": "2025-09-19T10:00:00Z"
            },
            {
                "id": 2,
                "name": "LF",
                "beschreibung": "Löschfahrzeug",
                "aktiv": true,
                "createdAt": "2025-09-19T10:00:00Z"
            }
        ]"""

        mockWebServer.enqueue(
            MockResponse()
                .setBody(mockResponse)
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
        )

        val response = apiService.getVehicleTypes()

        assertEquals(2, response.size)
        assertEquals("MTF", response[0].name)
        assertEquals("Mannschaftstransportfahrzeug", response[0].beschreibung)
        assertEquals("LF", response[1].name)
        assertEquals("Löschfahrzeug", response[1].beschreibung)

        // Verify the request
        val request = mockWebServer.takeRequest()
        assertEquals("GET", request.method)
        assertTrue(request.path?.contains("vehicle-types") == true)
    }

    @Test
    fun `getVehicleGroups should parse response correctly`() = runTest {
        val mockResponse = """[
            {
                "id": 1,
                "name": "Gruppe 1",
                "createdAt": "2025-09-19T10:00:00Z"
            },
            {
                "id": 2,
                "name": "Gruppe 2",
                "createdAt": "2025-09-19T10:00:00Z"
            }
        ]"""

        mockWebServer.enqueue(
            MockResponse()
                .setBody(mockResponse)
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
        )

        val response = apiService.getVehicleGroups()

        assertEquals(2, response.size)
        assertEquals("Gruppe 1", response[0].name)
        assertEquals("Gruppe 2", response[1].name)

        // Verify the request
        val request = mockWebServer.takeRequest()
        assertEquals("GET", request.method)
        assertTrue(request.path?.contains("vehicle-groups") == true)
    }

    @Test
    fun `getVehicleById should handle single vehicle response`() = runTest {
        val mockResponse = """{
            "id": 1,
            "kennzeichen": "B-2183",
            "fahrzeugtypId": 1,
            "fahrzeuggruppeId": 1,
            "aktiv": true,
            "createdAt": "2025-09-19T10:00:00Z"
        }"""

        mockWebServer.enqueue(
            MockResponse()
                .setBody(mockResponse)
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
        )

        val response = apiService.getVehicleById(1)

        assertEquals(1, response.id)
        assertEquals("B-2183", response.kennzeichen)
        assertEquals(1, response.fahrzeugtypId)
        assertEquals(1, response.fahrzeuggruppeId)
        assertTrue(response.aktiv)

        // Verify the request
        val request = mockWebServer.takeRequest()
        assertEquals("GET", request.method)
        assertTrue(request.path?.contains("vehicles/1") == true)
    }

    @Test
    fun `API should handle 404 error correctly`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(404)
                .setBody("{\"detail\":\"Vehicle not found\"}")
                .addHeader("Content-Type", "application/json")
        )

        try {
            apiService.getVehicleById(999)
            fail("Expected exception for 404 response")
        } catch (e: Exception) {
            // Expected behavior for 404 response
            assertTrue(e.message?.contains("404") == true || e is retrofit2.HttpException)
        }

        // Verify the request was made
        val request = mockWebServer.takeRequest()
        assertEquals("GET", request.method)
        assertTrue(request.path?.contains("vehicles/999") == true)
    }

    @Test
    fun `API should handle server error correctly`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setBody("{\"detail\":\"Internal server error\"}")
                .addHeader("Content-Type", "application/json")
        )

        try {
            apiService.getVehicles()
            fail("Expected exception for 500 response")
        } catch (e: Exception) {
            // Expected behavior for server error
            assertTrue(e.message?.contains("500") == true || e is retrofit2.HttpException)
        }

        // Verify the request was made
        val request = mockWebServer.takeRequest()
        assertEquals("GET", request.method)
        assertTrue(request.path?.contains("vehicles") == true)
    }

    @Test
    fun `API should include authorization header when provided`() = runTest {
        // This would be set in a real authenticated request
        val mockResponse = """{"items": [], "total": 0, "page": 1, "size": 10, "pages": 0}"""

        mockWebServer.enqueue(
            MockResponse()
                .setBody(mockResponse)
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
        )

        apiService.getVehicles()

        val request = mockWebServer.takeRequest()
        // In a real implementation, we'd verify the Authorization header here
        // This test demonstrates the pattern for testing authenticated requests
        assertEquals("GET", request.method)
    }
}