package com.feuerwehr.checklist.data.error

import com.feuerwehr.checklist.domain.exception.*
import retrofit2.HttpException
import retrofit2.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import org.junit.Assert.*
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * Unit tests for ErrorMapper
 * Tests exception mapping and German error message generation
 */
class ErrorMapperTest {

    private val errorMapper = ErrorMapper()

    @Test
    fun `mapException should map IOException to NetworkException`() {
        // Given
        val ioException = IOException("Connection refused")

        // When
        val result = errorMapper.mapException(ioException)

        // Then
        assertTrue("Should return NetworkException", result is NetworkException)
        assertEquals("Should have German user message", 
            "Netzwerkfehler - Überprüfen Sie Ihre Internetverbindung", result.userMessage)
        assertEquals("Should have correct error code", "NETWORK_IO_ERROR", result.errorCode)
    }

    @Test
    fun `mapException should map SocketTimeoutException to NetworkException`() {
        // Given
        val timeoutException = SocketTimeoutException("Read timeout")

        // When
        val result = errorMapper.mapException(timeoutException)

        // Then
        assertTrue("Should return NetworkException", result is NetworkException)
        assertEquals("Should have German timeout message", 
            "Zeitüberschreitung - Der Server antwortet nicht", result.userMessage)
        assertEquals("Should have timeout error code", "NETWORK_TIMEOUT", result.errorCode)
    }

    @Test
    fun `mapException should map UnknownHostException to NetworkException`() {
        // Given
        val hostException = UnknownHostException("Unknown host")

        // When
        val result = errorMapper.mapException(hostException)

        // Then
        assertTrue("Should return NetworkException", result is NetworkException)
        assertEquals("Should have German host error message", 
            "Server nicht erreichbar - Überprüfen Sie die URL", result.userMessage)
        assertEquals("Should have host error code", "NETWORK_HOST_UNKNOWN", result.errorCode)
    }

    @Test
    fun `mapException should map SSLException to NetworkException`() {
        // Given
        val sslException = SSLException("SSL handshake failed")

        // When
        val result = errorMapper.mapException(sslException)

        // Then
        assertTrue("Should return NetworkException", result is NetworkException)
        assertEquals("Should have German SSL error message", 
            "Sichere Verbindung fehlgeschlagen", result.userMessage)
        assertEquals("Should have SSL error code", "NETWORK_SSL_ERROR", result.errorCode)
    }

    @Test
    fun `mapException should map HTTP 401 to AuthenticationException`() {
        // Given
        val response = Response.error<Any>(401, "Unauthorized".toResponseBody())
        val httpException = HttpException(response)

        // When
        val result = errorMapper.mapException(httpException)

        // Then
        assertTrue("Should return AuthenticationException", result is AuthenticationException)
        assertEquals("Should have German auth error message", 
            "Anmeldung fehlgeschlagen - Überprüfen Sie Ihre Zugangsdaten", result.userMessage)
        assertEquals("Should have auth error code", "AUTH_UNAUTHORIZED", result.errorCode)
    }

    @Test
    fun `mapException should map HTTP 403 to AuthenticationException`() {
        // Given
        val response = Response.error<Any>(403, "Forbidden".toResponseBody())
        val httpException = HttpException(response)

        // When
        val result = errorMapper.mapException(httpException)

        // Then
        assertTrue("Should return AuthenticationException", result is AuthenticationException)
        assertEquals("Should have German permission error message", 
            "Keine Berechtigung für diese Aktion", result.userMessage)
        assertEquals("Should have permission error code", "AUTH_FORBIDDEN", result.errorCode)
    }

    @Test
    fun `mapException should map HTTP 400 to ValidationException`() {
        // Given
        val response = Response.error<Any>(400, "Bad Request".toResponseBody())
        val httpException = HttpException(response)

        // When
        val result = errorMapper.mapException(httpException)

        // Then
        assertTrue("Should return ValidationException", result is ValidationException)
        assertEquals("Should have German validation error message", 
            "Ungültige Daten - Überprüfen Sie Ihre Eingaben", result.userMessage)
        assertEquals("Should have validation error code", "VALIDATION_BAD_REQUEST", result.errorCode)
    }

