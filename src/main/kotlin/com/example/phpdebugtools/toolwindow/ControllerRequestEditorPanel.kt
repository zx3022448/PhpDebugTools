package com.example.phpdebugtools.toolwindow

import com.example.phpdebugtools.PhpDebugToolsBundle
import com.example.phpdebugtools.methods.HttpRequestMethod
import com.example.phpdebugtools.methods.RequestBodyMode
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Component
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JTabbedPane

data class ControllerRequestInput(
    val requestMethod: String,
    val queryJson: String,
    val headerJson: String,
    val bodyMode: String,
    val bodyJson: String,
)

class ControllerRequestEditorPanel : JBPanel<JBPanel<*>>(BorderLayout()) {
    private val requestMethodComboBox = JComboBox(HttpRequestMethod.entries.toTypedArray())
    private val requestMethodRow = JBPanel<JBPanel<*>>(BorderLayout(8, 0))
    private val queryTablePanel = RequestParameterTablePanel()
    private val headerTablePanel = RequestParameterTablePanel()
    private val formDataTablePanel = RequestParameterTablePanel()
    private val urlEncodedTablePanel = RequestParameterTablePanel()
    private val bodyJsonArea = JBTextArea("{}").apply {
        lineWrap = true
        wrapStyleWord = true
        rows = 8
        ToolWindowUiStyles.applyResultArea(this)
    }
    private val requestTabs = JBTabbedPane()
    private val bodyTabs = JBTabbedPane()

    init {
        isOpaque = false
        requestMethodComboBox.addActionListener { syncBodyModeAvailability() }
        ToolWindowUiStyles.applyInputSurface(requestMethodComboBox)

        bodyTabs.addTab(PhpDebugToolsBundle.message("toolwindow.methodInvoke.body.tab.none"), JBPanel<JBPanel<*>>())
        bodyTabs.addTab(PhpDebugToolsBundle.message("toolwindow.methodInvoke.body.tab.formData"), formDataTablePanel)
        bodyTabs.addTab(PhpDebugToolsBundle.message("toolwindow.methodInvoke.body.tab.urlEncoded"), urlEncodedTablePanel)
        bodyTabs.addTab(PhpDebugToolsBundle.message("toolwindow.methodInvoke.body.tab.json"), createTextAreaScrollPane(bodyJsonArea))

        requestTabs.addTab(PhpDebugToolsBundle.message("toolwindow.methodInvoke.request.tab.query"), queryTablePanel)
        requestTabs.addTab(PhpDebugToolsBundle.message("toolwindow.methodInvoke.request.tab.body"), bodyTabs)
        requestTabs.addTab(PhpDebugToolsBundle.message("toolwindow.methodInvoke.request.tab.headers"), headerTablePanel)

        requestTabs.addChangeListener {
            if (requestTabs.selectedIndex == 1) {
                syncBodyModeAvailability()
            }
        }

        configureTabs(requestTabs)
        configureTabs(bodyTabs)
        add(buildWorkbenchContent(), BorderLayout.CENTER)
        syncBodyModeAvailability()
        configureTabLook(requestTabs)
        configureTabLook(bodyTabs)
    }

    fun applyState(state: ControllerRequestViewState) {
        requestMethodComboBox.selectedItem = state.requestMethod
        queryTablePanel.setRows(state.queryParameters)
        headerTablePanel.setRows(state.headerParameters)
        formDataTablePanel.setRows(state.bodyParameters)
        urlEncodedTablePanel.setRows(state.bodyParameters)
        bodyJsonArea.text = state.bodyJsonTemplate
        bodyJsonArea.caretPosition = 0
        bodyTabs.selectedIndex = bodyModeToTabIndex(state.bodyMode)
        syncBodyModeAvailability()
    }

    fun buildRequestInput(): ControllerRequestInput {
        val requestMethod = (requestMethodComboBox.selectedItem as? HttpRequestMethod) ?: HttpRequestMethod.GET
        val bodyMode = selectedBodyMode()
        return ControllerRequestInput(
            requestMethod = requestMethod.wireValue,
            queryJson = requestParameterDraftsToJson(queryTablePanel.rows()),
            headerJson = requestParameterDraftsToJson(headerTablePanel.rows()),
            bodyMode = bodyMode.wireValue,
            bodyJson = when (bodyMode) {
                RequestBodyMode.NONE -> "{}"
                RequestBodyMode.FORM_DATA -> requestParameterDraftsToJson(formDataTablePanel.rows())
                RequestBodyMode.X_WWW_FORM_URLENCODED -> requestParameterDraftsToJson(urlEncodedTablePanel.rows())
                RequestBodyMode.JSON -> bodyJsonArea.text.trim().ifEmpty { "{}" }
            },
        )
    }

