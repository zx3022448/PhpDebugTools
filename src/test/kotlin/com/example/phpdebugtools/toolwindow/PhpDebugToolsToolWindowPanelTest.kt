package com.example.phpdebugtools.toolwindow

import org.junit.Assert.assertTrue
import org.junit.Test
import java.awt.Component
import java.awt.Container
import javax.swing.JTabbedPane
import javax.swing.JLabel

class PhpDebugToolsToolWindowPanelTest {

    @Test
    fun updateOverviewRendersUpdatedSummaryTexts() {
        val panel = PhpDebugToolsToolWindowPanel(JTabbedPane())

        panel.updateOverview(
            OverviewViewState(
                projectSummary = "ThinkPHP 6",
                runtimeSummary = ".php-debug-tools installed",
                diagnosticsSummary = "1 warning",
            ),
        )

        val renderedTexts = collectLabelTexts(panel.overviewComponent)

        assertTrue(renderedTexts.any { it.contains("ThinkPHP 6") })
        assertTrue(renderedTexts.any { it.contains(".php-debug-tools installed") })
        assertTrue(renderedTexts.any { it.contains("1 warning") })
    }

    private fun collectLabelTexts(component: Component): List<String> {
        val texts = mutableListOf<String>()
        if (component is JLabel) {
            texts += component.text
        }
        if (component is Container) {
            component.components.forEach { child ->
                texts += collectLabelTexts(child)
            }
        }
        return texts
    }
}
