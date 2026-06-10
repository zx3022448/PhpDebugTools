package com.example.phpdebugtools.toolwindow

import com.example.phpdebugtools.PhpDebugToolsBundle
import com.example.phpdebugtools.project.ThinkPhpProjectDetector
import com.example.phpdebugtools.runtime.RuntimeInstaller
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.content.ContentFactory
import java.nio.file.Files
import java.nio.file.Path

class PhpDebugToolsToolWindowFactory : ToolWindowFactory {
    override fun shouldBeAvailable(project: Project): Boolean = true

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val tabs = JBTabbedPane()
        val panel = PhpDebugToolsToolWindowPanel(tabs)
        tabs.addTab(PhpDebugToolsBundle.message("toolwindow.tab.overview"), panel.overviewComponent)
        tabs.addTab(
            PhpDebugToolsBundle.message("toolwindow.tab.requestDebug"),
            createPlaceholderPanel(PhpDebugToolsBundle.message("toolwindow.placeholder.requestDebug")),
        )
        tabs.addTab(
            PhpDebugToolsBundle.message("toolwindow.tab.methodInvoke"),
            createPlaceholderPanel(PhpDebugToolsBundle.message("toolwindow.placeholder.methodInvoke")),
        )
        tabs.addTab(
            PhpDebugToolsBundle.message("toolwindow.tab.diagnostics"),
            createPlaceholderPanel(PhpDebugToolsBundle.message("toolwindow.placeholder.diagnostics")),
        )
        project.basePath?.let { projectBasePath ->
            panel.updateOverview(buildOverviewState(Path.of(projectBasePath)))
        }
        val content = ContentFactory.getInstance().createContent(panel, null, false)
        toolWindow.contentManager.addContent(content)
    }

    private fun createPlaceholderPanel(message: String): JBPanel<JBPanel<*>> {
        return JBPanel<JBPanel<*>>().apply {
            add(JBLabel(message))
        }
    }
}

internal fun buildOverviewState(
    projectRoot: Path,
    runtimeInstaller: RuntimeInstaller = RuntimeInstaller(),
): OverviewViewState {
    val detection = ThinkPhpProjectDetector.detect(
        composerJson = readProjectFile(projectRoot.resolve("composer.json")),
        installedFrameworkVersion = null,
        entryFileText = readProjectFile(projectRoot.resolve("public/index.php")),
        knownPaths = collectKnownPaths(projectRoot),
    )

    val projectSummary = if (detection.isThinkPhp) {
        PhpDebugToolsBundle.message("overview.project.detected", detection.majorVersion ?: "?")
    } else {
        PhpDebugToolsBundle.message("overview.project.unknown")
    }
    val runtimeSummary = if (detection.isThinkPhp) {
        runtimeInstaller.install(projectRoot)
        PhpDebugToolsBundle.message("runtime.installed")
    } else {
        PhpDebugToolsBundle.message("toolwindow.overview.runtime.placeholder")
    }

    return OverviewViewState(
        projectSummary = projectSummary,
        runtimeSummary = runtimeSummary,
        diagnosticsSummary = PhpDebugToolsBundle.message("toolwindow.overview.diagnostics.placeholder"),
    )
}

private fun readProjectFile(path: Path): String? {
    if (!Files.isRegularFile(path)) {
        return null
    }
    return Files.readString(path)
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
