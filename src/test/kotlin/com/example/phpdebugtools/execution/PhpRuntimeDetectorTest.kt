package com.example.phpdebugtools.execution

import com.example.phpdebugtools.diagnostics.CommandResult
import com.example.phpdebugtools.diagnostics.CommandRunner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PhpRuntimeDetectorTest {
    @Test
    fun `探测 PHP 时过滤 Windows bat 和 cmd 包装脚本`() {
        val runner = CommandRunner { command, _ ->
            when {
                command == listOf("where", "php") -> CommandResult(
                    exitCode = 0,
                    stdout = "C:/tools/php/php.bat\nC:/tools/php/php.cmd\nC:/tools/php/php.exe\n",
                    stderr = "",
                )
                command.firstOrNull()?.endsWith("php.exe") == true -> CommandResult(
                    exitCode = 0,
                    stdout = "8.3.12",
                    stderr = "",
                )
                command.firstOrNull()?.startsWith("where") == true -> CommandResult(1, "", "")
                else -> CommandResult(1, "", "")
            }
        }

        val runtimes = PhpRuntimeDetector(runner).detect()

        assertEquals(true, runtimes.any { it.command == "C:/tools/php/php.exe" })
        assertFalse(runtimes.any { it.command.endsWith(".bat") || it.command.endsWith(".cmd") })
    }
}
