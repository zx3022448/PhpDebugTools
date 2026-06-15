package com.example.phpdebugtools.execution

import com.example.phpdebugtools.diagnostics.CommandRunner
import com.example.phpdebugtools.methods.MethodDebugTarget
import com.example.phpdebugtools.methods.MethodKind
import com.example.phpdebugtools.project.ThinkPhpProjectDetector
import com.example.phpdebugtools.runtime.RuntimeInstallOptions
import com.example.phpdebugtools.runtime.RuntimeInstaller
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

data class MethodInvokeRequest(
    val projectRoot: Path,
    val phpExecutable: String,
    val target: MethodDebugTarget,
    val argsJson: String = "[]",
    val requestMethod: String = "GET",
    val queryJson: String = "{}",
    val headerJson: String = "{}",
    val bodyMode: String = "none",
    val bodyJson: String = "{}",
)

class MethodInvokeExecutor(
    private val commandRunner: CommandRunner,
    private val runtimeInstaller: RuntimeInstaller = RuntimeInstaller(),
) {
    fun execute(request: MethodInvokeRequest): DebugExecutionResult {
        val detection = detectProject(request.projectRoot)
        runtimeInstaller.install(
            request.projectRoot,
            RuntimeInstallOptions(
                frameworkAdapter = detection.majorVersion?.let { "thinkphp$it" },
                entryFile = detection.entryFile,
            ),
        )

        val payloadPath = request.projectRoot
            .resolve(RuntimeInstaller.RUNTIME_DIR_NAME)
            .resolve("toolwindow-payload.json")
        Files.createDirectories(payloadPath.parent)
        Files.writeString(payloadPath, buildPayload(request), StandardCharsets.UTF_8)

        val entryScript = when (request.target.kind) {
            MethodKind.CONTROLLER -> "invoke-controller.php"
            MethodKind.SERVICE -> "invoke-service.php"
        }
        val command = CliDebugCommandBuilder.build(
            phpExecutable = request.phpExecutable,
            projectRoot = request.projectRoot,
            entryScript = entryScript,
            payloadPath = payloadPath,
        )
        return RuntimeExecutor(commandRunner).run(command, request.projectRoot)
    }

    private fun buildPayload(request: MethodInvokeRequest): String {
        return when (request.target.kind) {
            MethodKind.CONTROLLER -> RuntimeJson.controllerPayload(
                classFqn = request.target.classFqn,
                methodName = request.target.methodName,
                isStatic = request.target.isStatic,
                requestMethod = request.requestMethod,
                queryJson = request.queryJson,
                headerJson = request.headerJson,
                bodyMode = request.bodyMode,
                bodyJson = request.bodyJson,
                argsJson = request.argsJson,
            )

            MethodKind.SERVICE -> RuntimeJson.servicePayload(
                classFqn = request.target.classFqn,
                methodName = request.target.methodName,
                isStatic = request.target.isStatic,
                argsJson = request.argsJson,
            )
        }
    }

    private fun detectProject(projectRoot: Path) = ThinkPhpProjectDetector.detect(
        composerJson = readProjectFile(projectRoot.resolve("composer.json")),
        installedFrameworkVersion = null,
        entryFileText = readProjectFile(projectRoot.resolve("public/index.php")),
        knownPaths = collectKnownPaths(projectRoot),
    )

    private fun readProjectFile(path: Path): String? {
        return if (Files.isRegularFile(path)) {
            Files.readString(path)
        } else {
            null
        }
    }

    private fun collectKnownPaths(projectRoot: Path): Set<String> {
        if (!Files.exists(projectRoot)) {
            return emptySet()
        }

        val knownPaths = mutableSetOf<String>()
        Files.walk(projectRoot).use { paths ->
            paths.forEach { path ->
                if (path != projectRoot) {
                    knownPaths += projectRoot.relativize(path).toString().replace('\\', '/')
                }
            }
        }
        return knownPaths
    }
}
