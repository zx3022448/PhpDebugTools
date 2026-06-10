package com.example.phpdebugtools.execution

import com.example.phpdebugtools.diagnostics.CommandRunner
import java.nio.file.Path

class RuntimeExecutor(
    private val commandRunner: CommandRunner,
) {
    fun run(command: List<String>, projectRoot: Path): DebugExecutionResult {
        val commandResult = commandRunner.run(command, projectRoot.toString())
        val stdoutResult = parseStructuredResult(commandResult.stdout)
        val stderrResult = parseStructuredResult(commandResult.stderr)

        val structuredResult = when {
            stderrResult != null && stdoutResult == null -> stderrResult
            stdoutResult != null && stderrResult == null -> stdoutResult
            stdoutResult != null -> stdoutResult
            else -> null
        }

        if (structuredResult != null) {
            return structuredResult
        }

        val fallbackOutput = commandResult.stderr.takeIf { it.isNotBlank() }
            ?: commandResult.stdout

        return DebugExecutionResult(
            status = if (commandResult.exitCode == 0) "ok" else "error",
            stage = "runtime",
            message = "",
            rawOutput = fallbackOutput,
        )
    }

    private fun parseStructuredResult(output: String): DebugExecutionResult? {
        if (output.isBlank()) {
            return null
        }

        val status = extract(output, "status") ?: return null
        val stage = extract(output, "stage") ?: return null
        return DebugExecutionResult(
            status = status,
            stage = stage,
            message = extract(output, "message") ?: "",
            rawOutput = output,
        )
    }

    private fun extract(json: String, field: String): String? {
        val pattern = Regex(""""$field"\s*:\s*"((?:\\.|[^"\\])*)"""")
        val match = pattern.find(json) ?: return null
        return match.groupValues[1]
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
    }
}
