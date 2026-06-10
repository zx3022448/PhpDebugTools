package com.example.phpdebugtools.toolwindow

import com.example.phpdebugtools.PhpDebugToolsBundle
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.content.ContentFactory

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
        val content = ContentFactory.getInstance().createContent(panel, null, false)
        toolWindow.contentManager.addContent(content)
    }

    private fun createPlaceholderPanel(message: String): JBPanel<JBPanel<*>> {
        return JBPanel<JBPanel<*>>().apply {
            add(JBLabel(message))
        }
    }
}
