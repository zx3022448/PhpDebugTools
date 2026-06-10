package com.example.phpdebugtools.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EnvironmentDiagnosticServiceTest {

    @Test
    fun reportsMissingXdebugModuleWithIniPathHint() {
        val runner = object : CommandRunner {
            override fun run(command: List<String>, workingDirectory: String?): CommandResult {
                return when (command.joinToString(" ")) {
                    "php -m" -> CommandResult(0, "Core\njson\n", "")
                    "php --ini" -> CommandResult(0, "Loaded Configuration File: C:\\php\\php.ini", "")
                    else -> CommandResult(1, "", "unsupported")
                }
            }
        }

        val findings = EnvironmentDiagnosticService(runner).inspect("php")

        val finding = findings.single()
        assertEquals(DiagnosticStage.PHP_XDEBUG, finding.stage)
        assertTrue(finding.message.contains("Xdebug"))
        assertTrue(finding.hint.contains("C:\\php\\php.ini"))
    }

    @Test
    fun returnsEmptyListWhenXdebugModuleExists() {
        val runner = object : CommandRunner {
            override fun run(command: List<String>, workingDirectory: String?): CommandResult {
                return when (command.joinToString(" ")) {
                    "php -m" -> CommandResult(0, "Core\nxdebug\njson\n", "")
                    "php --ini" -> CommandResult(0, "Loaded Configuration File: C:\\php\\php.ini", "")
                    else -> CommandResult(1, "", "unsupported")
                }
            }
        }

        val findings = EnvironmentDiagnosticService(runner).inspect("php")

        assertTrue(findings.isEmpty())
    }

    @Test
    fun ignoresStderrWarningsWhenPhpModulesCommandSucceeds() {
        val runner = object : CommandRunner {
            override fun run(command: List<String>, workingDirectory: String?): CommandResult {
                return when (command.joinToString(" ")) {
                    "php -m" -> CommandResult(0, "Core\nxdebug\njson\n", "PHP Warning: module startup notice")
                    "php --ini" -> CommandResult(0, "Loaded Configuration File: C:\\php\\php.ini", "")
                    else -> CommandResult(1, "", "unsupported")
                }
            }
        }

        val findings = EnvironmentDiagnosticService(runner).inspect("php")

        assertTrue(findings.isEmpty())
    }

    @Test
    fun keepsIniPathHintWhenIniCommandSucceedsWithWarnings() {
        val runner = object : CommandRunner {
            override fun run(command: List<String>, workingDirectory: String?): CommandResult {
                return when (command.joinToString(" ")) {
                    "php -m" -> CommandResult(0, "Core\njson\n", "PHP Warning: module startup notice")
                    "php --ini" -> CommandResult(0, "Loaded Configuration File: C:\\php\\php.ini", "Deprecated: legacy extension")
                    else -> CommandResult(1, "", "unsupported")
                }
            }
        }

        val findings = EnvironmentDiagnosticService(runner).inspect("php")

        val finding = findings.single()
        assertTrue(finding.message.contains("未检测到 Xdebug"))
        assertTrue(finding.hint.contains("C:\\php\\php.ini"))
    }

    @Test
    fun reportsCommandFailureWithoutMisleadingXdebugHint() {
        val runner = object : CommandRunner {
            override fun run(command: List<String>, workingDirectory: String?): CommandResult {
                return when (command.joinToString(" ")) {
                    "php -m" -> CommandResult(1, "", "php command failed")
                    "php --ini" -> CommandResult(0, "Loaded Configuration File: C:\\php\\php.ini", "")
                    else -> CommandResult(1, "", "unsupported")
                }
            }
        }

        val findings = EnvironmentDiagnosticService(runner).inspect("php")

        val finding = findings.single()
        assertEquals(DiagnosticStage.PHP_XDEBUG, finding.stage)
        assertFalse(finding.message.contains("未检测到 Xdebug"))
        assertFalse(finding.hint.contains("检查 C:\\php\\php.ini"))
        assertFalse(finding.hint.contains("php.ini"))
        assertTrue(finding.message.contains("执行失败"))
    }
}
