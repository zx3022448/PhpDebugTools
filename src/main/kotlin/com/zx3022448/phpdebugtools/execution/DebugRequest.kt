package com.zx3022448.phpdebugtools.execution

import java.nio.file.Path

sealed class DebugRequest {
    data class Cli(
        val phpExecutable: String,
        val projectRoot: Path,
        val entryScript: String,
        val payloadPath: Path,
    ) : DebugRequest()
}
