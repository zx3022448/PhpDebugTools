package com.example.phpdebugtools.toolwindow

import org.junit.Assert.assertTrue
import org.junit.Test

class MethodInvokeToolWindowPanelTest {
    @Test
    fun `主面板包含工作台卡片标题`() {
        val panel = MethodInvokeToolWindowPanel(project = null)

        assertTrue(panel.hasWorkbenchSectionTitles())
    }

    @Test
    fun `刷新和执行按钮仍保持同一操作行`() {
        val panel = MethodInvokeToolWindowPanel(project = null)

        assertTrue(panel.areCandidateActionsInline())
    }

    @Test
    fun `执行与刷新按钮保留图标按钮风格`() {
        val panel = MethodInvokeToolWindowPanel(project = null)

        assertTrue(panel.refreshButtonIcon() != null)
        assertTrue(panel.executeButtonIcon() != null)
    }

    @Test
    fun `主面板暴露摘要与结果卡片标题`() {
        val panel = MethodInvokeToolWindowPanel(project = null)

        assertTrue(panel.hasSummaryAndResultSections())
    }

    @Test
    fun `控制器请求编辑器保留请求方式行和 body 标签`() {
        val panel = ControllerRequestEditorPanel()

        assertTrue(panel.hasRequestMethodRow())
        assertTrue(panel.bodyTabTitles().contains("JSON"))
    }

    @Test
    fun `工具窗口主面板使用概览卡片容器`() {
        val tabs = javax.swing.JTabbedPane()
        val panel = PhpDebugToolsToolWindowPanel(tabs, project = null)

        assertTrue(panel.hasOverviewCard())
    }
}
