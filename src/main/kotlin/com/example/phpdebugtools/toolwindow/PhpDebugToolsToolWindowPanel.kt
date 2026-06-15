package com.example.phpdebugtools.toolwindow

import com.example.phpdebugtools.PhpDebugToolsBundle
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JTabbedPane

class PhpDebugToolsToolWindowPanel(
    private val tabs: JTabbedPane,
    project: Project? = null,
) : JComponent() {
    private val projectSummaryLabel = JBLabel()
    private val runtimeSummaryLabel = JBLabel()
    private val summaryPanel = JBPanel<JBPanel<*>>().apply {
        layout = BorderLayout()
        add(
            JBPanel<JBPanel<*>>().apply {
                layout = javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS)
                add(projectSummaryLabel)
                add(runtimeSummaryLabel)
            },
            BorderLayout.NORTH,
        )
    }
    val overviewComponent: JComponent = JBPanel<JBPanel<*>>(BorderLayout()).apply {
        add(summaryPanel, BorderLayout.NORTH)
    }
    private val methodInvokePanel = MethodInvokeToolWindowPanel(project)
    val methodInvokeComponent: JComponent = methodInvokePanel

    init {
        layout = BorderLayout()
        add(tabs, BorderLayout.CENTER)
        updateWorkspace(
            ToolWindowWorkspaceState(
                overview = OverviewViewState(
                    projectSummary = PhpDebugToolsBundle.message("toolwindow.overview.project.placeholder"),
                    runtimeSummary = PhpDebugToolsBundle.message("toolwindow.overview.runtime.placeholder"),
                ),
                methodInvoke = ToolWindowDetailState(
                    summary = PhpDebugToolsBundle.message("toolwindow.methodInvoke.summary.pending"),
                    details = listOf(PhpDebugToolsBundle.message("toolwindow.methodInvoke.detail.pending")),
                ),
            ),
        )
    }

    fun updateOverview(state: OverviewViewState) {
        projectSummaryLabel.text = PhpDebugToolsBundle.message("toolwindow.overview.project.label", state.projectSummary)
        runtimeSummaryLabel.text = PhpDebugToolsBundle.message("toolwindow.overview.runtime.label", state.runtimeSummary)
    }

    fun updateWorkspace(state: ToolWindowWorkspaceState) {
        updateOverview(state.overview)
        methodInvokePanel.updateGuidance(state.methodInvoke)
    }
}
