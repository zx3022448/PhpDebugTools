package com.example.phpdebugtools.execution

import com.example.phpdebugtools.runtime.RuntimeInstaller
import kotlin.io.path.invariantSeparatorsPathString
import java.nio.file.Path

object CliDebugCommandBuilder {
    private val allowedEntryScripts = setOf(
        "invoke-service.php",
        "invoke-controller.php",
        "debug-web-entry.php",
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
            entryScriptPath.invariantSeparatorsPathString,
            payloadPath.invariantSeparatorsPathString,
        )
    }
}
