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
        assert(actual.isAll())
    }

    @Test
    fun testEncode() {
        var actual = BackgroundLocationType.fromEmpty()
        assertEquals(0x1000, actual.encode())

        // Single
        actual = BackgroundLocationType.fromEmpty().apply {
            gps = true
        }
        assertEquals(0x1002, actual.encode())
    }

    @Test
    fun testFromOldEncode() {
        var actual = BackgroundLocationType.fromOldEncoding(0)
        assertEquals(0x1002, actual.encode())

        actual = BackgroundLocationType.fromOldEncoding(1)
        assertEquals(0x1000, actual.encode())

        actual = BackgroundLocationType.fromOldEncoding(2)
        assertEquals(0x1002, actual.encode())

        actual = BackgroundLocationType.fromOldEncoding(3)
        assertEquals(0x1000, actual.encode())

    }

}
