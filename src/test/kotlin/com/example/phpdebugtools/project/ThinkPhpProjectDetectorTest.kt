package com.example.phpdebugtools.project

import org.junit.Assert.assertEquals
import org.junit.Test

class ThinkPhpProjectDetectorTest {

    @Test
    fun detectsThinkPhp6FromComposerConstraint() {
        val composerJson = """
            {
              "require": {
                "topthink/framework": "^6.1"
              }
            }
        """.trimIndent()

        val result = ThinkPhpProjectDetector.detect(
            composerJson = composerJson,
            installedFrameworkVersion = "6.1.4",
            entryFileText = "<?php require __DIR__ . '/../vendor/autoload.php';",
            knownPaths = setOf("app", "config", "public/index.php", "route"),
        )

        assertEquals(true, result.isThinkPhp)
        assertEquals("6", result.majorVersion)
        assertEquals("composer+installed", result.detectionSource)
        assertEquals("public/index.php", result.entryFile)
        assertEquals(90, result.confidence)
    }

    @Test
    fun prefersComposerWhenComposerAndInstalledVersionsConflict() {
        val composerJson = """
            {
              "require": {
                "topthink/framework": "^6.1"
              }
            }
        """.trimIndent()

        val result = ThinkPhpProjectDetector.detect(
            composerJson = composerJson,
            installedFrameworkVersion = "5.1.0",
            entryFileText = null,
            knownPaths = setOf("app", "config", "public/index.php", "route"),
        )

        assertEquals(true, result.isThinkPhp)
        assertEquals("6", result.majorVersion)
        assertEquals("composer", result.detectionSource)
        assertEquals("public/index.php", result.entryFile)
        assertEquals(90, result.confidence)
    }

    @Test
    fun detectsThinkPhpFromLayout() {
        val result = ThinkPhpProjectDetector.detect(
            composerJson = null,
            installedFrameworkVersion = null,
            entryFileText = null,
            knownPaths = setOf("app", "config", "public/index.php"),
        )

        assertEquals(true, result.isThinkPhp)
        assertEquals(null, result.majorVersion)
        assertEquals("layout", result.detectionSource)
        assertEquals("public/index.php", result.entryFile)
        assertEquals(60, result.confidence)
    }

    @Test
    fun returnsNoneWhenProjectIsNotThinkPhp() {
        val result = ThinkPhpProjectDetector.detect(
            composerJson = """{"require":{"laravel/framework":"^11.0"}}""",
            installedFrameworkVersion = null,
            entryFileText = null,
            knownPaths = setOf("src", "bootstrap", "routes/web.php"),
        )

        assertEquals(false, result.isThinkPhp)
        assertEquals(null, result.majorVersion)
        assertEquals("none", result.detectionSource)
        assertEquals(null, result.entryFile)
        assertEquals(0, result.confidence)
    }
}
