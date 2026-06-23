package com.example.phpdebugtools.toolwindow

import com.example.phpdebugtools.PhpDebugToolsBundle
import com.example.phpdebugtools.methods.HttpRequestMethod
import com.example.phpdebugtools.methods.RequestBodyMode
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Component
import java.awt.FlowLayout
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JPanel

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
    private val requestTabBar = JPanel(FlowLayout(FlowLayout.LEFT, 8, 0))
    private val requestCardLayout = java.awt.CardLayout()
    private val requestCardPanel = JPanel(requestCardLayout)
    private val bodyModeBar = JPanel(FlowLayout(FlowLayout.LEFT, 8, 0))
    private val bodyCardLayout = java.awt.CardLayout()
    private val bodyCardPanel = JPanel(bodyCardLayout)
    private val queryButton = JButton(PhpDebugToolsBundle.message("toolwindow.methodInvoke.request.tab.query"))
    private val bodyButton = JButton(PhpDebugToolsBundle.message("toolwindow.methodInvoke.request.tab.body"))
    private val headersButton = JButton(PhpDebugToolsBundle.message("toolwindow.methodInvoke.request.tab.headers"))
    private val noneButton = JButton(PhpDebugToolsBundle.message("toolwindow.methodInvoke.body.tab.none"))
    private val formDataButton = JButton(PhpDebugToolsBundle.message("toolwindow.methodInvoke.body.tab.formData"))
    private val urlEncodedButton = JButton(PhpDebugToolsBundle.message("toolwindow.methodInvoke.body.tab.urlEncoded"))
    private val jsonButton = JButton(PhpDebugToolsBundle.message("toolwindow.methodInvoke.body.tab.json"))

    init {
        isOpaque = false
        requestMethodComboBox.addActionListener { syncBodyModeAvailability() }
        ToolWindowUiStyles.applyInputSurface(requestMethodComboBox)
        ToolWindowUiStyles.applyComboPopup(requestMethodComboBox)
        configureActionStyles()
        buildCards()
        add(buildWorkbenchContent(), BorderLayout.CENTER)
        syncBodyModeAvailability()
        showRequestCard(RequestRequestCard.QUERY)
        showBodyCard(RequestBodyCard.JSON)
    }

    fun applyState(state: ControllerRequestViewState) {
        requestMethodComboBox.selectedItem = state.requestMethod
        queryTablePanel.setRows(state.queryParameters)
        headerTablePanel.setRows(state.headerParameters)
        formDataTablePanel.setRows(state.bodyParameters)
        urlEncodedTablePanel.setRows(state.bodyParameters)
        bodyJsonArea.text = state.bodyJsonTemplate
        bodyJsonArea.caretPosition = 0
        showBodyCard(state.bodyMode.toCard())
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

    internal fun bodyTabTitles(): List<String> = listOf(
        noneButton.text,
        formDataButton.text,
        urlEncodedButton.text,
        jsonButton.text,
    )

    private fun buildWorkbenchContent(): JComponent =
        JBPanel<JBPanel<*>>().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = false
            add(buildRequestMethodRow())
            add(Box.createVerticalStrut(JBUI.scale(14)))
            add(requestTabBar.withAlignment())
            add(Box.createVerticalStrut(JBUI.scale(12)))
            add(requestCardPanel.withAlignment())
        }

    private fun buildRequestMethodRow(): JComponent {
        ToolWindowUiStyles.applyToolbarCard(requestMethodRow)
        requestMethodRow.add(
            JBPanel<JBPanel<*>>().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                isOpaque = false
                add(
                    com.intellij.ui.components.JBLabel(
                        PhpDebugToolsBundle.message("toolwindow.methodInvoke.requestMethod.label"),
                    ).also(ToolWindowUiStyles::applySectionEyebrow),
                )
            },
            BorderLayout.WEST,
        )
        requestMethodRow.add(requestMethodComboBox, BorderLayout.CENTER)
        return requestMethodRow.withAlignment()
    }

    private fun configureActionStyles() {
        listOf(queryButton, bodyButton, headersButton, noneButton, formDataButton, urlEncodedButton, jsonButton).forEach {
            ToolWindowUiStyles.applyCapsuleButton(it)
        }
    }

    private fun buildCards() {
        requestTabBar.isOpaque = false
        listOf(
            queryButton to RequestRequestCard.QUERY,
            bodyButton to RequestRequestCard.BODY,
            headersButton to RequestRequestCard.HEADERS,
        ).forEach { (button, card) ->
            button.addActionListener { showRequestCard(card) }
            requestTabBar.add(button)
        }

        bodyModeBar.isOpaque = false
        listOf(
            noneButton to RequestBodyCard.NONE,
            formDataButton to RequestBodyCard.FORM_DATA,
            urlEncodedButton to RequestBodyCard.URL_ENCODED,
            jsonButton to RequestBodyCard.JSON,
        ).forEach { (button, card) ->
            button.addActionListener { showBodyCard(card) }
            bodyModeBar.add(button)
        }

        requestCardPanel.isOpaque = false
        requestCardPanel.add(queryTablePanel, RequestRequestCard.QUERY)
        requestCardPanel.add(buildBodyPanel(), RequestRequestCard.BODY)
        requestCardPanel.add(headerTablePanel, RequestRequestCard.HEADERS)

        bodyCardPanel.isOpaque = false
        bodyCardPanel.add(JBPanel<JBPanel<*>>().apply { isOpaque = false }, RequestBodyCard.NONE)
        bodyCardPanel.add(formDataTablePanel, RequestBodyCard.FORM_DATA)
        bodyCardPanel.add(urlEncodedTablePanel, RequestBodyCard.URL_ENCODED)
        bodyCardPanel.add(createTextAreaScrollPane(bodyJsonArea), RequestBodyCard.JSON)
    }

    private fun buildBodyPanel(): JComponent =
        JBPanel<JBPanel<*>>().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = false
            add(bodyModeBar.withAlignment())
            add(Box.createVerticalStrut(JBUI.scale(12)))
            add(bodyCardPanel.withAlignment())
        }

    private fun syncBodyModeAvailability() {
        val requestMethod = (requestMethodComboBox.selectedItem as? HttpRequestMethod) ?: HttpRequestMethod.GET
        val bodyEnabled = requestMethod.supportsBody
        listOf(formDataButton, urlEncodedButton, jsonButton).forEach { it.isEnabled = bodyEnabled }
        if (!bodyEnabled) {
            showBodyCard(RequestBodyCard.NONE)
        } else if (selectedBodyMode() == RequestBodyMode.NONE) {
            showBodyCard(RequestBodyCard.JSON)
        }
    }

    private fun selectedBodyMode(): RequestBodyMode =
        when (currentBodyCard()) {
            RequestBodyCard.FORM_DATA -> RequestBodyMode.FORM_DATA
            RequestBodyCard.URL_ENCODED -> RequestBodyMode.X_WWW_FORM_URLENCODED
            RequestBodyCard.JSON -> RequestBodyMode.JSON
            else -> RequestBodyMode.NONE
        }

    private fun showRequestCard(card: String) {
        requestCardLayout.show(requestCardPanel, card)
        ToolWindowUiStyles.applyCapsuleButton(queryButton, card == RequestRequestCard.QUERY)
        ToolWindowUiStyles.applyCapsuleButton(bodyButton, card == RequestRequestCard.BODY)
        ToolWindowUiStyles.applyCapsuleButton(headersButton, card == RequestRequestCard.HEADERS)
    }

    private fun showBodyCard(card: String) {
        bodyCardLayout.show(bodyCardPanel, card)
        ToolWindowUiStyles.applyCapsuleButton(noneButton, card == RequestBodyCard.NONE)
        ToolWindowUiStyles.applyCapsuleButton(formDataButton, card == RequestBodyCard.FORM_DATA)
        ToolWindowUiStyles.applyCapsuleButton(urlEncodedButton, card == RequestBodyCard.URL_ENCODED)
        ToolWindowUiStyles.applyCapsuleButton(jsonButton, card == RequestBodyCard.JSON)
        bodyCardPanel.putClientProperty("currentCard", card)
    }

    private fun currentBodyCard(): String =
        bodyCardPanel.getClientProperty("currentCard") as? String ?: RequestBodyCard.NONE

    private fun createTextAreaScrollPane(area: JBTextArea): JBScrollPane =
        JBScrollPane(area).also {
            it.background = area.background
            ToolWindowUiStyles.applyScrollPane(it)
        }

    private fun RequestBodyMode.toCard(): String =
        when (this) {
            RequestBodyMode.NONE -> RequestBodyCard.NONE
            RequestBodyMode.FORM_DATA -> RequestBodyCard.FORM_DATA
            RequestBodyMode.X_WWW_FORM_URLENCODED -> RequestBodyCard.URL_ENCODED
            RequestBodyMode.JSON -> RequestBodyCard.JSON
        }

    private fun <T : JComponent> T.withAlignment(): T = apply {
        alignmentX = Component.LEFT_ALIGNMENT
    }
}

private object RequestRequestCard {
    const val QUERY = "query"
    const val BODY = "body"
    const val HEADERS = "headers"
}

private object RequestBodyCard {
    const val NONE = "none"
    const val FORM_DATA = "formData"
    const val URL_ENCODED = "urlEncoded"
    const val JSON = "json"
}
