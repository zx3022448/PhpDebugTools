package com.example.phpdebugtools.startup

import com.example.phpdebugtools.toolwindow.buildOverviewState
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import java.nio.file.Path

class PhpDebugToolsProjectActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        val projectBasePath = project.basePath ?: return
        val overviewState = buildOverviewState(Path.of(projectBasePath))
        logger.info(
            "PhpDebugTools startup initialized for '$projectBasePath': " +
                "project='${overviewState.projectSummary}', runtime='${overviewState.runtimeSummary}'",
        )
    }

    private companion object {
        private val logger = Logger.getInstance(PhpDebugToolsProjectActivity::class.java)
    }
}
