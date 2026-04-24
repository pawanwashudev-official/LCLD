package com.neubofy.lcld.data

import org.junit.Assert.assertEquals
import org.junit.Test


class BackgroundLocationTypeTest {

    @Test
    fun testBasic() {
        val actual = BackgroundLocationType(0x1000)
        assert(actual.isEmpty())

        actual.gps = true
        assert(!actual.isEmpty())
        assert(!actual.isAll())

        actual.fused = true
        actual.cell = true
        assert(!actual.isEmpty())
        assert(actual.isAll())
    }

    @Test
    fun testEncode() {
        var actual = BackgroundLocationType.fromEmpty()
        assertEquals(0x1000, actual.encode())

        // Single
        actual = BackgroundLocationType.fromEmpty().apply {
            fused = true
        }
        assertEquals(0x1001, actual.encode())

        actual = BackgroundLocationType.fromEmpty().apply {
            gps = true
        }
        assertEquals(0x1002, actual.encode())

        actual = BackgroundLocationType.fromEmpty().apply {
            cell = true
        }
        assertEquals(0x1004, actual.encode())

        // Double
        actual = BackgroundLocationType.fromEmpty().apply {
            fused = true
            gps = true
        }
        assertEquals(0x1003, actual.encode())

        actual = BackgroundLocationType.fromEmpty().apply {
            fused = true
            cell = true
        }
        assertEquals(0x1005, actual.encode())

        actual = BackgroundLocationType.fromEmpty().apply {
            gps = true
            cell = true
        }
        assertEquals(0x1006, actual.encode())

        // All
        actual = BackgroundLocationType.fromEmpty().apply {
            fused = true
            gps = true
            cell = true
        }
        assertEquals(0x1007, actual.encode())
    }

    @Test
    fun testFromOldEncode() {
        var actual = BackgroundLocationType.fromOldEncoding(0)
        assertEquals(0x1002, actual.encode())

        actual = BackgroundLocationType.fromOldEncoding(1)
        assertEquals(0x1004, actual.encode())

        actual = BackgroundLocationType.fromOldEncoding(2)
        assertEquals(0x1006, actual.encode())

        actual = BackgroundLocationType.fromOldEncoding(3)
        assertEquals(0x1000, actual.encode())

    }

}
