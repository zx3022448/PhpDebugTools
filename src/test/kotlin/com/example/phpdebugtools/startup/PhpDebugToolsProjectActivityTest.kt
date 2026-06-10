package com.example.phpdebugtools.startup

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.nio.file.Path

class PhpDebugToolsProjectActivityTest : BasePlatformTestCase() {
    fun testProjectOpenInstallsRuntimeForThinkPhpProject() = runBlocking {
        val projectRoot = Path.of(project.basePath!!)
        Files.createDirectories(projectRoot)
        resetProjectRoot(projectRoot)
        Files.createDirectories(projectRoot.resolve("app"))
        Files.createDirectories(projectRoot.resolve("config"))
        Files.createDirectories(projectRoot.resolve("public"))
        Files.writeString(
            projectRoot.resolve("composer.json"),
            """
            {
              "require": {
                "topthink/framework": "^6.1"
              }
            }
            """.trimIndent(),
        )
        Files.writeString(projectRoot.resolve("public/index.php"), "<?php")

        PhpDebugToolsProjectActivity().execute(project)

        assertTrue(Files.exists(projectRoot.resolve(".php-debug-tools/bootstrap.php")))
    }

    fun testProjectOpenDoesNotInstallRuntimeForUnknownProject() = runBlocking {
        val projectRoot = Path.of(project.basePath!!)
        Files.createDirectories(projectRoot)
        resetProjectRoot(projectRoot)
        Files.writeString(projectRoot.resolve("README.md"), "# demo")

        PhpDebugToolsProjectActivity().execute(project)

        assertFalse(Files.exists(projectRoot.resolve(".php-debug-tools/bootstrap.php")))
    }

    private fun resetProjectRoot(projectRoot: Path) {
        deleteIfExists(projectRoot.resolve(".php-debug-tools"))
        deleteIfExists(projectRoot.resolve("app"))
        deleteIfExists(projectRoot.resolve("config"))
        deleteIfExists(projectRoot.resolve("public"))
        Files.deleteIfExists(projectRoot.resolve("composer.json"))
        Files.deleteIfExists(projectRoot.resolve("README.md"))
    }

    private fun deleteIfExists(path: Path) {
        if (!Files.exists(path)) {
            return
        }

        Files.walk(path)
            .sorted(Comparator.reverseOrder())
            .forEach(Files::deleteIfExists)
    }
}
