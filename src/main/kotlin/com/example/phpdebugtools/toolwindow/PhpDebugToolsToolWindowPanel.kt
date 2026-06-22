package com.example.phpdebugtools.toolwindow

import com.example.phpdebugtools.PhpDebugToolsBundle
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import java.awt.BorderLayout
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JComponent
import com.intellij.ui.components.JBTabbedPane

class PhpDebugToolsToolWindowPanel(
    private val tabs: JBTabbedPane = JBTabbedPane(),
    project: Project? = null,
) : JBPanel<PhpDebugToolsToolWindowPanel>(BorderLayout()) {
    private val overviewTitleLabel = JBLabel(PhpDebugToolsBundle.message("toolwindow.overview.section.summary"))
    private val projectSummaryLabel = JBLabel()
    private val runtimeSummaryLabel = JBLabel()
    private val summaryPanel = JBPanel<JBPanel<*>>().apply {
        layout = BorderLayout(0, 8)
        ToolWindowUiStyles.applyCard(this)
        add(
            JBPanel<JBPanel<*>>().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                isOpaque = false
                ToolWindowUiStyles.applyTitleLabel(overviewTitleLabel)
                add(overviewTitleLabel)
                add(Box.createVerticalStrut(8))
                add(projectSummaryLabel.also(ToolWindowUiStyles::applyMutedLabel))
                add(Box.createVerticalStrut(4))
                add(runtimeSummaryLabel.also(ToolWindowUiStyles::applyMutedLabel))
            },
            BorderLayout.CENTER,
        )
    }
    val overviewComponent: JComponent = JBPanel<JBPanel<*>>(BorderLayout()).apply {
        ToolWindowUiStyles.applyWorkbenchSurface(this)
        add(summaryPanel, BorderLayout.NORTH)
    }
    private val methodInvokePanel = MethodInvokeToolWindowPanel(project)
    val methodInvokeComponent: JComponent = methodInvokePanel

    init {
        ToolWindowUiStyles.applyWorkbenchSurface(this)
        buildToolWindowTabs(this).forEach { tab ->
            tabs.addTab(tab.title, tab.component)
        }
        selectDefaultToolWindowTab(tabs)
        add(tabs, BorderLayout.CENTER)
        updateWorkspace(buildLazyToolWindowWorkspaceState())
    }

    fun updateOverview(state: OverviewViewState) {
        projectSummaryLabel.text = PhpDebugToolsBundle.message("toolwindow.overview.project.label", state.projectSummary)
        runtimeSummaryLabel.text = PhpDebugToolsBundle.message("toolwindow.overview.runtime.label", state.runtimeSummary)
    }

    fun updateWorkspace(state: ToolWindowWorkspaceState) {
        updateOverview(state.overview)
        methodInvokePanel.updateGuidance(state.methodInvoke)
    }

    internal fun hasOverviewCard(): Boolean = summaryPanel.parent != null

    internal fun selectedToolWindowComponent(): JComponent? = tabs.selectedComponent as? JComponent
}
