package com.zx3022448.phpdebugtools.diagnostics

data class CommandResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
)

fun interface CommandRunner {
    fun run(command: List<String>, workingDirectory: String?): CommandResult
}
