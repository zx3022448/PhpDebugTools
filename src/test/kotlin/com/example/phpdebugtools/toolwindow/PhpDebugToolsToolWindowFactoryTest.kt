package com.example.phpdebugtools.toolwindow

import com.example.phpdebugtools.runtime.RuntimeInstaller
import com.example.phpdebugtools.runtime.RuntimeTemplate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class PhpDebugToolsToolWindowFactoryTest {

    @Test
    fun buildsOverviewStateForThinkPhpProjectAndInstallsRuntime() {
        val projectRoot = Files.createTempDirectory("php-debug-tools-thinkphp")
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

        val state = buildOverviewState(
            projectRoot = projectRoot,
            runtimeInstaller = RuntimeInstaller(
                templates = listOf(
                    RuntimeTemplate("bootstrap.php", "<?php echo 'ok';"),
                    RuntimeTemplate(
                        "runtime-config.json",
                        """
                        {
                          "version": "1",
                          "frameworkAdapter": "thinkphp5",
                          "entryFile": "index.php"
                        }
                        """.trimIndent(),
                    ),
                ),
            ),
        )

        assertEquals("ThinkPHP 6 project detected", state.projectSummary)
        assertEquals(".php-debug-tools 已安装", state.runtimeSummary)
        assertEquals("等待诊断执行", state.diagnosticsSummary)
        assertTrue(Files.exists(projectRoot.resolve(".php-debug-tools/bootstrap.php")))
        val runtimeConfig = Files.readString(projectRoot.resolve(".php-debug-tools/runtime-config.json"))
        assertTrue(runtimeConfig.contains("\"frameworkAdapter\": \"thinkphp6\""))
        assertTrue(runtimeConfig.contains("\"entryFile\": \"public/index.php\""))
    }
}