    internal fun hasRequestMethodRow(): Boolean = requestMethodComboBox.parent === requestMethodRow

    internal fun bodyTabTitles(): List<String> = (0 until bodyTabs.tabCount).map(bodyTabs::getTitleAt)

    private fun buildWorkbenchContent(): JComponent =
        JBPanel<JBPanel<*>>().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = false
            add(buildRequestMethodRow())
            add(Box.createVerticalStrut(JBUI.scale(12)))
            add(requestTabs.withAlignment())
        }

    private fun buildRequestMethodRow(): JComponent {
        ToolWindowUiStyles.applyToolbarCard(requestMethodRow)
        requestMethodRow.add(
            JBLabel(PhpDebugToolsBundle.message("toolwindow.methodInvoke.requestMethod.label")).also(
                ToolWindowUiStyles::applyMutedLabel,
            ),
            BorderLayout.WEST,
        )
        requestMethodRow.add(requestMethodComboBox, BorderLayout.CENTER)
        return requestMethodRow.withAlignment()
    }

    private fun configureTabs(tabs: JBTabbedPane) {
        tabs.border = JBUI.Borders.empty()
        tabs.background = background
        tabs.tabLayoutPolicy = JTabbedPane.SCROLL_TAB_LAYOUT
        tabs.putClientProperty("JTabbedPane.tabInsets", JBUI.insets(8, 16))
        tabs.putClientProperty("JTabbedPane.contentSeparatorHeight", 1)
        tabs.alignmentX = Component.LEFT_ALIGNMENT
    }

    private fun configureTabLook(tabs: JBTabbedPane) {
        for (index in 0 until tabs.tabCount) {
            styleTab(tabs, index, index == tabs.selectedIndex)
        }
        tabs.addChangeListener {
            for (index in 0 until tabs.tabCount) {
                styleTab(tabs, index, index == tabs.selectedIndex)
            }
        }
    }

    private fun styleTab(tabs: JBTabbedPane, index: Int, selected: Boolean) {
        val label = (tabs.getTabComponentAt(index) as? JBLabel) ?: JBLabel(tabs.getTitleAt(index)).also {
            tabs.setTabComponentAt(index, it)
        }
        label.text = tabs.getTitleAt(index)
        label.border = ToolWindowUiStyles.tabBorder(selected)
        label.background = if (selected) ToolWindowUiStyles.selectedTabBackground() else tabs.background
        label.foreground = if (selected) ToolWindowUiStyles.activeBlue() else ToolWindowUiStyles.statusColor(MethodInvokeVisualStatus.IDLE)
        label.isOpaque = selected
        label.font = label.font.deriveFont(if (selected) java.awt.Font.BOLD else java.awt.Font.PLAIN)
    }

    private fun syncBodyModeAvailability() {
        val requestMethod = (requestMethodComboBox.selectedItem as? HttpRequestMethod) ?: HttpRequestMethod.GET
        val bodyEnabled = requestMethod.supportsBody
        for (index in 1 until bodyTabs.tabCount) {
            bodyTabs.setEnabledAt(index, bodyEnabled)
        }
        if (!bodyEnabled) {
            bodyTabs.selectedIndex = 0
        } else if (bodyTabs.selectedIndex == 0) {
            bodyTabs.selectedIndex = bodyModeToTabIndex(RequestBodyMode.JSON)
        }
    }

    private fun selectedBodyMode(): RequestBodyMode =
        when (bodyTabs.selectedIndex) {
            1 -> RequestBodyMode.FORM_DATA
            2 -> RequestBodyMode.X_WWW_FORM_URLENCODED
            3 -> RequestBodyMode.JSON
            else -> RequestBodyMode.NONE
        }

    private fun bodyModeToTabIndex(bodyMode: RequestBodyMode): Int =
        when (bodyMode) {
            RequestBodyMode.NONE -> 0
            RequestBodyMode.FORM_DATA -> 1
            RequestBodyMode.X_WWW_FORM_URLENCODED -> 2
            RequestBodyMode.JSON -> 3
        }

    private fun createTextAreaScrollPane(area: JBTextArea): JBScrollPane =
        JBScrollPane(area).also(ToolWindowUiStyles::applyScrollPane)

    private fun <T : JComponent> T.withAlignment(): T = apply {
        alignmentX = Component.LEFT_ALIGNMENT
    }
}
