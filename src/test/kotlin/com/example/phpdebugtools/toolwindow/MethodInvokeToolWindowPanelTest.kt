package com.example.phpdebugtools.toolwindow

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import com.example.phpdebugtools.execution.DetectedPhpRuntime
import com.intellij.openapi.project.DumbAware
import com.intellij.ui.components.JBPanel
import org.junit.Test
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Container
import javax.swing.JLabel
import javax.swing.JPanel

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
    fun `PHP 运行时选中后只显示版本但执行仍使用真实路径`() {
        val panel = MethodInvokeToolWindowPanel(project = null)
        val runtime = DetectedPhpRuntime(
            command = "D:/php/php.exe",
            version = "8.3.12",
            source = "scan",
        )

        panel.applyPhpRuntimeOptionsForTest(listOf(runtime), runtime.command)

        assertEquals("PHP 8.3.12", panel.phpRuntimeEditorTextForTest())
        assertEquals("D:/php/php.exe", panel.currentPhpCommandForTest())
    }

    @Test
    fun `主面板暴露摘要与结果卡片标题`() {
        val panel = MethodInvokeToolWindowPanel(project = null)

        assertTrue(panel.hasSummaryAndResultSections())
    }

    @Test
    fun `参数编辑区域不会被重复挂载导致空白`() {
        val panel = MethodInvokeToolWindowPanel(project = null)

        assertTrue(panel.titledSections("args JSON").all { it.hasCenterContent() })
    }

    @Test
    fun `控制器请求编辑器保留请求方式行和 body 标签`() {
        val panel = ControllerRequestEditorPanel()

        assertTrue(panel.hasRequestMethodRow())
        assertTrue(panel.bodyTabTitles().contains("JSON"))
    }

    @Test
    fun `工具窗口主面板使用概览卡片容器`() {
        val tabs = com.intellij.ui.components.JBTabbedPane()
        val panel = PhpDebugToolsToolWindowPanel(tabs, project = null)

        assertTrue(panel.hasOverviewCard())
    }

    @Test
    fun `工具窗口主面板使用可绘制容器避免恢复后空白`() {
        val tabs = com.intellij.ui.components.JBTabbedPane()
        val panel = PhpDebugToolsToolWindowPanel(tabs, project = null)
        val rootComponent: Any = panel

        assertTrue(rootComponent is JBPanel<*>)
        assertTrue(panel.isOpaque)
    }

    @Test
    fun `工具窗口工厂支持索引期间加载`() {
        val factory: Any = PhpDebugToolsToolWindowFactory()

        assertTrue(factory is DumbAware)
    }

    @Test
    fun `工具窗口创建后默认打开方法直调页避免首屏空白`() {
        val panel = PhpDebugToolsToolWindowPanel(project = null)

        assertTrue(panel.selectedToolWindowComponent() === panel.methodInvokeComponent)
        assertTrue((panel.methodInvokeComponent as MethodInvokeToolWindowPanel).hasScrollableFormContent())
    }
}

private fun Container.titledSections(title: String): List<Container> {
    val sections = mutableListOf<Container>()
    collectTitledSections(title, sections)
    return sections
}

private fun Component.collectTitledSections(title: String, sections: MutableList<Container>) {
    if (this is JLabel && text == title) {
        (parent as? Container)?.let(sections::add)
    }
    if (this is Container) {
        components.forEach { it.collectTitledSections(title, sections) }
    }
}

private fun Container.hasCenterContent(): Boolean {
    val borderLayout = layout as? BorderLayout ?: return false
    val center = borderLayout.getLayoutComponent(BorderLayout.CENTER) as? JPanel ?: return false
    return center.componentCount > 0
}
