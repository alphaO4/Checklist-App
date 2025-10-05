package com.feuerwehr.checklist.simple

import org.junit.Test
import org.junit.Assert.*

/**
 * Simple test to verify testing framework is working
 */
class SimpleTest {

    @Test
    fun `simple test should pass`() {
        val result = 2 + 2
        assertEquals(4, result)
    }

    @Test
    fun `string concatenation should work`() {
        val str1 = "Hello"
        val str2 = "World"
        val result = "$str1 $str2"
        assertEquals("Hello World", result)
    }

    @Test
    fun `boolean logic should work`() {
        assertTrue(true)
        assertFalse(false)
        assertNotNull("not null")
        assertNull(null)
    }
}