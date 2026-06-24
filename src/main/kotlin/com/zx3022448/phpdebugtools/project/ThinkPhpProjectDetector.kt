package com.zx3022448.phpdebugtools.project

object ThinkPhpProjectDetector {
    private val frameworkVersionPattern =
        Regex(""""topthink/framework"\s*:\s*"([^"]+)"""")
    private val majorVersionPattern = Regex("""([56])(?:[.x]|$)""")

    fun detect(
        composerJson: String?,
        installedFrameworkVersion: String?,
        entryFileText: String?,
        knownPaths: Set<String>,
    ): ThinkPhpProjectInfo {
        val composerVersion = composerJson?.let(::extractComposerMajorVersion)
        val installedVersion = installedFrameworkVersion?.let(::extractMajorVersion)
        val looksLikeThinkPhp = looksLikeThinkPhp(knownPaths)
        val entryFile = resolveEntryFile(knownPaths)

        if (composerVersion != null && installedVersion != null) {
            if (composerVersion == installedVersion) {
                return ThinkPhpProjectInfo(
                    isThinkPhp = true,
                    majorVersion = installedVersion,
                    detectionSource = "composer+installed",
                    entryFile = entryFile,
                    confidence = 90,
                )
            }

            return ThinkPhpProjectInfo(
                isThinkPhp = true,
                majorVersion = composerVersion,
                detectionSource = "composer",
                entryFile = entryFile,
                confidence = 90,
            )
        }

        if (composerVersion != null) {
            return ThinkPhpProjectInfo(
                isThinkPhp = true,
                majorVersion = composerVersion,
                detectionSource = "composer",
                entryFile = entryFile,
                confidence = 90,
            )
        }

        if (looksLikeThinkPhp) {
            return ThinkPhpProjectInfo(
                isThinkPhp = true,
                majorVersion = installedVersion ?: composerVersion,
                detectionSource = "layout",
                entryFile = entryFile,
                confidence = 60,
            )
        }

        return ThinkPhpProjectInfo(
            isThinkPhp = false,
            detectionSource = "none",
            entryFile = entryFile,
            confidence = 0,
        )
    }

    private fun extractComposerMajorVersion(composerJson: String): String? {
        val constraint = frameworkVersionPattern.find(composerJson)?.groupValues?.get(1) ?: return null
        return extractMajorVersion(constraint)
    }

    private fun extractMajorVersion(version: String): String? {
        val normalized = version.replace("^", "").trim()
        return majorVersionPattern.find(normalized)?.groupValues?.get(1)
    }

    private fun looksLikeThinkPhp(knownPaths: Set<String>): Boolean {
        val hasPublicIndex = knownPaths.contains("public/index.php")
        val hasAppDirectory = knownPaths.contains("app")
        val hasConfigDirectory = knownPaths.contains("config")
        return hasPublicIndex && hasAppDirectory && hasConfigDirectory
    }

    private fun resolveEntryFile(knownPaths: Set<String>): String? {
        return if (knownPaths.contains("public/index.php")) {
            "public/index.php"
        } else {
            null
        }
    }
}
