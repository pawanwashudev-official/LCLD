package de.nulide.findmydevice.commands

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class CommandParserTest {

    // All the possible branches in the CommandParser should have a test

    @Test
    fun testEmpty() = runTest {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext

        val cmds = availableCommands(appContext)
        val helpCommand = HelpCommand(cmds, appContext)

        val parser = CommandParser("fmd", "", helpCommand, availableCommands(appContext))
        val actual = parser.parse("")

        assertTrue(actual is ParserResult.Empty)
    }

    @Test
    fun testTriggerWordMismatch() = runTest {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext

        val cmds = availableCommands(appContext)
        val helpCommand = HelpCommand(cmds, appContext)

        val parser = CommandParser("fmd", "", helpCommand, availableCommands(appContext))
        val actual = parser.parse("fmdev locate")
        val expected = ParserResult.TriggerWordMismatch("fmdev", "fmd")

        assertEquals(expected, actual)
    }

    @Test
    fun testUnknownCommand() = runTest {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext

        val cmds = availableCommands(appContext)
        val helpCommand = HelpCommand(cmds, appContext)

        val parser = CommandParser("fmd", "", helpCommand, availableCommands(appContext))
        val actual = parser.parse("fmd nonexistent")
        val expected = ParserResult.UnknownCommand("nonexistent")

        assertEquals(expected, actual)
    }

    @Test
    fun testBasic() = runTest {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext

        val cmds = availableCommands(appContext)
        val helpCommand = HelpCommand(cmds, appContext)

        val parser = CommandParser("fmd", "", helpCommand, availableCommands(appContext))
        val actual = parser.parse("fmd locate gps")

        assertTrue(actual is ParserResult.Success)
        actual as ParserResult.Success

        assertEquals("locate", actual.command.keyword)
        assertEquals(listOf("gps"), actual.args)
        assertEquals(null, actual.pin)
    }

    @Test
    fun testPin() = runTest {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext

        val cmds = availableCommands(appContext)
        val helpCommand = HelpCommand(cmds, appContext)

        val expectedPin = "horse battery staple"
        val parser =
            CommandParser("fmd", expectedPin, helpCommand, availableCommands(appContext))
        val actual = parser.parse("""fmd "horse battery staple" ring""")

        assertTrue(actual is ParserResult.Success)
        actual as ParserResult.Success

        assertEquals("ring", actual.command.keyword)
        assertEquals(emptyList<String>(), actual.args)
        assertEquals("horse battery staple", actual.pin)
    }

    @Test
    fun testHelp() = runTest {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext

        val cmds = availableCommands(appContext)
        val helpCommand = HelpCommand(cmds, appContext)

        val parser = CommandParser("fmd", "", helpCommand, availableCommands(appContext))
        val actual = parser.parse("fmd") // no command

        assertTrue(actual is ParserResult.Success)
        actual as ParserResult.Success

        assertEquals("help", actual.command.keyword)
        assertEquals(emptyList<String>(), actual.args)
        assertEquals(null, actual.pin)
    }

    @Test
    fun testHelpWithPin() = runTest {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext

        val cmds = availableCommands(appContext)
        val helpCommand = HelpCommand(cmds, appContext)

        val expectedPin = "horse battery staple"
        val parser =
            CommandParser("fmd", expectedPin, helpCommand, availableCommands(appContext))
        val actual = parser.parse("""fmd "horse battery staple" """) // no command, just pin

        assertTrue(actual is ParserResult.Success)
        actual as ParserResult.Success

        assertEquals("help", actual.command.keyword)
        assertEquals(emptyList<String>(), actual.args)
        assertEquals("horse battery staple", actual.pin)
    }

    @Test
    fun testCaseInsensitivity() = runTest {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext

        val cmds = availableCommands(appContext)
        val helpCommand = HelpCommand(cmds, appContext)

        val parser = CommandParser("fmd", "", helpCommand, availableCommands(appContext))
        val actual = parser.parse("FMD LoCaTe gps")

        assertTrue(actual is ParserResult.Success)
        actual as ParserResult.Success

        assertEquals("locate", actual.command.keyword)
        assertEquals(listOf("gps"), actual.args)
        assertEquals(null, actual.pin)
    }

}
