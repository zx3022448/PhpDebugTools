package com.example.phpdebugtools.toolwindow

import com.example.phpdebugtools.PhpDebugToolsBundle
import com.example.phpdebugtools.methods.HttpRequestMethod
import com.example.phpdebugtools.methods.RequestBodyMode
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.FormBuilder
import java.awt.BorderLayout
import javax.swing.JComboBox

data class ControllerRequestInput(
    val requestMethod: String,
    val queryJson: String,
    val headerJson: String,
    val bodyMode: String,
    val bodyJson: String,
)

class ControllerRequestEditorPanel : JBPanel<JBPanel<*>>(BorderLayout()) {
    private val requestMethodComboBox = JComboBox(HttpRequestMethod.entries.toTypedArray())
    private val queryTablePanel = RequestParameterTablePanel()
    private val headerTablePanel = RequestParameterTablePanel()
    private val formDataTablePanel = RequestParameterTablePanel()
    private val urlEncodedTablePanel = RequestParameterTablePanel()
    private val bodyJsonArea = JBTextArea("{}").apply {
        lineWrap = true
        wrapStyleWord = true
        rows = 8
    }
    private val requestTabs = JBTabbedPane()
    private val bodyTabs = JBTabbedPane()

    init {
        requestMethodComboBox.addActionListener { syncBodyModeAvailability() }

        bodyTabs.addTab(PhpDebugToolsBundle.message("toolwindow.methodInvoke.body.tab.none"), JBPanel<JBPanel<*>>())
        bodyTabs.addTab(PhpDebugToolsBundle.message("toolwindow.methodInvoke.body.tab.formData"), formDataTablePanel)
        bodyTabs.addTab(PhpDebugToolsBundle.message("toolwindow.methodInvoke.body.tab.urlEncoded"), urlEncodedTablePanel)
        bodyTabs.addTab(PhpDebugToolsBundle.message("toolwindow.methodInvoke.body.tab.json"), JBScrollPane(bodyJsonArea))

        requestTabs.addTab(PhpDebugToolsBundle.message("toolwindow.methodInvoke.request.tab.query"), queryTablePanel)
        requestTabs.addTab(PhpDebugToolsBundle.message("toolwindow.methodInvoke.request.tab.body"), bodyTabs)
        requestTabs.addTab(PhpDebugToolsBundle.message("toolwindow.methodInvoke.request.tab.headers"), headerTablePanel)

        add(
            FormBuilder.createFormBuilder()
                .addLabeledComponent(
                    PhpDebugToolsBundle.message("toolwindow.methodInvoke.requestMethod.label"),
                    requestMethodComboBox,
                    1,
                    false,
                )
                .addComponentFillVertically(requestTabs, 0)
                .panel,
            BorderLayout.CENTER,
        )
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

    private fun selectedBodyMode(): RequestBodyMode {
        return when (bodyTabs.selectedIndex) {
            1 -> RequestBodyMode.FORM_DATA
            2 -> RequestBodyMode.X_WWW_FORM_URLENCODED
            3 -> RequestBodyMode.JSON
            else -> RequestBodyMode.NONE
        }
    }

    private fun bodyModeToTabIndex(bodyMode: RequestBodyMode): Int {
        return when (bodyMode) {
            RequestBodyMode.NONE -> 0
            RequestBodyMode.FORM_DATA -> 1
            RequestBodyMode.X_WWW_FORM_URLENCODED -> 2
            RequestBodyMode.JSON -> 3
        }
    }
}
