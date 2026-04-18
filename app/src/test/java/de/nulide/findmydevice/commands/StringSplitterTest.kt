package de.nulide.findmydevice.commands

import org.junit.Assert.*
import org.junit.Test


class StringSplitterTest {

    @Test
    fun testSplitEmpty() {
        val actual = splitBySpaceWithQuotes("")
        val expected = emptyList<String>()
        assertEquals(expected, actual)
    }

    @Test
    fun testSplitBasic() {
        val actual = splitBySpaceWithQuotes("fmd mypin locate gps")
        val expected = listOf("fmd", "mypin", "locate", "gps")
        assertEquals(expected, actual)
    }

    @Test
    fun testSplitPinWithQuotes() {
        val actual = splitBySpaceWithQuotes("""fmd "horse battery staple" locate gps""")
        val expected = listOf("fmd", "horse battery staple", "locate", "gps")
        assertEquals(expected, actual)
    }

    @Test
    fun testSplitEverythingWithQuotesAndSpaces() {
        val actual =
            splitBySpaceWithQuotes("""   "fmd"    "horse battery staple"    "locate"   "gps"     """)
        val expected = listOf("fmd", "horse battery staple", "locate", "gps")
        assertEquals(expected, actual)
    }

}
