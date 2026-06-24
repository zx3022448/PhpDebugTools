package com.zx3022448.phpdebugtools.execution

import com.zx3022448.phpdebugtools.diagnostics.CommandResult
import com.zx3022448.phpdebugtools.diagnostics.CommandRunner
import java.nio.charset.StandardCharsets
import java.nio.file.Path

class ProcessCommandRunner : CommandRunner {
    override fun run(command: List<String>, workingDirectory: String?): CommandResult {
        val process = ProcessBuilder(command)
            .apply {
                if (workingDirectory != null) {
                    directory(Path.of(workingDirectory).toFile())
                }
            }
            .start()

        val stdout = process.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
        val stderr = process.errorStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
        val exitCode = process.waitFor()
        return CommandResult(exitCode = exitCode, stdout = stdout, stderr = stderr)
    }
}
