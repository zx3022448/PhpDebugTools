package com.zx3022448.phpdebugtools.execution

import com.zx3022448.phpdebugtools.diagnostics.CommandRunner
import java.nio.file.Path

class RuntimeExecutor(
    private val commandRunner: CommandRunner,
) {
    fun run(command: List<String>, projectRoot: Path): DebugExecutionResult {
        val commandResult = commandRunner.run(command, projectRoot.toString())
        val stdoutResult = parseStructuredResult(commandResult.stdout, commandResult.stderr)
        val stderrResult = parseStructuredResult(commandResult.stderr, commandResult.stdout)

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
            resultText = "",
            resultType = "",
            consoleText = fallbackOutput,
            exceptionText = "",
            rawOutput = fallbackOutput,
        )
    }

    private fun parseStructuredResult(output: String, pairedOutput: String = ""): DebugExecutionResult? {
        if (output.isBlank()) {
            return null
        }

        val json = extractJsonObject(output) ?: return null
        val status = extract(json, "status") ?: return null
        val stage = extract(json, "stage") ?: return null
        val strayOutput = listOf(
            output.removeJsonObject(json),
            pairedOutput,
        ).filter { it.isNotBlank() }.joinToString(separator = "\n").trim()
        val consoleText = listOf(
            extract(json, "consoleText") ?: "",
            strayOutput,
        ).filter { it.isNotBlank() }.joinToString(separator = "\n")

        return DebugExecutionResult(
            status = status,
            stage = stage,
            message = extract(json, "message") ?: "",
            resultText = extract(json, "resultText") ?: "",
            resultType = extract(json, "resultType") ?: "",
            consoleText = consoleText,
            exceptionText = extract(json, "exceptionText") ?: "",
            rawOutput = listOf(output, pairedOutput).filter { it.isNotBlank() }.joinToString(separator = "\n"),
        )
    }

    private fun extractJsonObject(output: String): String? {
        val start = output.indexOf('{')
        if (start < 0) {
            return null
        }

        var depth = 0
        var inString = false
        var escaped = false
        for (index in start until output.length) {
            val char = output[index]
            if (inString) {
                if (escaped) {
                    escaped = false
                } else if (char == '\\') {
                    escaped = true
                } else if (char == '"') {
                    inString = false
                }
                continue
            }

            when (char) {
                '"' -> inString = true
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) {
                        return output.substring(start, index + 1)
                    }
                }
            }
        }

        return null
    }

    private fun String.removeJsonObject(json: String): String {
        val start = indexOf(json)
        if (start < 0) {
            return this.trim()
        }
        return (substring(0, start) + substring(start + json.length)).trim()
    }

    private fun extract(json: String, field: String): String? {
        val pattern = Regex("\"$field\"\\s*:\\s*(\"((?:\\\\.|[^\"\\\\])*)\"|null)")
        val match = pattern.find(json) ?: return null
        if (match.groupValues[1] == "null") {
            return ""
        }
        return match.groupValues[2]
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace("\\/", "/")
    }
}
