package com.zx3022448.phpdebugtools.execution

import com.zx3022448.phpdebugtools.diagnostics.CommandRunner
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.isRegularFile
import kotlin.io.path.name

/**
 * 探测本机可用的 PHP 解释器，优先覆盖 Windows 下常见的安装与别名场景。
 */
class PhpRuntimeDetector(
    private val commandRunner: CommandRunner,
) {
    fun detect(): List<DetectedPhpRuntime> {
        val candidates = linkedMapOf<String, String>()
        collectFromWhere(candidates)
        collectFromKnownDirectories(candidates)

        return candidates.keys
            .mapNotNull { command -> inspectRuntime(command, candidates[command].orEmpty()) }
            .sortedWith(compareByDescending<DetectedPhpRuntime> { versionWeight(it.version) }.thenBy { it.command.lowercase() })
    }

    private fun collectFromWhere(candidates: MutableMap<String, String>) {
        val aliases = listOf("php", "php7", "php70", "php71", "php72", "php73", "php74", "php80", "php81", "php82", "php83", "php84")
        aliases.forEach { alias ->
            val result = runCatching {
                commandRunner.run(listOf("where", alias), null)
            }.getOrNull() ?: return@forEach
            if (result.exitCode != 0) {
                return@forEach
            }
            result.stdout.lineSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() && isExecutablePhpCandidate(it) }
                .forEach { path -> candidates.putIfAbsent(path, "PATH") }
        }
    }

    private fun collectFromKnownDirectories(candidates: MutableMap<String, String>) {
        val roots = listOf(
            Path.of("D:/phpstudy_pro/Extensions"),
            Path.of("D:/Program Files/PhpWebStudy-Data/env"),
            Path.of("D:/Program Files/PhpWebStudy-Data"),
            Path.of("D:/php"),
            Path.of("C:/php"),
            Path.of("C:/Program Files"),
        )

        roots.filter { Files.exists(it) }.forEach { root ->
            runCatching {
                Files.walk(root, 4).use { paths ->
                    paths.filter { path ->
                        path.isRegularFile() && path.name.equals("php.exe", ignoreCase = true)
                    }.forEach { path ->
                        candidates.putIfAbsent(path.absolutePathString(), "scan")
                    }
                }
            }
        }
    }

    private fun inspectRuntime(command: String, source: String): DetectedPhpRuntime? {
        if (!isExecutablePhpCandidate(command)) {
            return null
        }
        val result = runCatching {
            commandRunner.run(listOf(command, "-r", "echo PHP_VERSION;"), null)
        }.getOrNull() ?: return null
        if (result.exitCode != 0) {
            return null
        }
        val version = result.stdout.trim().ifBlank { parseVersionFromBanner(command) }
        return DetectedPhpRuntime(
            command = command,
            version = version,
            source = source,
        )
    }

    private fun parseVersionFromBanner(command: String): String {
        val banner = runCatching {
            commandRunner.run(listOf(command, "-v"), null)
        }.getOrNull()?.stdout.orEmpty()
        return Regex("PHP\\s+([0-9]+(?:\\.[0-9]+){1,2})")
            .find(banner)
            ?.groupValues
            ?.getOrNull(1)
            .orEmpty()
    }

    private fun isExecutablePhpCandidate(command: String): Boolean {
        val normalized = command.trim().lowercase()
        if (normalized.endsWith(".bat") || normalized.endsWith(".cmd")) {
            return false
        }
        return true
    }

    private fun versionWeight(version: String): Long {
        val parts = version.split('.')
            .mapNotNull { it.toLongOrNull() }
        if (parts.isEmpty()) {
            return 0L
        }
        val major = parts.getOrElse(0) { 0L }
        val minor = parts.getOrElse(1) { 0L }
        val patch = parts.getOrElse(2) { 0L }
        return major * 1_000_000L + minor * 1_000L + patch
    }
}
