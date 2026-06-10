package com.example.phpdebugtools.execution

import com.example.phpdebugtools.diagnostics.CommandResult
import com.example.phpdebugtools.diagnostics.CommandRunner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Paths

class RuntimeExecutorServiceTest {
    @Test
    fun buildsServicePayloadJson() {
        val payload = RuntimeJson.servicePayload(
            classFqn = "\\app\\service\\UserService",
            methodName = "load",
            isStatic = false,
            argsJson = """["42"]""",
        )

        assertEquals(
            """{"type":"service","class":"\\app\\service\\UserService","method":"load","static":false,"args":["42"]}""",
            payload,
        )
    }

    @Test
    fun parsesRuntimeJsonForServiceExecution() {
        val runner = CommandRunner { _, _ ->
            CommandResult(0, """{"status":"ok","stage":"target","message":"service invoked"}""", "")
        }

        val result = RuntimeExecutor(runner).run(
            command = listOf("php", "invoke-service.php", "payload.json"),
            projectRoot = Paths.get("D:/demo")
        )

        assertEquals("ok", result.status)
        assertEquals("target", result.stage)
        assertEquals("service invoked", result.message)
    }

    @Test
    fun prefersStructuredStderrWhenStdoutIsNoise() {
        val runner = CommandRunner { _, _ ->
            CommandResult(
                1,
                "PHP Warning: banner output",
                """{"status":"error","stage":"target","message":"service failed"}""",
            )
        }

        val result = RuntimeExecutor(runner).run(
            command = listOf("php", "invoke-service.php", "payload.json"),
            projectRoot = Paths.get("D:/demo")
        )

        assertEquals("error", result.status)
        assertEquals("target", result.stage)
        assertEquals("service failed", result.message)
        assertEquals("""{"status":"error","stage":"target","message":"service failed"}""", result.rawOutput)
    }

    @Test
    fun fallsBackWhenNoStructuredJsonExists() {
        val runner = CommandRunner { _, _ ->
            CommandResult(2, "plain stdout", "plain stderr")
        }

        val result = RuntimeExecutor(runner).run(
            command = listOf("php", "invoke-service.php", "payload.json"),
            projectRoot = Paths.get("D:/demo")
        )

        assertEquals("error", result.status)
        assertEquals("runtime", result.stage)
        assertEquals("", result.message)
        assertEquals("plain stderr", result.rawOutput)
    }

    @Test
    fun rejectsInvalidArgsJson() {
        val error = try {
            RuntimeJson.servicePayload(
                classFqn = "\\app\\service\\UserService",
                methodName = "load",
                isStatic = false,
                argsJson = """[oops]""",
            )
            null
        } catch (exception: IllegalArgumentException) {
            exception
        }

        assertTrue(error != null)
        assertTrue(error!!.message!!.contains("argsJson"))
    }
}
