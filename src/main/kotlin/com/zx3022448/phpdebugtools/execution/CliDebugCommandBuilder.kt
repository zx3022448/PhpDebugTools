package com.zx3022448.phpdebugtools.execution

import com.zx3022448.phpdebugtools.runtime.RuntimeInstaller
import kotlin.io.path.invariantSeparatorsPathString
import java.nio.file.Path

object CliDebugCommandBuilder {
    private val allowedEntryScripts = setOf(
        "invoke-service.php",
        "invoke-controller.php",
    )
    private val xdebugCliFlags = listOf(
        "-dxdebug.mode=debug",
        "-dxdebug.start_with_request=yes",
        "-dxdebug.remote_enable=1",
        "-dxdebug.remote_autostart=1",
    )

    fun build(
        phpExecutable: String,
        projectRoot: Path,
        entryScript: String,
        payloadPath: Path,
    ): List<String> {
        require(entryScript in allowedEntryScripts) {
            "Unsupported entry script: $entryScript"
        }

        val entryScriptPath = projectRoot
            .resolve(RuntimeInstaller.RUNTIME_DIR_NAME)
            .resolve(entryScript)

        return listOf(
            phpExecutable,
        ) + xdebugCliFlags + listOf(
            entryScriptPath.invariantSeparatorsPathString,
            payloadPath.invariantSeparatorsPathString,
        )
    }
}
