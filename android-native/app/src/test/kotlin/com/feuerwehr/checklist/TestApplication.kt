package com.feuerwehr.checklist

/**
 * Custom test application for testing
 * Note: HiltTestApplication is for Android tests, not unit tests
 */

/**
 * Base test class providing common test utilities
 */
abstract class BaseTest {
    
    /**
     * Helper function to create test timestamps in ISO format
     */
    protected fun createTestTimestamp(): String {
        return "2025-09-19T10:00:00Z"
    }

    /**
     * Helper function to assert exceptions with specific messages
     */
    protected inline fun <reified T : Exception> assertThrows(
        expectedMessage: String? = null,
        block: () -> Unit
    ) {
        try {
            block()
            throw AssertionError("Expected ${T::class.simpleName} but no exception was thrown")
        } catch (e: Exception) {
            if (e !is T) {
                throw AssertionError("Expected ${T::class.simpleName} but got ${e::class.simpleName}: ${e.message}")
            }
            if (expectedMessage != null && !e.message.orEmpty().contains(expectedMessage)) {
                throw AssertionError("Expected exception message to contain '$expectedMessage' but got '${e.message}'")
            }
        }
    }

    /**
     * Helper function to wait for async operations in tests
     */
    protected suspend fun waitForAsync(timeoutMs: Long = 1000L) {
        kotlinx.coroutines.delay(timeoutMs)
    }

    /**
     * Helper function to create deterministic test IDs
     */
    protected fun generateTestId(prefix: String = "test"): Int {
        return "$prefix${System.currentTimeMillis()}".hashCode().let { 
            if (it < 0) -it else it 
        }
    }
}