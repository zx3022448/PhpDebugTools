package com.example.phpdebugtools.execution

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Paths

class CliDebugCommandBuilderTest {

    @Test
    fun buildsCliInvokeServiceCommand() {
        val phpExecutable = "php"
        val projectRoot = Paths.get("D:/demo")
        val entryScript = "invoke-service.php"
        val payloadPath = Paths.get("D:/demo/.php-debug-tools/payload.json")

        val command = CliDebugCommandBuilder.build(
            phpExecutable = phpExecutable,
            projectRoot = projectRoot,
            entryScript = entryScript,
            payloadPath = payloadPath,
        )

        assertEquals(
            listOf(
                "php",
                "D:/demo/.php-debug-tools/invoke-service.php",
                "D:/demo/.php-debug-tools/payload.json",
            ),
            command,
        )
    }

    @Test
    fun rejectsUnknownEntryScript() {
        val error = try {
            CliDebugCommandBuilder.build(
                phpExecutable = "php",
                projectRoot = Paths.get("D:/demo"),
                entryScript = "../evil.php",
                payloadPath = Paths.get("D:/demo/.php-debug-tools/payload.json"),
            )
            null
        } catch (exception: IllegalArgumentException) {
            exception
        }

        assertTrue(error != null)
        assertTrue(error!!.message!!.contains("../evil.php"))
    }
}
