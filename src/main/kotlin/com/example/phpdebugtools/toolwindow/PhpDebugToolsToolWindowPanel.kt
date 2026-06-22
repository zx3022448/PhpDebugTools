package com.example.phpdebugtools.toolwindow

import com.example.phpdebugtools.PhpDebugToolsBundle
import com.example.phpdebugtools.persistence.RecentDebugStore
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel

class PhpDebugToolsToolWindowPanel(
    private val tabs: com.intellij.ui.components.JBTabbedPane = com.intellij.ui.components.JBTabbedPane(),
    private val project: Project? = null,
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
        isOpaque = false
        border = com.intellij.util.ui.JBUI.Borders.empty(18)
        add(summaryPanel, BorderLayout.NORTH)
    }
    private val methodInvokePanel = MethodInvokeToolWindowPanel(project)
    val methodInvokeComponent: JComponent = methodInvokePanel

    private val workbenchButton = JButton(PhpDebugToolsBundle.message("toolwindow.chrome.workbench"))
    private val recentRunsPanel = JPanel(FlowLayout(FlowLayout.LEFT, 8, 0))
    private val contentCardLayout = CardLayout()
    private val contentHost = JBPanel<JBPanel<*>>(contentCardLayout)
    private var selectedComponent: JComponent = methodInvokeComponent

    init {
        ToolWindowUiStyles.applyWorkbenchSurface(this)
        ToolWindowUiStyles.applyWindowShell(contentHost)
        contentHost.isOpaque = false
        contentHost.add(overviewComponent, ToolWindowSection.OVERVIEW)
        contentHost.add(methodInvokeComponent, ToolWindowSection.METHOD_INVOKE)

        add(
            JBPanel<JBPanel<*>>(BorderLayout()).apply {
                ToolWindowUiStyles.applyWindowShell(this)
                add(buildTitleBar(), BorderLayout.NORTH)
                add(
                    JBPanel<JBPanel<*>>(BorderLayout()).apply {
                        isOpaque = false
                        add(buildBrandSection(), BorderLayout.NORTH)
                        add(contentHost, BorderLayout.CENTER)
                    },
                    BorderLayout.CENTER,
                )
            },
            BorderLayout.CENTER,
        )

        updateWorkspace(buildLazyToolWindowWorkspaceState())
        refreshRecentRuns()
        showSection(ToolWindowSection.METHOD_INVOKE)
    }

    fun updateOverview(state: OverviewViewState) {
        projectSummaryLabel.text = PhpDebugToolsBundle.message("toolwindow.overview.project.label", state.projectSummary)
        runtimeSummaryLabel.text = PhpDebugToolsBundle.message("toolwindow.overview.runtime.label", state.runtimeSummary)
    }

    fun updateWorkspace(state: ToolWindowWorkspaceState) {
        updateOverview(state.overview)
        methodInvokePanel.updateGuidance(state.methodInvoke)
        refreshRecentRuns()
    }

    internal fun hasOverviewCard(): Boolean = summaryPanel.parent != null

    internal fun selectedToolWindowComponent(): JComponent = selectedComponent

    private fun buildTitleBar(): JComponent =
        JBPanel<JBPanel<*>>(BorderLayout()).apply {
            ToolWindowUiStyles.applyTitleBar(this)
            preferredSize = Dimension(0, com.intellij.util.ui.JBUI.scale(48))
            add(buildWindowControls(), BorderLayout.WEST)
            add(
                JBPanel<JBPanel<*>>().apply {
                    layout = BoxLayout(this, BoxLayout.X_AXIS)
                    isOpaque = false
                    add(JBLabel(PhpDebugToolsBundle.message("toolwindow.chrome.title")).also(ToolWindowUiStyles::applyTitleLabel))
                    add(Box.createHorizontalStrut(8))
                    add(JBLabel(PhpDebugToolsBundle.message("toolwindow.chrome.mode")).also(ToolWindowUiStyles::applyMutedLabel))
                },
                BorderLayout.CENTER,
            )
            add(
                JPanel(FlowLayout(FlowLayout.RIGHT, 6, 8)).apply {
                    isOpaque = false
                    add(buildTitleBarIconButton())
                    add(buildTitleBarIconButton())
                },
                BorderLayout.EAST,
            )
        }

    private fun buildWindowControls(): JComponent =
        JPanel(FlowLayout(FlowLayout.LEFT, 8, 14)).apply {
            isOpaque = false
            add(buildTrafficLight(java.awt.Color(0xFF5F57)))
            add(buildTrafficLight(java.awt.Color(0xFFBD2E)))
            add(buildTrafficLight(java.awt.Color(0x28C840)))
        }

    private fun buildTrafficLight(color: java.awt.Color): JComponent =
        JPanel().apply {
            isOpaque = true
            background = color
            preferredSize = Dimension(com.intellij.util.ui.JBUI.scale(12), com.intellij.util.ui.JBUI.scale(12))
            minimumSize = preferredSize
            maximumSize = preferredSize
        }

    private fun buildTitleBarIconButton(): JButton =
        JButton().apply {
            preferredSize = Dimension(com.intellij.util.ui.JBUI.scale(28), com.intellij.util.ui.JBUI.scale(28))
            minimumSize = preferredSize
            maximumSize = preferredSize
            ToolWindowUiStyles.applyRoundIconButton(this)
        }

    private fun buildBrandSection(): JComponent =
        JBPanel<JBPanel<*>>(BorderLayout(0, 0)).apply {
            ToolWindowUiStyles.applySidebarSurface(this)
            add(
                JBPanel<JBPanel<*>>(BorderLayout(12, 0)).apply {
                    isOpaque = false
                    border = com.intellij.util.ui.JBUI.Borders.empty(14)
                    add(buildBrandCard(), BorderLayout.WEST)
                    add(buildWorkbenchButton(), BorderLayout.EAST)
                },
                BorderLayout.NORTH,
            )
            add(buildRecentRunsSection(), BorderLayout.SOUTH)
        }

    private fun buildBrandCard(): JComponent =
        JBPanel<JBPanel<*>>(BorderLayout(10, 0)).apply {
            isOpaque = false
            add(
                JPanel().apply {
                    background = ToolWindowUiStyles.activeBlue()
                    preferredSize = Dimension(com.intellij.util.ui.JBUI.scale(34), com.intellij.util.ui.JBUI.scale(34))
                },
                BorderLayout.WEST,
            )
            add(
                JBPanel<JBPanel<*>>().apply {
                    layout = BoxLayout(this, BoxLayout.Y_AXIS)
                    isOpaque = false
                    add(
                        JBLabel(PhpDebugToolsBundle.message("toolwindow.chrome.brand.title")).also(
                            ToolWindowUiStyles::applyTitleLabel,
                        ),
                    )
                    add(
                        JBLabel(PhpDebugToolsBundle.message("toolwindow.chrome.brand.subtitle")).also(
                            ToolWindowUiStyles::applyMutedLabel,
                        ),
                    )
                },
                BorderLayout.CENTER,
            )
        }

    private fun buildWorkbenchButton(): JComponent =
        JPanel(FlowLayout(FlowLayout.RIGHT, 0, 0)).apply {
            isOpaque = false
            ToolWindowUiStyles.applyCapsuleButton(workbenchButton, true)
            workbenchButton.isEnabled = false
            add(workbenchButton)
        }

    private fun buildRecentRunsSection(): JComponent =
        JBPanel<JBPanel<*>>(BorderLayout()).apply {
            isOpaque = false
            border = com.intellij.util.ui.JBUI.Borders.empty(0, 14, 14, 14)
            add(
                JBLabel(PhpDebugToolsBundle.message("toolwindow.chrome.recent")).also(
                    ToolWindowUiStyles::applySectionEyebrow,
                ),
                BorderLayout.NORTH,
            )
            recentRunsPanel.isOpaque = false
            add(recentRunsPanel, BorderLayout.CENTER)
        }

    private fun refreshRecentRuns() {
        recentRunsPanel.removeAll()
        val recentMethods = project?.service<RecentDebugStore>()?.state?.recentMethods.orEmpty()
        val items = if (recentMethods.isEmpty()) {
            listOf(PhpDebugToolsBundle.message("toolwindow.chrome.recent.empty"))
        } else {
            recentMethods.take(3)
        }

        items.forEach { text ->
            recentRunsPanel.add(buildRecentRunCard(text))
        }
        recentRunsPanel.revalidate()
        recentRunsPanel.repaint()
    }

    private fun buildRecentRunCard(text: String): JComponent =
        JBPanel<JBPanel<*>>(BorderLayout(0, 6)).apply {
            ToolWindowUiStyles.applySoftCard(this)
            preferredSize = Dimension(com.intellij.util.ui.JBUI.scale(200), com.intellij.util.ui.JBUI.scale(72))
            add(
                JBLabel(PhpDebugToolsBundle.message("toolwindow.chrome.recent.status.idle")).also(
                    ToolWindowUiStyles::applySectionEyebrow,
                ),
                BorderLayout.NORTH,
            )
            add(
                JBLabel(
                    if (text.length > 38) {
                        "${text.take(35)}..."
                    } else {
                        text
                    },
                ).also(ToolWindowUiStyles::applyMutedLabel),
                BorderLayout.CENTER,
            )
        }

    private fun showSection(section: String) {
        contentCardLayout.show(contentHost, section)
        selectedComponent = when (section) {
            ToolWindowSection.OVERVIEW -> overviewComponent
            else -> methodInvokeComponent
        }
    }
}

private object ToolWindowSection {
    const val OVERVIEW = "overview"
    const val METHOD_INVOKE = "methodInvoke"
}
