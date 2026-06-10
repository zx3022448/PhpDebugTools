package com.example.phpdebugtools.diagnostics

class EnvironmentDiagnosticService(
    private val commandRunner: CommandRunner,
) {

    fun inspect(phpExecutable: String): List<DiagnosticFinding> {
        val moduleResult = commandRunner.run(listOf(phpExecutable, "-m"), null)
        if (moduleResult.isFailure()) {
            return listOf(commandFailureFinding("php -m", moduleResult))
        }

        val hasXdebug = moduleResult.stdout
            .lineSequence()
            .map { it.trim() }
            .any { it.equals("xdebug", ignoreCase = true) }

        if (hasXdebug) {
            return emptyList()
        }

        val iniResult = commandRunner.run(listOf(phpExecutable, "--ini"), null)
        if (iniResult.isFailure()) {
            return listOf(commandFailureFinding("php --ini", iniResult))
        }

        val iniLocation = iniResult.stdout
            .lineSequence()
            .map { it.trim() }
            .firstOrNull { it.startsWith("Loaded Configuration File:", ignoreCase = true) }
            ?.substringAfter(':')
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: "php.ini"

        return listOf(
            DiagnosticFinding(
                stage = DiagnosticStage.PHP_XDEBUG,
                severity = "error",
                message = "PHP CLI 未检测到 Xdebug 扩展，请确认当前解释器已启用 Xdebug。",
                hint = "请检查 $iniLocation 中的 Xdebug 配置，确保 php -m 能看到 xdebug 模块。",
            ),
        )
    }

    private fun commandFailureFinding(command: String, result: CommandResult): DiagnosticFinding {
        val details = result.stderr.ifBlank { "exitCode=${result.exitCode}" }
        return DiagnosticFinding(
            stage = DiagnosticStage.PHP_XDEBUG,
            severity = "error",
            message = "$command 执行失败，暂时无法完成 PHP/Xdebug 环境诊断。",
            hint = "请先确认命令可正常执行。错误信息：$details",
        )
    }

    private fun CommandResult.isFailure(): Boolean = exitCode != 0
}
