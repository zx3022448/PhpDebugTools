package com.example.phpdebugtools.toolwindow

import com.example.phpdebugtools.PhpDebugToolsBundle
import com.example.phpdebugtools.execution.MethodInvokeExecutor
import com.example.phpdebugtools.execution.MethodInvokeRequest
import com.example.phpdebugtools.execution.ProcessCommandRunner
import com.example.phpdebugtools.methods.MethodLookupItem
import com.example.phpdebugtools.methods.MethodKind
import com.example.phpdebugtools.methods.ProjectMethodCollector
import com.example.phpdebugtools.ui.ServiceMethodInputPanel
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.SearchTextField
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.Icon
import javax.swing.JPanel
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class MethodInvokeToolWindowPanel(
    private val project: Project?,
    private val methodProvider: (Project) -> List<MethodLookupItem> = ProjectMethodCollector::collect,
    private val methodInvokeExecutor: MethodInvokeExecutor = MethodInvokeExecutor(ProcessCommandRunner()),
) : JBPanel<JBPanel<*>>(BorderLayout()) {
    private val summaryLabel = JBLabel()
    private val detailArea = readOnlyArea()
    private val searchField = SearchTextField()
    private val phpExecutableField = JBTextField("php")
    private val methodModel = CollectionComboBoxModel<MethodLookupItem>(mutableListOf())
    private val methodComboBox = JComboBox(methodModel)
    private val parameterArea = readOnlyArea()
    private val argsArea = JBTextArea("[]")
    private val argsEditorHost = JPanel(BorderLayout())
    private val requestContextPanel = ControllerRequestEditorPanel()
    private val executeButton = createIconButton(
        tooltip = PhpDebugToolsBundle.message("toolwindow.methodInvoke.execute.button"),
        icon = MethodInvokeActionIcons.SEND,
    )
    private val refreshButton = createIconButton(
        tooltip = PhpDebugToolsBundle.message("toolwindow.methodInvoke.refresh.button"),
        icon = MethodInvokeActionIcons.RELOAD,
    )
    private val resultArea = readOnlyArea()
    private val formContentPanel by lazy { buildFormContentPanel() }
    private val formScrollPane = JBScrollPane(formContentPanel).apply {
        horizontalScrollBarPolicy = JBScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        border = javax.swing.BorderFactory.createEmptyBorder()
    }
    private var serviceMethodInputPanel: ServiceMethodInputPanel? = null

    private var allMethods: List<MethodLookupItem> = emptyList()

    init {
        detailArea.rows = 3
        parameterArea.rows = 5
        argsArea.rows = 4
        resultArea.rows = 8
        argsEditorHost.add(JBScrollPane(argsArea), BorderLayout.CENTER)

        methodComboBox.addActionListener { applySelectedMethodTemplate() }
        searchField.textEditor.document.addDocumentListener(
            object : DocumentListener {
                override fun insertUpdate(event: DocumentEvent) = refreshSearchResults(showPopup = true)
                override fun removeUpdate(event: DocumentEvent) = refreshSearchResults(showPopup = true)
                override fun changedUpdate(event: DocumentEvent) = refreshSearchResults(showPopup = true)
            },
        )
        refreshButton.addActionListener { reloadMethods() }
        executeButton.addActionListener { executeSelectedMethod() }

        add(buildTopPanel(), BorderLayout.NORTH)
        add(formScrollPane, BorderLayout.CENTER)
        add(JBScrollPane(resultArea), BorderLayout.SOUTH)

        updateGuidance(
            ToolWindowDetailState(
                summary = PhpDebugToolsBundle.message("toolwindow.methodInvoke.summary.pending"),
                details = listOf(PhpDebugToolsBundle.message("toolwindow.methodInvoke.detail.pending")),
            ),
        )
        setExecutionEnabled(false)

        if (project != null) {
            DumbService.getInstance(project).runWhenSmart {
                reloadMethods()
            }
        }
    }

    fun updateGuidance(state: ToolWindowDetailState) {
        summaryLabel.text = state.summary
        detailArea.text = state.details.joinToString(separator = "\n")
        detailArea.caretPosition = 0
    }

    internal fun reloadMethods() {
        val currentProject = project ?: return
        val methods = ReadAction.compute<List<MethodLookupItem>, RuntimeException>(
            ThrowableComputable<List<MethodLookupItem>, RuntimeException> { methodProvider(currentProject) },
        )
        allMethods = methods
        refreshSearchResults(showPopup = false)
        if (methods.isEmpty()) {
            resultArea.text = PhpDebugToolsBundle.message("toolwindow.methodInvoke.result.noMethods")
        }
    }

    private fun buildTopPanel(): JComponent {
        return JBPanel<JBPanel<*>>(BorderLayout()).apply {
            add(summaryLabel, BorderLayout.NORTH)
            add(JBScrollPane(detailArea), BorderLayout.CENTER)
        }
    }

    private fun buildFormContentPanel(): JComponent {
        return FormBuilder.createFormBuilder()
            .addLabeledComponent(PhpDebugToolsBundle.message("toolwindow.methodInvoke.search.label"), searchField, 1, false)
            .addLabeledComponent(
                PhpDebugToolsBundle.message("toolwindow.methodInvoke.candidate.label"),
                buildCandidateSelectorRow(),
                1,
                false,
            )
            .addLabeledComponent(PhpDebugToolsBundle.message("toolwindow.methodInvoke.php.label"), phpExecutableField, 1, false)
            .addLabeledComponent(
                PhpDebugToolsBundle.message("toolwindow.methodInvoke.parameter.label"),
                JBScrollPane(parameterArea),
                1,
                false,
            )
            .addLabeledComponent(PhpDebugToolsBundle.message("toolwindow.methodInvoke.args.label"), argsEditorHost, 1, false)
            .addComponent(requestContextPanel)
            .panel
    }

    private fun buildCandidateSelectorRow(): JComponent {
        return JPanel(BorderLayout(8, 0)).apply {
            add(stretchHorizontally(methodComboBox), BorderLayout.CENTER)
            add(
                JPanel().apply {
                    add(refreshButton)
                    add(executeButton)
                },
                BorderLayout.EAST,
            )
        }
    }

    private fun stretchHorizontally(component: JComponent): JComponent {
        return JPanel(BorderLayout()).apply {
            maximumSize = Dimension(Int.MAX_VALUE, component.preferredSize.height)
            add(component, BorderLayout.CENTER)
        }
    }

    private fun createIconButton(tooltip: String, icon: Icon): JButton {
        return JButton().apply {
            text = ""
            toolTipText = tooltip
            this.icon = icon
            preferredSize = Dimension(30, 30)
            minimumSize = preferredSize
            maximumSize = preferredSize
            isFocusPainted = false
        }
    }

    private fun refreshSearchResults(showPopup: Boolean) {
        val filtered = filterMethodLookupItems(allMethods, searchField.text.trim())
        methodModel.removeAll()
        filtered.forEach(methodModel::add)
        if (filtered.isNotEmpty()) {
            methodComboBox.selectedIndex = 0
            applySelectedMethodTemplate()
            if (showPopup && methodComboBox.isShowing) {
                methodComboBox.showPopup()
            }
        } else {
            parameterArea.text = PhpDebugToolsBundle.message("toolwindow.methodInvoke.parameter.empty")
            setExecutionEnabled(false)
        }
    }

    private fun applySelectedMethodTemplate() {
        val selected = methodComboBox.selectedItem as? MethodLookupItem ?: return
        val state = buildMethodInvokeSelectionState(selected)
        parameterArea.text = state.parameterLines.joinToString(separator = "\n")
        parameterArea.caretPosition = 0
        switchArgsEditor(selected)
        requestContextPanel.isVisible = state.showRequestContext
        state.controllerRequest?.let(requestContextPanel::applyState)
        resultArea.text = PhpDebugToolsBundle.message("toolwindow.methodInvoke.result.ready", state.targetSignature)
        setExecutionEnabled(true)
    }

    private fun switchArgsEditor(selected: MethodLookupItem) {
        serviceMethodInputPanel = if (selected.target.kind == MethodKind.SERVICE && selected.target.parameters.isNotEmpty()) {
            ServiceMethodInputPanel(selected.target.parameters)
        } else {
            null
        }

        argsEditorHost.removeAll()
        val editorComponent = serviceMethodInputPanel ?: JBScrollPane(argsArea).also {
            argsArea.text = buildMethodInvokeSelectionState(selected).argsTemplate
        }
        argsEditorHost.add(editorComponent, BorderLayout.CENTER)
        argsEditorHost.revalidate()
        argsEditorHost.repaint()
    }

    private fun executeSelectedMethod() {
        val currentProject = project
        val selected = methodComboBox.selectedItem as? MethodLookupItem
        if (currentProject == null || selected == null) {
            return
        }

        val projectBasePath = currentProject.basePath
        if (projectBasePath.isNullOrBlank()) {
            Messages.showErrorDialog(
                currentProject,
                PhpDebugToolsBundle.message("toolwindow.methodInvoke.error.projectPath"),
                PhpDebugToolsBundle.message("toolwindow.methodInvoke.execute.button"),
            )
            return
        }

        setExecutionEnabled(false)
        resultArea.text = PhpDebugToolsBundle.message("toolwindow.methodInvoke.result.running")
        ApplicationManager.getApplication().executeOnPooledThread {
            val requestInput = requestContextPanel.buildRequestInput()
            val result = runCatching {
                methodInvokeExecutor.execute(
                    MethodInvokeRequest(
                        projectRoot = java.nio.file.Path.of(projectBasePath),
                        phpExecutable = phpExecutableField.text.trim().ifEmpty { "php" },
                        target = selected.target,
                        argsJson = serviceMethodInputPanel?.argsJson() ?: argsArea.text,
                        requestMethod = requestInput.requestMethod,
                        queryJson = requestInput.queryJson,
                        headerJson = requestInput.headerJson,
                        bodyMode = requestInput.bodyMode,
                        bodyJson = requestInput.bodyJson,
                    ),
                )
            }

            ApplicationManager.getApplication().invokeLater {
                setExecutionEnabled(true)
                result.onSuccess { execution ->
                    currentProject.service<com.example.phpdebugtools.persistence.RecentDebugStore>()
                        .rememberMethod(selected.targetSignature)
                    resultArea.text = buildString {
                        appendLine("status: ${execution.status}")
                        appendLine("stage: ${execution.stage}")
                        if (execution.message.isNotBlank()) {
                            appendLine("message: ${execution.message}")
                        }
                        if (execution.rawOutput.isNotBlank()) {
                            appendLine()
                            append(execution.rawOutput)
                        }
                    }
                }.onFailure { throwable ->
                    resultArea.text = PhpDebugToolsBundle.message(
                        "toolwindow.methodInvoke.result.failed",
                        throwable.message ?: throwable::class.java.simpleName,
                    )
                }
            }
        }
    }

    private fun setExecutionEnabled(enabled: Boolean) {
        executeButton.isEnabled = enabled
        methodComboBox.isEnabled = enabled || allMethods.isNotEmpty()
    }

    private fun readOnlyArea(): JBTextArea {
        return JBTextArea().apply {
            lineWrap = true
            wrapStyleWord = true
            isEditable = false
        }
    }

    internal fun hasVisibleServiceArgsForm(): Boolean = serviceMethodInputPanel != null

    internal fun areCandidateActionsInline(): Boolean =
        refreshButton.parent != null && refreshButton.parent == executeButton.parent && executeButton.parent?.parent == methodComboBox.parent?.parent

    internal fun refreshButtonText(): String = refreshButton.text.orEmpty()

    internal fun executeButtonText(): String = executeButton.text.orEmpty()

    internal fun refreshButtonIcon(): Icon? = refreshButton.icon

    internal fun executeButtonIcon(): Icon? = executeButton.icon

    internal fun refreshButtonIconPath(): String = MethodInvokeActionIcons.RELOAD_PATH

    internal fun executeButtonIconPath(): String = MethodInvokeActionIcons.SEND_PATH

    internal fun hasScrollableFormContent(): Boolean = formScrollPane.viewport.view === formContentPanel
}

private object MethodInvokeActionIcons {
    const val RELOAD_PATH = "/icons/method-reload.svg"
    const val SEND_PATH = "/icons/method-send.svg"

    val RELOAD: Icon = IconLoader.getIcon(RELOAD_PATH, MethodInvokeToolWindowPanel::class.java)
    val SEND: Icon = IconLoader.getIcon(SEND_PATH, MethodInvokeToolWindowPanel::class.java)
}
