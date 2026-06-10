package com.example.phpdebugtools.toolwindow

import com.example.phpdebugtools.PhpDebugToolsBundle
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JTabbedPane

class PhpDebugToolsToolWindowPanel(
    private val tabs: JTabbedPane,
) : JComponent() {
    private val projectSummaryLabel = JBLabel()
    private val runtimeSummaryLabel = JBLabel()
    private val diagnosticsSummaryLabel = JBLabel()
    private val summaryPanel = JBPanel<JBPanel<*>>().apply {
        layout = BorderLayout()
        add(
            JBPanel<JBPanel<*>>().apply {
                layout = javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS)
                add(projectSummaryLabel)
                add(runtimeSummaryLabel)
                add(diagnosticsSummaryLabel)
            },
            BorderLayout.NORTH,
        )
    }
    val overviewComponent: JComponent = JBPanel<JBPanel<*>>(BorderLayout()).apply {
        add(summaryPanel, BorderLayout.NORTH)
    }

    init {
        layout = BorderLayout()
        add(tabs, BorderLayout.CENTER)
        updateOverview(
            OverviewViewState(
                projectSummary = PhpDebugToolsBundle.message("toolwindow.overview.project.placeholder"),
                runtimeSummary = PhpDebugToolsBundle.message("toolwindow.overview.runtime.placeholder"),
                diagnosticsSummary = PhpDebugToolsBundle.message("toolwindow.overview.diagnostics.placeholder"),
            ),
        )
    }

    fun updateOverview(state: OverviewViewState) {
        projectSummaryLabel.text = PhpDebugToolsBundle.message("toolwindow.overview.project.label", state.projectSummary)
        runtimeSummaryLabel.text = PhpDebugToolsBundle.message("toolwindow.overview.runtime.label", state.runtimeSummary)
        diagnosticsSummaryLabel.text =
            PhpDebugToolsBundle.message("toolwindow.overview.diagnostics.label", state.diagnosticsSummary)
    }
}
