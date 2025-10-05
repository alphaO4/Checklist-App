package com.feuerwehr.checklist

import org.junit.Test
import org.junit.Assert.*

/**
 * Basic test to verify testing framework is working
 * This establishes the foundation for comprehensive testing
 */
class BasicTestSuite {

    @Test
    fun `framework verification test`() {
        val result = 2 + 2
        assertEquals(4, result)
    }

    @Test
    fun `string operations test`() {
        val vehicle = "MTF"
        val number = "B-2183"
        val combined = "$vehicle-$number"
        
        assertEquals("MTF-B-2183", combined)
        assertTrue(combined.contains("MTF"))
        assertTrue(combined.contains("B-2183"))
    }

    @Test
    fun `collection operations test`() {
        val vehicles = listOf("MTF", "LF", "RTB")
        
        assertEquals(3, vehicles.size)
        assertTrue(vehicles.contains("MTF"))
        assertFalse(vehicles.contains("TLF"))
        assertEquals("MTF", vehicles.first())
    }

    @Test
    fun `nullable values test`() {
        val value: String? = null
        val nonNullValue: String? = "test"
        
        assertNull(value)
        assertNotNull(nonNullValue)
        assertEquals("test", nonNullValue)
    }
}