    @Test
    fun `mapException should map HTTP 404 to BusinessLogicException`() {
        // Given
        val response = Response.error<Any>(404, "Not Found".toResponseBody())
        val httpException = HttpException(response)

        // When
        val result = errorMapper.mapException(httpException)

        // Then
        assertTrue("Should return BusinessLogicException", result is BusinessLogicException)
        assertEquals("Should have German not found message", 
            "Ressource nicht gefunden", result.userMessage)
        assertEquals("Should have not found error code", "BUSINESS_NOT_FOUND", result.errorCode)
    }

    @Test
    fun `mapException should map HTTP 409 to SyncException`() {
        // Given
        val response = Response.error<Any>(409, "Conflict".toResponseBody())
        val httpException = HttpException(response)

        // When
        val result = errorMapper.mapException(httpException)

        // Then
        assertTrue("Should return SyncException", result is SyncException)
        assertEquals("Should have German conflict message", 
            "Konflikt bei der Synchronisation - Daten wurden zwischenzeitlich geändert", result.userMessage)
        assertEquals("Should have conflict error code", "SYNC_CONFLICT", result.errorCode)
    }

    @Test
    fun `mapException should map HTTP 500 to NetworkException`() {
        // Given
        val response = Response.error<Any>(500, "Internal Server Error".toResponseBody())
        val httpException = HttpException(response)

        // When
        val result = errorMapper.mapException(httpException)

        // Then
        assertTrue("Should return NetworkException", result is NetworkException)
        assertEquals("Should have German server error message", 
            "Serverfehler - Versuchen Sie es später erneut", result.userMessage)
        assertEquals("Should have server error code", "NETWORK_SERVER_ERROR", result.errorCode)
    }

    @Test
    fun `mapException should handle database exceptions`() {
        // Given
        val dbException = android.database.SQLException("Database is locked")

        // When
        val result = errorMapper.mapException(dbException)

        // Then
        assertTrue("Should return DatabaseException", result is DatabaseException)
        assertEquals("Should have German database error message", 
            "Datenbankfehler - Versuchen Sie es erneut", result.userMessage)
        assertEquals("Should have database error code", "DATABASE_ERROR", result.errorCode)
    }

    @Test
    fun `mapException should handle specific German fire department error messages`() {
        // Test specific fire department related errors
        val testCases = mapOf(
            "Fahrzeug nicht gefunden" to BusinessLogicException::class.java,
            "TÜV-Termin abgelaufen" to ValidationException::class.java,
            "Checklist bereits gestartet" to BusinessLogicException::class.java,
            "Keine Berechtigung für Fahrzeug" to AuthenticationException::class.java
        )

        testCases.forEach { (message, expectedType) ->
            val exception = RuntimeException(message)
            val result = errorMapper.mapException(exception)
            
            assertTrue("Message '$message' should map to ${expectedType.simpleName}", 
                expectedType.isInstance(result))
            assertTrue("Should have German user message", 
                result.userMessage.isNotEmpty())
        }
    }

    @Test
    fun `mapException should preserve original exception as cause`() {
        // Given
        val originalException = IOException("Original error")

        // When
        val result = errorMapper.mapException(originalException)

        // Then
        assertEquals("Should preserve original exception as cause", 
            originalException, result.cause)
    }

    @Test
    fun `mapException should handle null messages gracefully`() {
        // Given
        val exceptionWithNullMessage = RuntimeException(null as String?)

        // When
        val result = errorMapper.mapException(exceptionWithNullMessage)

        // Then
        assertNotNull("Should have non-null user message", result.userMessage)
        assertNotNull("Should have non-null error code", result.errorCode)
        assertTrue("User message should not be empty", result.userMessage.isNotEmpty())
    }

    @Test
    fun `mapByMessage should correctly categorize German fire department terms`() {
        val testMessages = mapOf(
            "Fahrzeug nicht verfügbar" to BusinessLogicException::class.java,
            "Benutzer nicht berechtigt" to AuthenticationException::class.java,
            "Kennzeichen ungültig" to ValidationException::class.java,
            "Checklist nicht gefunden" to BusinessLogicException::class.java,
            "TÜV-Datum in der Vergangenheit" to ValidationException::class.java,
            "Gruppe hat keine Fahrzeuge" to BusinessLogicException::class.java
        )

        testMessages.forEach { (message, expectedType) ->
            val result = errorMapper.mapByMessage(message)
            
            assertTrue("Message '$message' should be categorized as ${expectedType.simpleName}", 
                expectedType.isInstance(result))
        }
    }
}