package com.example.phpdebugtools.toolwindow

import com.example.phpdebugtools.PhpDebugToolsBundle
import com.example.phpdebugtools.runtime.RuntimeInstaller
import com.example.phpdebugtools.runtime.RuntimeInstallOptions
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.content.ContentFactory
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.JComponent

class PhpDebugToolsToolWindowFactory : ToolWindowFactory {
    override fun shouldBeAvailable(project: Project): Boolean = true

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        logger.info("Creating PhpDebugTools tool window content for project '${project.name}'")
        val tabs = JBTabbedPane()
        val panel = PhpDebugToolsToolWindowPanel(tabs, project)
        buildToolWindowTabs(panel).forEach { tab ->
            tabs.addTab(tab.title, tab.component)
        }
        project.basePath?.let { projectBasePath ->
            updateWorkspaceInBackground(panel, Path.of(projectBasePath))
        }
        val content = ContentFactory.getInstance().createContent(panel, null, false)
        toolWindow.contentManager.addContent(content)
    }

    private companion object {
        private val logger = Logger.getInstance(PhpDebugToolsToolWindowFactory::class.java)

        private fun updateWorkspaceInBackground(panel: PhpDebugToolsToolWindowPanel, projectRoot: Path) {
            ApplicationManager.getApplication().executeOnPooledThread {
                val state = buildToolWindowWorkspaceState(projectRoot)
                ApplicationManager.getApplication().invokeLater {
                    panel.updateWorkspace(state)
                }
            }
        }
    }
}

internal data class ToolWindowTabDefinition(
    val title: String,
    val component: JComponent,
)

internal fun buildToolWindowTabs(panel: PhpDebugToolsToolWindowPanel): List<ToolWindowTabDefinition> {
    return listOf(
        ToolWindowTabDefinition(
            title = PhpDebugToolsBundle.message("toolwindow.tab.overview"),
            component = panel.overviewComponent,
        ),
        ToolWindowTabDefinition(
            title = PhpDebugToolsBundle.message("toolwindow.tab.methodInvoke"),
            component = panel.methodInvokeComponent,
        ),
    )
}

internal fun buildToolWindowWorkspaceState(
    projectRoot: Path,
    runtimeInstaller: RuntimeInstaller = RuntimeInstaller(),
): ToolWindowWorkspaceState {
    val detection = detectThinkPhpProject(projectRoot)
    val runtimeResult = if (detection.isThinkPhp) {
        runtimeInstaller.install(
            projectRoot,
            RuntimeInstallOptions(
                frameworkAdapter = detection.majorVersion?.let { "thinkphp$it" },
                entryFile = detection.entryFile,
            ),
        )
    } else {
        null
    }

    return ToolWindowWorkspaceState(
        overview = OverviewViewState(
            projectSummary = if (detection.isThinkPhp) {
                PhpDebugToolsBundle.message("overview.project.detected", detection.majorVersion ?: "?")
            } else {
                PhpDebugToolsBundle.message("overview.project.unknown")
            },
            runtimeSummary = if (runtimeResult != null) {
                PhpDebugToolsBundle.message("runtime.installed")
            } else {
                PhpDebugToolsBundle.message("toolwindow.runtime.skipped")
            },
        ),
        methodInvoke = buildMethodInvokeState(detection),
    )
}

internal fun buildLazyToolWindowWorkspaceState(): ToolWindowWorkspaceState =
    ToolWindowWorkspaceState(
        overview = OverviewViewState(
            projectSummary = PhpDebugToolsBundle.message("toolwindow.overview.project.placeholder"),
            runtimeSummary = PhpDebugToolsBundle.message("toolwindow.overview.runtime.placeholder"),
        ),
        methodInvoke = ToolWindowDetailState(
            summary = PhpDebugToolsBundle.message("toolwindow.methodInvoke.summary.pending"),
            details = listOf(PhpDebugToolsBundle.message("toolwindow.methodInvoke.detail.pending")),
        ),
    )

internal fun buildOverviewState(
    projectRoot: Path,
    runtimeInstaller: RuntimeInstaller = RuntimeInstaller(),
): OverviewViewState {
    val detection = detectThinkPhpProject(projectRoot)

    val projectSummary = if (detection.isThinkPhp) {
        PhpDebugToolsBundle.message("overview.project.detected", detection.majorVersion ?: "?")
    } else {
        PhpDebugToolsBundle.message("overview.project.unknown")
    }
    val runtimeSummary = if (detection.isThinkPhp) {
        runtimeInstaller.install(
            projectRoot,
            RuntimeInstallOptions(
                frameworkAdapter = detection.majorVersion?.let { "thinkphp$it" },
                entryFile = detection.entryFile,
            ),
        )
        PhpDebugToolsBundle.message("runtime.installed")
    } else {
        PhpDebugToolsBundle.message("toolwindow.overview.runtime.placeholder")
    }

    return OverviewViewState(
        projectSummary = projectSummary,
        runtimeSummary = runtimeSummary,
    )
}

private fun detectThinkPhpProject(projectRoot: Path): com.example.phpdebugtools.project.ThinkPhpProjectInfo {
    return com.example.phpdebugtools.project.ThinkPhpProjectDetector.detect(
        composerJson = readProjectFile(projectRoot.resolve("composer.json")),
        installedFrameworkVersion = null,
        entryFileText = readProjectFile(projectRoot.resolve("public/index.php")),
        knownPaths = collectKnownPaths(projectRoot, maxEntries = 2_000),
    )
}

private fun readProjectFile(path: Path): String? {
    if (!Files.isRegularFile(path)) {
        return null
    }
    return Files.readString(path)
}

private fun collectKnownPaths(projectRoot: Path, maxEntries: Int): Set<String> {
    if (!Files.exists(projectRoot)) {
        return emptySet()
    }

    val knownPaths = mutableSetOf<String>()
    Files.walk(projectRoot).use { paths ->
        val iterator = paths.iterator()
        while (iterator.hasNext() && knownPaths.size < maxEntries) {
            val path = iterator.next()
            if (path != projectRoot) {
                knownPaths += projectRoot.relativize(path).toString().replace('\\', '/')
            }
        }
    }
    return knownPaths
}

private fun buildMethodInvokeState(detection: com.example.phpdebugtools.project.ThinkPhpProjectInfo): ToolWindowDetailState {
    return ToolWindowDetailState(
        summary = if (detection.isThinkPhp) {
            PhpDebugToolsBundle.message("toolwindow.methodInvoke.summary.ready")
        } else {
            PhpDebugToolsBundle.message("toolwindow.methodInvoke.summary.requiresThinkPhp")
        },
        details = listOf(
            PhpDebugToolsBundle.message("toolwindow.methodInvoke.detail.serviceAction"),
            PhpDebugToolsBundle.message("toolwindow.methodInvoke.detail.controllerAction"),
            PhpDebugToolsBundle.message("toolwindow.methodInvoke.detail.serviceArgs"),
            PhpDebugToolsBundle.message("toolwindow.methodInvoke.detail.controllerArgs"),
            PhpDebugToolsBundle.message("toolwindow.methodInvoke.detail.runtimeEntries"),
        ),
    )
}
