package com.example.phpdebugtools.runtime

import org.junit.Assume.assumeTrue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path

class RuntimeInstallerTest {

    @Test
    fun installsRuntimeFilesIntoProjectFolder() {
        val projectRoot = Files.createTempDirectory("php-debug-tools-project")
        val installer = RuntimeInstaller(
            templates = listOf(
                RuntimeTemplate("bootstrap.php", "<?php echo 'ok';"),
                RuntimeTemplate("runtime-config.json", """{"version":"1"}"""),
            ),
        )

        val result = installer.install(projectRoot)

        assertTrue(result.installedFiles.contains(".php-debug-tools/bootstrap.php"))
        assertTrue(Files.exists(projectRoot.resolve(".php-debug-tools/bootstrap.php")))
    }

    @Test
    fun rejectsTemplatePathTraversalOutsideRuntimeDirectory() {
        val projectRoot = Files.createTempDirectory("php-debug-tools-project")
        val installer = RuntimeInstaller(
            templates = listOf(RuntimeTemplate("../escape.php", "<?php echo 'escape';")),
        )

        val error = try {
            installer.install(projectRoot)
            null
        } catch (exception: IllegalArgumentException) {
            exception
        }

        assertTrue(error != null)
        assertTrue(error!!.message!!.contains("outside runtime directory"))
    }

    @Test
    fun installsBundledTemplatesByDefault() {
        val projectRoot = Files.createTempDirectory("php-debug-tools-project")

        val result = RuntimeInstaller().install(projectRoot)
        val expectedFiles = setOf(
            ".php-debug-tools/bootstrap.php",
            ".php-debug-tools/adapters/thinkphp5.php",
            ".php-debug-tools/adapters/thinkphp6.php",
            ".php-debug-tools/invoke-service.php",
            ".php-debug-tools/invoke-controller.php",
            ".php-debug-tools/debug-web-entry.php",
            ".php-debug-tools/runtime-config.json",
        )

        assertEquals(expectedFiles, result.installedFiles.toSet())
        expectedFiles.forEach { installedFile ->
            assertTrue(
                Files.exists(
                    projectRoot.resolve(".php-debug-tools").resolve(installedFile.removePrefix(".php-debug-tools/")),
                ),
            )
        }
        val bootstrapContents = Files.readString(projectRoot.resolve(".php-debug-tools/bootstrap.php"))
        val debugWebEntryContents = Files.readString(projectRoot.resolve(".php-debug-tools/debug-web-entry.php"))
        assertTrue(bootstrapContents.contains("function php_debug_tools_fail"))
        assertTrue(bootstrapContents.contains("header('Content-Type: application/json; charset=utf-8');"))
        assertTrue(bootstrapContents.contains("is_array(\$adapterDefinition)"))
        assertTrue(debugWebEntryContents.contains("header('Content-Type: application/json; charset=utf-8');"))
        assertEquals(projectRoot.resolve(".php-debug-tools").toString(), result.runtimeRoot)
        assertEquals(true, result.updated)
    }

    @Test
    fun rejectsWritingThroughExistingLinkThatEscapesRuntimeDirectory() {
        val projectRoot = Files.createTempDirectory("php-debug-tools-project")
        val runtimeRoot = projectRoot.resolve(".php-debug-tools")
        Files.createDirectories(runtimeRoot)

        val outsideDirectory = Files.createTempDirectory("php-debug-tools-outside")
        val linkPath = runtimeRoot.resolve("adapters")
        assumeTrue(
            "Current environment cannot create a junction/symlink for this regression test",
            createEscapingLink(linkPath, outsideDirectory),
        )

        val installer = RuntimeInstaller(
            templates = listOf(RuntimeTemplate("adapters/thinkphp6.php", "<?php return [];")),
        )

        val error = try {
            installer.install(projectRoot)
            null
        } catch (exception: IllegalArgumentException) {
            exception
        }

        assertTrue(error != null)
        assertTrue(error!!.message!!.contains("outside runtime directory"))
        assertFalse(Files.exists(outsideDirectory.resolve("thinkphp6.php")))
    }

    @Test
    fun rejectsEscapingRuntimeRootLink() {
        val projectRoot = Files.createTempDirectory("php-debug-tools-project")
        val outsideDirectory = Files.createTempDirectory("php-debug-tools-outside")
        val runtimeRoot = projectRoot.resolve(".php-debug-tools")
        assumeTrue(
            "Current environment cannot create a junction/symlink for this regression test",
            createEscapingLink(runtimeRoot, outsideDirectory),
        )

        val installer = RuntimeInstaller(
            templates = listOf(RuntimeTemplate("bootstrap.php", "<?php echo 'blocked';")),
        )

        val error = try {
            installer.install(projectRoot)
            null
        } catch (exception: IllegalArgumentException) {
            exception
        }

        assertTrue(error != null)
        assertTrue(error!!.message!!.contains("outside project root"))
        assertFalse(Files.exists(outsideDirectory.resolve("bootstrap.php")))
    }

    private fun createEscapingLink(linkPath: Path, targetPath: Path): Boolean {
        return if (isWindows()) {
            createWindowsJunction(linkPath, targetPath)
        } else {
            createSymbolicLink(linkPath, targetPath)
        }
    }

    private fun createWindowsJunction(linkPath: Path, targetPath: Path): Boolean {
        val process = ProcessBuilder(
            "powershell",
            "-NoProfile",
            "-NonInteractive",
            "-Command",
            "New-Item -ItemType Junction -Path \$env:LINK_PATH -Target \$env:TARGET_PATH | Out-Null",
        ).apply {
            redirectErrorStream(true)
            environment()["LINK_PATH"] = linkPath.toString()
            environment()["TARGET_PATH"] = targetPath.toString()
        }.start()

        return process.waitFor() == 0 && Files.exists(linkPath)
    }

    private fun createSymbolicLink(linkPath: Path, targetPath: Path): Boolean {
        return try {
            Files.createSymbolicLink(linkPath, targetPath)
            Files.exists(linkPath)
        } catch (_: Exception) {
            false
        }
    }

    private fun isWindows(): Boolean = System.getProperty("os.name").startsWith("Windows", ignoreCase = true)
}
