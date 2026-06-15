package com.example.phpdebugtools.runtime

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes

class RuntimeInstaller(
    private val templates: List<RuntimeTemplate> = bundledTemplates(),
) {
    fun install(
        projectRoot: Path,
        options: RuntimeInstallOptions = RuntimeInstallOptions(),
    ): RuntimeInstallResult {
        val projectRealRoot = projectRoot.toRealPath()
        val runtimeRoot = projectRoot.resolve(RUNTIME_DIR_NAME).normalize()
        if (Files.exists(runtimeRoot, LinkOption.NOFOLLOW_LINKS)) {
            validateRuntimeRoot(projectRealRoot, runtimeRoot)
        } else {
            Files.createDirectories(runtimeRoot)
        }

        val installedFiles = templates.map { template ->
            val target = runtimeRoot.resolve(template.relativePath).normalize()
            require(target.startsWith(runtimeRoot)) {
                "Template path '${template.relativePath}' resolves outside runtime directory '$RUNTIME_DIR_NAME'"
            }

            validateExistingPathNodes(runtimeRoot, target.parent)
            Files.createDirectories(target.parent)
            Files.writeString(target, renderTemplate(template, options), StandardCharsets.UTF_8)

            "$RUNTIME_DIR_NAME/${template.relativePath.replace('\\', '/')}"
        }

        return RuntimeInstallResult(
            runtimeRoot = runtimeRoot.toString(),
            installedFiles = installedFiles,
            updated = true,
        )
    }

    companion object {
        const val RUNTIME_DIR_NAME = ".php-debug-tools"

        fun bundledTemplates(): List<RuntimeTemplate> = listOf(
            template("bootstrap.php"),
            template("adapters/thinkphp5.php"),
            template("adapters/thinkphp6.php"),
            template("invoke-service.php"),
            template("invoke-controller.php"),
            template("runtime-config.json"),
        )

        private fun template(relativePath: String): RuntimeTemplate {
            val resourcePath = "runtime/$relativePath"
            val contents = requireNotNull(RuntimeInstaller::class.java.classLoader.getResourceAsStream(resourcePath)) {
                "Missing runtime resource: $resourcePath"
            }.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }

            return RuntimeTemplate(relativePath = relativePath, contents = contents)
        }
    }

    private fun renderTemplate(
        template: RuntimeTemplate,
        options: RuntimeInstallOptions,
    ): String {
        if (template.relativePath != "runtime-config.json") {
            return template.contents
        }

        return template.contents
            .replaceJsonValue("frameworkAdapter", options.frameworkAdapter)
            .replaceJsonValue("entryFile", options.entryFile)
    }

    private fun String.replaceJsonValue(key: String, replacement: String?): String {
        if (replacement == null) {
            return this
        }

        val pattern = Regex("""("$key"\s*:\s*")([^"]*)(")""")
        require(pattern.containsMatchIn(this)) { "Runtime config template is missing JSON key '$key'" }
        return replace(pattern) { match ->
            "${match.groupValues[1]}${escapeJsonString(replacement)}${match.groupValues[3]}"
        }
    }

    private fun escapeJsonString(value: String): String {
        return buildString {
            value.forEach { character ->
                when (character) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\b' -> append("\\b")
                    '\u000C' -> append("\\f")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(character)
                }
            }
        }
    }

    private fun validateExistingPathNodes(runtimeRoot: Path, targetParent: Path) {
        val runtimeRealRoot = runtimeRoot.toRealPath()
        var current = runtimeRoot
        validatePathNode(runtimeRoot, runtimeRealRoot, current)

        for (segment in runtimeRoot.relativize(targetParent)) {
            current = current.resolve(segment.toString())
            if (Files.exists(current, LinkOption.NOFOLLOW_LINKS)) {
                validatePathNode(runtimeRoot, runtimeRealRoot, current)
            }
        }
    }

    private fun validatePathNode(runtimeRoot: Path, runtimeRealRoot: Path, candidate: Path) {
        val attributes = Files.readAttributes(candidate, BasicFileAttributes::class.java, LinkOption.NOFOLLOW_LINKS)
        val isLinkLike = Files.isSymbolicLink(candidate) || attributes.isOther
        if (!isLinkLike) {
            return
        }

        val candidateRealPath = candidate.toRealPath()
        require(candidateRealPath.startsWith(runtimeRealRoot)) {
            "Runtime path '${runtimeRoot.relativize(candidate)}' resolves outside runtime directory '$RUNTIME_DIR_NAME'"
        }
    }

    private fun validateRuntimeRoot(projectRealRoot: Path, runtimeRoot: Path) {
        val attributes = Files.readAttributes(runtimeRoot, BasicFileAttributes::class.java, LinkOption.NOFOLLOW_LINKS)
        val isLinkLike = Files.isSymbolicLink(runtimeRoot) || attributes.isOther
        if (!isLinkLike) {
            return
        }

        val runtimeRealRoot = runtimeRoot.toRealPath()
        require(runtimeRealRoot.startsWith(projectRealRoot)) {
            "Runtime root '$RUNTIME_DIR_NAME' resolves outside project root"
        }
    }
}

data class RuntimeInstallOptions(
    val frameworkAdapter: String? = null,
    val entryFile: String? = null,
)
