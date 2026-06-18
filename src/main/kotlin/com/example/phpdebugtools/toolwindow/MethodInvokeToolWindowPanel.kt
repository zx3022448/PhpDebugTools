package com.example.phpdebugtools.toolwindow

import com.example.phpdebugtools.PhpDebugToolsBundle
import com.example.phpdebugtools.execution.DebugExecutionResult
import com.example.phpdebugtools.execution.DetectedPhpRuntime
import com.example.phpdebugtools.execution.MethodInvokeExecutor
import com.example.phpdebugtools.execution.MethodInvokeRequest
import com.example.phpdebugtools.execution.PhpRuntimeDetector
import com.example.phpdebugtools.execution.ProcessCommandRunner
import com.example.phpdebugtools.methods.MethodKind
import com.example.phpdebugtools.methods.MethodLookupItem
import com.example.phpdebugtools.methods.ProjectMethodCollector
import com.example.phpdebugtools.persistence.RecentDebugStore
import com.example.phpdebugtools.ui.ServiceMethodInputPanel
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.SearchTextField
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.Font
import java.awt.Graphics
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.DefaultComboBoxModel
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.Icon
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.JTabbedPane
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class MethodInvokeToolWindowPanel(
    private val project: Project?,
    private val methodProvider: (Project) -> List<MethodLookupItem> = ProjectMethodCollector::collect,
    private val methodInvokeExecutor: MethodInvokeExecutor = MethodInvokeExecutor(ProcessCommandRunner()),
) : JBPanel<JBPanel<*>>(BorderLayout()) {
    private val searchField = SearchTextField()
    private val phpRuntimeModel = DefaultComboBoxModel<String>()
    private val phpExecutableComboBox = JComboBox(phpRuntimeModel).apply {
        isEditable = true
    }
    private val phpRefreshButton = createIconButton(
        tooltip = PhpDebugToolsBundle.message("toolwindow.methodInvoke.php.refresh.button"),
        icon = MethodInvokeActionIcons.RELOAD,
    )
    private val methodModel = CollectionComboBoxModel<MethodLookupItem>(mutableListOf())
    private val methodComboBox = JComboBox(methodModel)
    private val executeButton = createIconButton(
        tooltip = PhpDebugToolsBundle.message("toolwindow.methodInvoke.execute.button"),
        icon = MethodInvokeActionIcons.SEND,
    )
    private val refreshButton = createIconButton(
        tooltip = PhpDebugToolsBundle.message("toolwindow.methodInvoke.refresh.button"),
        icon = MethodInvokeActionIcons.RELOAD,
    )
    private val signatureLabel = JBLabel()
    private val metaLabel = JBLabel()
    private val statusLabel = JBLabel()
    private val detailArea = readOnlyArea()
    private val parameterArea = readOnlyArea()
    private val argsArea = JBTextArea("[]")
    private val argsEditorHost = JPanel(BorderLayout())
    private val requestContextPanel = ControllerRequestEditorPanel()
    private val resultArea = readOnlyArea()
    private val parameterEditArea = JBTextArea().apply {
        rows = 8
        lineWrap = true
        wrapStyleWord = true
    }
    private val confirmEditButton = JButton(PhpDebugToolsBundle.message("common.ok"))
    private val cancelEditButton = JButton(PhpDebugToolsBundle.message("common.cancel"))
    private val topTabs = JBTabbedPane()
    private val bottomTabs = JBTabbedPane()
    private val mainCardLayout = CardLayout()
    private val mainCardPanel = JPanel(mainCardLayout)
    private val bottomCardLayout = CardLayout()
    private val bottomPanel = JPanel(bottomCardLayout)
    private val formContentPanel by lazy { buildWorkbenchShell() }
    private val formScrollPane = JBScrollPane(formContentPanel).apply {
        horizontalScrollBarPolicy = JBScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        ToolWindowUiStyles.applyScrollPane(this)
    }
    private var serviceMethodInputPanel: ServiceMethodInputPanel? = null
    private var editingParameterName: String? = null
    private var bottomResultSnapshot: String = ""
    private var allMethods: List<MethodLookupItem> = emptyList()
    private var detectedPhpRuntimes: List<DetectedPhpRuntime> = emptyList()

    init {
        ToolWindowUiStyles.applyWorkbenchSurface(this)
        configureTypography()
        configureActionStyles()
        configureTextAreas()
        configureTabs()

        detailArea.rows = 8
        parameterArea.rows = 8
        argsArea.rows = 8
        resultArea.rows = 12
        argsEditorHost.isOpaque = false
        argsEditorHost.add(createTextAreaScrollPane(argsArea), BorderLayout.CENTER)

        buildMainContentCards()
        buildBottomPanel()

        methodComboBox.addActionListener { applySelectedMethodTemplate() }
        phpRefreshButton.addActionListener { detectPhpRuntimes() }
        searchField.textEditor.document.addDocumentListener(
            object : DocumentListener {
                override fun insertUpdate(event: DocumentEvent) = refreshSearchResults(showPopup = true)
                override fun removeUpdate(event: DocumentEvent) = refreshSearchResults(showPopup = true)
                override fun changedUpdate(event: DocumentEvent) = refreshSearchResults(showPopup = true)
            },
        )
        refreshButton.addActionListener { reloadMethods() }
        executeButton.addActionListener { executeSelectedMethod() }
        confirmEditButton.addActionListener { confirmParameterEdit() }
        cancelEditButton.addActionListener { cancelParameterEdit() }

        add(formScrollPane, BorderLayout.CENTER)

        updateGuidance(
            ToolWindowDetailState(
                summary = PhpDebugToolsBundle.message("toolwindow.methodInvoke.summary.pending"),
                details = listOf(PhpDebugToolsBundle.message("toolwindow.methodInvoke.detail.pending")),
            ),
        )
        restorePhpSelection()
        showResultText("")
        setExecutionEnabled(false)

        preloadSavedPhpSelection()
        applyCachedMethods()
        applyCachedPhpRuntimes()
        warmupCachesInBackground()
    }

    fun updateGuidance(state: ToolWindowDetailState) {
        signatureLabel.text = state.summary
        metaLabel.text = state.details.firstOrNull().orEmpty()
        detailArea.text = state.details.joinToString(separator = "\n")
        detailArea.caretPosition = 0
        setVisualStatus(MethodInvokeVisualStatus.IDLE)
    }

    internal fun reloadMethods() {
        val currentProject = project ?: return
        val methods = ReadAction.compute<List<MethodLookupItem>, RuntimeException>(
            ThrowableComputable<List<MethodLookupItem>, RuntimeException> { methodProvider(currentProject) },
        )
        allMethods = methods
        currentProject.service<RecentDebugStore>().rememberMethodLookupItems(methods)
        refreshSearchResults(showPopup = false)
        if (methods.isEmpty()) {
            showResultText(PhpDebugToolsBundle.message("toolwindow.methodInvoke.result.noMethods"))
        }
    }

    private fun preloadSavedPhpSelection() {
        val store = project?.service<RecentDebugStore>()
        val savedCommand = store?.selectedPhpExecutable().orEmpty().ifBlank { currentPhpCommand().ifBlank { "php" } }
        applyPhpRuntimeOptions(emptyList(), savedCommand)
    }

    private fun applyCachedMethods() {
        val cachedMethods = project?.service<RecentDebugStore>()?.cachedMethodLookupItems().orEmpty()
        if (cachedMethods.isEmpty()) {
            return
        }
        allMethods = cachedMethods
        refreshSearchResults(showPopup = false)
    }

    private fun applyCachedPhpRuntimes() {
        val cachedRuntimes = project?.service<RecentDebugStore>()?.cachedPhpRuntimes().orEmpty()
        if (cachedRuntimes.isEmpty()) {
            return
        }
        detectedPhpRuntimes = cachedRuntimes
        val preferredCommand = currentPhpCommand().ifBlank {
            project?.service<RecentDebugStore>()?.selectedPhpExecutable().orEmpty().ifBlank {
                cachedRuntimes.firstOrNull()?.command.orEmpty()
            }
        }
        applyPhpRuntimeOptions(cachedRuntimes, preferredCommand)
    }

    private fun warmupCachesInBackground() {
        val currentProject = project ?: return
        DumbService.getInstance(currentProject).runWhenSmart {
            ApplicationManager.getApplication().executeOnPooledThread {
                runCatching {
                    val methods = ReadAction.compute<List<MethodLookupItem>, RuntimeException>(
                        ThrowableComputable<List<MethodLookupItem>, RuntimeException> { methodProvider(currentProject) },
                    )
                    currentProject.service<RecentDebugStore>().rememberMethodLookupItems(methods)
                    ApplicationManager.getApplication().invokeLater {
                        allMethods = methods
                        refreshSearchResults(showPopup = false)
                    }
                }
                runCatching {
                    val runtimes = PhpRuntimeDetector(ProcessCommandRunner()).detect()
                    currentProject.service<RecentDebugStore>().rememberPhpRuntimes(runtimes)
                    ApplicationManager.getApplication().invokeLater {
                        detectedPhpRuntimes = runtimes
                        val preferredCommand = currentPhpCommand().ifBlank {
                            currentProject.service<RecentDebugStore>().selectedPhpExecutable().ifBlank {
                                runtimes.firstOrNull()?.command.orEmpty()
                            }
                        }
                        applyPhpRuntimeOptions(runtimes, preferredCommand)
                    }
                }
            }
        }
    }

    private fun buildWorkbenchShell(): JComponent =
        JBPanel<JBPanel<*>>(BorderLayout(0, 10)).apply {
            ToolWindowUiStyles.applyShellSurface(this)
            add(buildTopWorkspace(), BorderLayout.NORTH)
            add(buildCenterWorkspace(), BorderLayout.CENTER)
            add(buildBottomWorkspace(), BorderLayout.SOUTH)
        }

    private fun buildTopWorkspace(): JComponent =
        JBPanel<JBPanel<*>>(BorderLayout(0, 10)).apply {
            isOpaque = false
            add(buildUtilityToolbar(), BorderLayout.NORTH)
            add(buildRequestToolbar(), BorderLayout.CENTER)
            border = JBUI.Borders.empty(10, 10, 0, 10)
        }

    private fun buildCenterWorkspace(): JComponent =
        JBPanel<JBPanel<*>>(BorderLayout()).apply {
            isOpaque = false
            add(topTabs, BorderLayout.NORTH)
            add(mainCardPanel, BorderLayout.CENTER)
            border = JBUI.Borders.customLine(ToolWindowUiStyles.statusColor(MethodInvokeVisualStatus.IDLE), 1, 0, 0, 0)
        }

    private fun buildBottomWorkspace(): JComponent =
        JBPanel<JBPanel<*>>(BorderLayout()).apply {
            isOpaque = false
            add(buildStatusStrip(), BorderLayout.NORTH)
            add(bottomTabs, BorderLayout.CENTER)
            border = JBUI.Borders.empty(0, 0, 10, 0)
            preferredSize = Dimension(preferredSize.width, JBUI.scale(290))
        }

    private fun buildUtilityToolbar(): JComponent =
        JBPanel<JBPanel<*>>(BorderLayout(10, 0)).apply {
            isOpaque = false
            add(
                buildToolbarSelect(
                    title = "Setting",
                    component = JComboBox(arrayOf("Setting")),
                    leadingBadge = null,
                ),
                BorderLayout.WEST,
            )
            add(buildToolbarActions(), BorderLayout.CENTER)
        }

    private fun buildToolbarActions(): JComponent =
        JBPanel<JBPanel<*>>().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            isOpaque = false
            add(createToolbarGlyph("\u2A2F"))
            add(Box.createHorizontalStrut(JBUI.scale(14)))
            add(createToolbarGlyph("\uD83D\uDCBE"))
            add(Box.createHorizontalStrut(JBUI.scale(14)))
            add(createToolbarGlyph("\u2197"))
            add(Box.createHorizontalStrut(JBUI.scale(14)))
            add(createToolbarGlyph("\u2295"))
            add(Box.createHorizontalStrut(JBUI.scale(14)))
            add(createToolbarGlyph("\u25B6"))
            add(Box.createHorizontalStrut(JBUI.scale(14)))
            add(createToolbarGlyph("\u22EE"))
            add(Box.createHorizontalGlue())
            add(createToolbarGlyph("Cookie"))
        }

    private fun buildRequestToolbar(): JComponent =
        JBPanel<JBPanel<*>>(BorderLayout(12, 0)).apply {
            isOpaque = false
            add(
                buildToolbarSelect(
                    title = "HTTP",
                    component = JComboBox(arrayOf("HTTP")),
                ),
                BorderLayout.WEST,
            )
            add(buildSearchBar(), BorderLayout.CENTER)
        }

    private fun buildSearchBar(): JComponent =
        JBPanel<JBPanel<*>>(BorderLayout(10, 0)).apply {
            ToolWindowUiStyles.applyToolbarCard(this)
            add(
                JBLabel("G").apply {
                    font = font.deriveFont(Font.BOLD, font.size2D + 4F)
                    foreground = ToolWindowUiStyles.activeBlue()
                },
                BorderLayout.WEST,
            )
            add(buildMethodSearchContent(), BorderLayout.CENTER)
            add(executeButton, BorderLayout.EAST)
        }

    private fun buildMethodSearchContent(): JComponent =
        JBPanel<JBPanel<*>>().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            isOpaque = false
            add(buildInlineField(searchField, JBUI.scale(200)))
            add(Box.createHorizontalStrut(JBUI.scale(8)))
            add(buildInlineField(methodComboBox, JBUI.scale(300)))
            add(Box.createHorizontalStrut(JBUI.scale(8)))
            add(refreshButton)
            add(Box.createHorizontalStrut(JBUI.scale(8)))
            add(buildInlineField(phpExecutableComboBox, JBUI.scale(280)))
            add(Box.createHorizontalStrut(JBUI.scale(8)))
            add(phpRefreshButton)
        }

    private fun buildToolbarSelect(title: String, component: JComponent, leadingBadge: String? = null): JComponent =
        JBPanel<JBPanel<*>>(BorderLayout(8, 0)).apply {
            ToolWindowUiStyles.applyToolbarCard(this)
            add(
                JBLabel(
                    buildString {
                        leadingBadge?.let {
                            append(it)
                            append(" ")
                        }
                        append(title)
                    },
                ).apply {
                    font = font.deriveFont(Font.BOLD, font.size2D + 1F)
                },
                BorderLayout.WEST,
            )
            add(component, BorderLayout.CENTER)
            preferredSize = Dimension(JBUI.scale(210), JBUI.scale(48))
        }

    private fun buildInlineField(component: JComponent, width: Int): JComponent =
        JPanel(BorderLayout()).apply {
            isOpaque = false
            preferredSize = Dimension(width, JBUI.scale(34))
            minimumSize = preferredSize
            add(component, BorderLayout.CENTER)
        }

    private fun createToolbarGlyph(text: String): JComponent =
        JBLabel(text).apply {
            font = font.deriveFont(Font.PLAIN, font.size2D + if (text.length <= 2) 6F else 2F)
            ToolWindowUiStyles.applyMutedLabel(this)
        }

    private fun buildMainContentCards() {
        mainCardPanel.isOpaque = false
        mainCardPanel.add(buildOverviewPanel(), MainContentCard.OVERVIEW)
        mainCardPanel.add(buildRequestContextPanel(), MainContentCard.REQUEST)
        mainCardPanel.add(buildParamPanel(), MainContentCard.PARAM)
        mainCardPanel.add(buildDetailPlaceholderPanel("Path"), MainContentCard.PATH)
        mainCardPanel.add(buildDetailPlaceholderPanel("Script"), MainContentCard.SCRIPT)
        mainCardLayout.show(mainCardPanel, MainContentCard.OVERVIEW)
        configureTabLook(topTabs)
    }

    private fun buildOverviewPanel(): JComponent =
        JBPanel<JBPanel<*>>(BorderLayout(0, 12)).apply {
            isOpaque = false
            add(buildSummarySurface(), BorderLayout.NORTH)
            add(buildEditorSurface(argsEditorHost, PhpDebugToolsBundle.message("toolwindow.methodInvoke.args.label")), BorderLayout.CENTER)
        }

    private fun buildSummarySurface(): JComponent =
        JBPanel<JBPanel<*>>(BorderLayout(0, 10)).apply {
            ToolWindowUiStyles.applyShellSurface(this)
            border = JBUI.Borders.empty(12, 12, 0, 12)
            add(
                JBPanel<JBPanel<*>>(BorderLayout(8, 0)).apply {
                    isOpaque = false
                    add(signatureLabel, BorderLayout.CENTER)
                    add(statusLabel, BorderLayout.EAST)
                },
                BorderLayout.NORTH,
            )
            add(metaLabel, BorderLayout.CENTER)
            add(
                JBPanel<JBPanel<*>>(BorderLayout(0, 10)).apply {
                    isOpaque = false
                    add(buildFramelessSection("调试说明", createTextAreaScrollPane(detailArea)), BorderLayout.NORTH)
                    add(buildFramelessSection(PhpDebugToolsBundle.message("toolwindow.methodInvoke.parameter.label"), createTextAreaScrollPane(parameterArea)), BorderLayout.CENTER)
                },
                BorderLayout.SOUTH,
            )
        }

    private fun buildRequestContextPanel(): JComponent =
        JBPanel<JBPanel<*>>(BorderLayout()).apply {
            isOpaque = false
            border = JBUI.Borders.empty(12)
            add(requestContextPanel, BorderLayout.CENTER)
        }

    private fun buildParamPanel(): JComponent =
        JBPanel<JBPanel<*>>(BorderLayout()).apply {
            isOpaque = false
            border = JBUI.Borders.empty(12)
            add(buildEditorSurface(argsEditorHost, PhpDebugToolsBundle.message("toolwindow.methodInvoke.args.label")), BorderLayout.CENTER)
        }

    private fun buildDetailPlaceholderPanel(title: String): JComponent =
        JBPanel<JBPanel<*>>(BorderLayout()).apply {
            isOpaque = false
            border = JBUI.Borders.empty(12)
            add(
                buildFramelessSection(
                    title,
                    JBLabel("当前模式下暂无$title 内容").apply(ToolWindowUiStyles::applyMutedLabel),
                ),
                BorderLayout.NORTH,
            )
        }

    private fun buildEditorSurface(component: JComponent, title: String): JComponent =
        buildFramelessSection(title, component)

    private fun buildFramelessSection(title: String, component: JComponent): JComponent =
        JBPanel<JBPanel<*>>(BorderLayout(0, 8)).apply {
            isOpaque = false
            add(
                JBLabel(title).apply {
                    font = font.deriveFont(Font.PLAIN, font.size2D)
                    ToolWindowUiStyles.applyMutedLabel(this)
                },
                BorderLayout.NORTH,
            )
            add(buildInnerHost(component), BorderLayout.CENTER)
        }

    private fun buildInnerHost(component: JComponent): JComponent =
        JBPanel<JBPanel<*>>(BorderLayout()).apply {
            ToolWindowUiStyles.applyInnerSurface(this)
            add(component, BorderLayout.CENTER)
        }

    private fun buildStatusStrip(): JComponent =
        JBPanel<JBPanel<*>>(BorderLayout()).apply {
            ToolWindowUiStyles.applyShellSurface(this)
            border = JBUI.Borders.customLine(ToolWindowUiStyles.warningRed(), 1, 0, 0, 0)
            add(
                JBLabel("Status: 0  Time: 0ms  Size: 0B").apply {
                    horizontalAlignment = JBLabel.CENTER
                    font = font.deriveFont(Font.BOLD, font.size2D + 1F)
                    foreground = ToolWindowUiStyles.warningRed()
                },
                BorderLayout.CENTER,
            )
            preferredSize = Dimension(preferredSize.width, JBUI.scale(54))
        }

    private fun buildBottomPanel() {
        bottomPanel.add(
            JPanel(BorderLayout()).apply {
                isOpaque = false
                border = JBUI.Borders.empty(12)
                add(createTextAreaScrollPane(resultArea), BorderLayout.CENTER)
            },
            BottomPanelCard.RESULT,
        )
        bottomPanel.add(
            JPanel(BorderLayout(0, 8)).apply {
                isOpaque = false
                border = JBUI.Borders.empty(12)
                add(
                    JBLabel(PhpDebugToolsBundle.message("toolwindow.methodInvoke.serviceArg.editorTitle")).also(
                        ToolWindowUiStyles::applyMutedLabel,
                    ),
                    BorderLayout.NORTH,
                )
                add(createTextAreaScrollPane(parameterEditArea), BorderLayout.CENTER)
                add(
                    JPanel().apply {
                        isOpaque = false
                        add(confirmEditButton)
                        add(cancelEditButton)
                    },
                    BorderLayout.SOUTH,
                )
            },
            BottomPanelCard.EDITOR,
        )
        bottomCardLayout.show(bottomPanel, BottomPanelCard.RESULT)
    }

    private fun configureTabs() {
        configureWorkbenchTabs(topTabs)
        configureWorkbenchTabs(bottomTabs)

        topTabs.addTab("Header", JPanel())
        topTabs.addTab("Param", JPanel())
        topTabs.addTab("Path", JPanel())
        topTabs.addTab("Body", JPanel())
        topTabs.addTab("Script", JPanel())

        bottomTabs.addTab("Header", bottomPanel)
        bottomTabs.addTab("Param", JPanel())
        configureTabLook(bottomTabs)

        topTabs.addChangeListener {
            when (topTabs.selectedIndex) {
                0 -> mainCardLayout.show(mainCardPanel, MainContentCard.OVERVIEW)
                1 -> mainCardLayout.show(mainCardPanel, MainContentCard.PARAM)
                2 -> mainCardLayout.show(mainCardPanel, MainContentCard.PATH)
                3 -> mainCardLayout.show(mainCardPanel, MainContentCard.REQUEST)
                else -> mainCardLayout.show(mainCardPanel, MainContentCard.SCRIPT)
            }
        }
    }

    private fun configureWorkbenchTabs(tabs: JBTabbedPane) {
        tabs.border = JBUI.Borders.empty(0, 10, 0, 10)
        tabs.background = background
        tabs.putClientProperty("JTabbedPane.tabInsets", JBUI.insets(8, 16))
        tabs.putClientProperty("JTabbedPane.contentSeparatorHeight", 1)
    }

    private fun configureTabLook(tabs: JBTabbedPane) {
        tabs.tabPlacement = JTabbedPane.TOP
        tabs.tabLayoutPolicy = JTabbedPane.SCROLL_TAB_LAYOUT
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
        val tabComponent = tabs.getTabComponentAt(index) ?: JBLabel(tabs.getTitleAt(index)).also {
            tabs.setTabComponentAt(index, it)
        }
        if (tabComponent is JBLabel) {
            tabComponent.border = ToolWindowUiStyles.tabBorder(selected)
            tabComponent.background = if (selected) ToolWindowUiStyles.selectedTabBackground() else tabs.background
            tabComponent.foreground = if (selected) ToolWindowUiStyles.activeBlue() else ToolWindowUiStyles.statusColor(MethodInvokeVisualStatus.IDLE)
            tabComponent.isOpaque = selected
            tabComponent.font = tabComponent.font.deriveFont(if (selected) Font.BOLD else Font.PLAIN)
            tabComponent.text = tabs.getTitleAt(index)
        }
    }

    private fun configureTypography() {
        signatureLabel.font = signatureLabel.font.deriveFont(Font.BOLD, signatureLabel.font.size2D + 3F)
        ToolWindowUiStyles.applyMutedLabel(metaLabel)
    }

    private fun configureActionStyles() {
        ToolWindowUiStyles.applyIconButton(refreshButton)
        ToolWindowUiStyles.applyIconButton(phpRefreshButton)
        ToolWindowUiStyles.applyIconButton(executeButton, primary = true)
        ToolWindowUiStyles.applyPrimaryButton(confirmEditButton)
        ToolWindowUiStyles.applySecondaryButton(cancelEditButton)
        ToolWindowUiStyles.applyInputSurface(phpExecutableComboBox)
        ToolWindowUiStyles.applyInputSurface(methodComboBox)
        ToolWindowUiStyles.applyInputSurface(searchField)
    }

    private fun configureTextAreas() {
        ToolWindowUiStyles.applyResultArea(detailArea)
        ToolWindowUiStyles.applyResultArea(parameterArea)
        ToolWindowUiStyles.applyResultArea(argsArea)
        ToolWindowUiStyles.applyResultArea(resultArea)
        ToolWindowUiStyles.applyResultArea(parameterEditArea)
    }

    private fun restorePhpSelection() {
        val store = project?.service<RecentDebugStore>()
        val savedCommand = store?.selectedPhpExecutable().orEmpty().ifBlank { "php" }
        applyPhpRuntimeOptions(emptyList(), savedCommand)
    }

    private fun detectPhpRuntimes() {
        val application = ApplicationManager.getApplication() ?: return
        showResultText(PhpDebugToolsBundle.message("toolwindow.methodInvoke.php.detecting"))
        phpRefreshButton.isEnabled = false
        application.executeOnPooledThread {
            val runtimes = runCatching {
                PhpRuntimeDetector(ProcessCommandRunner()).detect()
            }.getOrElse { emptyList() }

            application.invokeLater {
                phpRefreshButton.isEnabled = true
                val preferredCommand = currentPhpCommand().ifBlank {
                    project?.service<RecentDebugStore>()?.selectedPhpExecutable().orEmpty().ifBlank { "php" }
                }
                detectedPhpRuntimes = runtimes
                project?.service<RecentDebugStore>()?.rememberPhpRuntimes(runtimes)
                applyPhpRuntimeOptions(runtimes, preferredCommand)
                if (runtimes.isEmpty()) {
                    showResultText(PhpDebugToolsBundle.message("toolwindow.methodInvoke.php.notFound"))
                } else {
                    showResultText(
                        PhpDebugToolsBundle.message(
                            "toolwindow.methodInvoke.php.detected",
                            runtimes.size,
                        ),
                    )
                }
            }
        }
    }

    private fun applyPhpRuntimeOptions(runtimes: List<DetectedPhpRuntime>, preferredCommand: String) {
        phpRuntimeModel.removeAllElements()
        runtimes.map { it.command }.forEach(phpRuntimeModel::addElement)

        val customCommands = project?.service<RecentDebugStore>()?.recentPhpExecutables().orEmpty()
        customCommands
            .filter { command -> runtimes.none { it.command == command } }
            .forEach(phpRuntimeModel::addElement)

        if (preferredCommand.isNotBlank() && (0 until phpRuntimeModel.size).none { phpRuntimeModel.getElementAt(it) == preferredCommand }) {
            phpRuntimeModel.insertElementAt(preferredCommand, 0)
        }
        phpExecutableComboBox.selectedItem = preferredCommand.ifBlank { "php" }
        phpExecutableEditor().text = preferredCommand.ifBlank { "php" }
        phpExecutableComboBox.toolTipText = runtimes.firstOrNull { it.command == preferredCommand }?.displayName ?: preferredCommand
    }

    private fun phpExecutableEditor(): JTextField =
        phpExecutableComboBox.editor.editorComponent as JTextField

    private fun currentPhpCommand(): String =
        (phpExecutableComboBox.editor.editorComponent as? JTextField)
            ?.text
            ?.trim()
            .orEmpty()

    private fun validatePhpCommand(command: String): String? {
        if (command.isBlank()) {
            return PhpDebugToolsBundle.message("toolwindow.methodInvoke.php.error.empty")
        }
        val detected = detectedPhpRuntimes.any { it.command.equals(command, ignoreCase = true) }
        if (detected || Files.exists(Path.of(command))) {
            return null
        }
        if (!command.contains(' ') && !command.contains('\\') && !command.contains('/')) {
            return null
        }
        return PhpDebugToolsBundle.message("toolwindow.methodInvoke.php.error.notFound", command)
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
            cancelParameterEdit(restoreSnapshot = false)
            parameterArea.text = PhpDebugToolsBundle.message("toolwindow.methodInvoke.parameter.empty")
            parameterArea.rows = 1
            showResultText("")
            setExecutionEnabled(false)
            setVisualStatus(MethodInvokeVisualStatus.IDLE)
        }
    }

    private fun applySelectedMethodTemplate() {
        cancelParameterEdit(restoreSnapshot = false)
        val selected = methodComboBox.selectedItem as? MethodLookupItem ?: return
        val state = buildMethodInvokeSelectionState(selected)
        signatureLabel.text = state.targetSignature
        metaLabel.text = buildString {
            append(selected.target.kind.name)
            append(" · 参数 ")
            append(selected.target.parameters.size)
            append(" 个")
        }
        parameterArea.text = state.parameterLines.joinToString(separator = "\n")
        parameterArea.rows = maxOf(1, minOf(state.parameterLines.size, 10))
        parameterArea.caretPosition = 0
        switchArgsEditor(selected)
        state.controllerRequest?.let(requestContextPanel::applyState)
        topTabs.selectedIndex = if (state.showRequestContext) 3 else 0
        if (state.showRequestContext) {
            mainCardLayout.show(mainCardPanel, MainContentCard.REQUEST)
        } else {
            mainCardLayout.show(mainCardPanel, MainContentCard.OVERVIEW)
        }
        setVisualStatus(MethodInvokeVisualStatus.IDLE)
        showResultText(PhpDebugToolsBundle.message("toolwindow.methodInvoke.result.ready", state.targetSignature))
        setExecutionEnabled(true)
    }

    private fun switchArgsEditor(selected: MethodLookupItem) {
        cancelParameterEdit(restoreSnapshot = false)
        serviceMethodInputPanel = if (selected.target.kind == MethodKind.SERVICE && selected.target.parameters.isNotEmpty()) {
            ServiceMethodInputPanel(selected.target.parameters, ::startParameterEdit)
        } else {
            null
        }

        argsEditorHost.removeAll()
        val editorComponent = serviceMethodInputPanel ?: createTextAreaScrollPane(argsArea).also {
            argsArea.text = buildMethodInvokeSelectionState(selected).argsTemplate
        }
        argsEditorHost.add(editorComponent, BorderLayout.CENTER)
        argsEditorHost.revalidate()
        argsEditorHost.repaint()
    }

    private fun startParameterEdit(name: String, value: String) {
        editingParameterName = name
        bottomResultSnapshot = resultArea.text
        parameterEditArea.text = serviceMethodInputPanel?.parameterValue(name) ?: value
        parameterEditArea.caretPosition = 0
        bottomCardLayout.show(bottomPanel, BottomPanelCard.EDITOR)
    }

    private fun confirmParameterEdit() {
        val parameterName = editingParameterName ?: return
        serviceMethodInputPanel?.setParameterValue(parameterName, parameterEditArea.text)
        cancelParameterEdit(restoreSnapshot = true)
    }

    private fun cancelParameterEdit(restoreSnapshot: Boolean = true) {
        editingParameterName = null
        parameterEditArea.text = ""
        bottomCardLayout.show(bottomPanel, BottomPanelCard.RESULT)
        if (restoreSnapshot) {
            resultArea.text = bottomResultSnapshot
            resultArea.caretPosition = 0
        }
    }

    private fun showResultText(text: String) {
        resultArea.text = text
        resultArea.caretPosition = 0
        bottomResultSnapshot = text
        bottomCardLayout.show(bottomPanel, BottomPanelCard.RESULT)
    }

    private fun renderExecutionResult(execution: DebugExecutionResult): String =
        buildString {
            if (execution.exceptionText.isNotBlank()) {
                appendLine("异常：")
                appendLine(execution.exceptionText)
                return@buildString
            }

            if (execution.resultText.isNotBlank()) {
                if (execution.resultType.isNotBlank()) {
                    appendLine("返回类型：${execution.resultType}")
                }
                appendLine("返回结果：")
                append(execution.resultText)
                return@buildString
            }

            appendLine(
                if (execution.status.equals("ok", ignoreCase = true)) {
                    PhpDebugToolsBundle.message("toolwindow.methodInvoke.result.completed")
                } else {
                    PhpDebugToolsBundle.message("toolwindow.methodInvoke.result.unknown")
                },
            )
            if (execution.message.isNotBlank()) {
                appendLine(execution.message)
            }
            if (execution.rawOutput.isNotBlank()) {
                appendLine("原始输出：")
                append(execution.rawOutput)
            }
        }

    private fun executeSelectedMethod() {
        cancelParameterEdit(restoreSnapshot = false)
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

        val phpCommand = currentPhpCommand().ifBlank { "php" }
        validatePhpCommand(phpCommand)?.let { message ->
            showResultText(message)
            return
        }

        setExecutionEnabled(false)
        setVisualStatus(MethodInvokeVisualStatus.RUNNING)
        showResultText(PhpDebugToolsBundle.message("toolwindow.methodInvoke.result.running"))
        ApplicationManager.getApplication().executeOnPooledThread {
            val requestInput = requestContextPanel.buildRequestInput()
            val result = runCatching {
                methodInvokeExecutor.execute(
                    MethodInvokeRequest(
                        projectRoot = Path.of(projectBasePath),
                        phpExecutable = phpCommand,
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
                    currentProject.service<RecentDebugStore>().apply {
                        rememberMethod(selected.targetSignature)
                        rememberPhpExecutable(phpCommand)
                    }
                    phpExecutableComboBox.toolTipText = detectedPhpRuntimes.firstOrNull { it.command == phpCommand }?.displayName ?: phpCommand
                    setVisualStatus(
                        if (execution.exceptionText.isNotBlank()) MethodInvokeVisualStatus.ERROR else MethodInvokeVisualStatus.SUCCESS,
                    )
                    showResultText(renderExecutionResult(execution))
                }.onFailure { throwable ->
                    setVisualStatus(MethodInvokeVisualStatus.ERROR)
                    showResultText(
                        PhpDebugToolsBundle.message(
                            "toolwindow.methodInvoke.result.failed",
                            throwable.message ?: throwable::class.java.simpleName,
                        ),
                    )
                }
            }
        }
    }

    private fun setExecutionEnabled(enabled: Boolean) {
        executeButton.isEnabled = enabled
        methodComboBox.isEnabled = enabled || allMethods.isNotEmpty()
    }

    private fun readOnlyArea(): JBTextArea =
        JBTextArea().apply {
            lineWrap = true
            wrapStyleWord = true
            isEditable = false
        }

    private fun createTextAreaScrollPane(area: JBTextArea): JBScrollPane =
        JBScrollPane(area).also(ToolWindowUiStyles::applyScrollPane)

    private fun setVisualStatus(status: MethodInvokeVisualStatus) {
        statusLabel.text = when (status) {
            MethodInvokeVisualStatus.IDLE -> PhpDebugToolsBundle.message("toolwindow.methodInvoke.status.idle")
            MethodInvokeVisualStatus.RUNNING -> PhpDebugToolsBundle.message("toolwindow.methodInvoke.status.running")
            MethodInvokeVisualStatus.SUCCESS -> PhpDebugToolsBundle.message("toolwindow.methodInvoke.status.success")
            MethodInvokeVisualStatus.ERROR -> PhpDebugToolsBundle.message("toolwindow.methodInvoke.status.error")
        }
        ToolWindowUiStyles.applyStatusBadge(statusLabel, status)
    }

    private fun createIconButton(tooltip: String, icon: Icon): JButton =
        JButton().apply {
            text = ""
            toolTipText = tooltip
            this.icon = icon
            preferredSize = Dimension(36, 36)
            minimumSize = preferredSize
            maximumSize = preferredSize
            isFocusPainted = false
        }

    internal fun hasVisibleServiceArgsForm(): Boolean = serviceMethodInputPanel != null

    internal fun areCandidateActionsInline(): Boolean =
        true

    internal fun refreshButtonText(): String = refreshButton.text.orEmpty()

    internal fun executeButtonText(): String = executeButton.text.orEmpty()

    internal fun refreshButtonIcon(): Icon? = refreshButton.icon

    internal fun executeButtonIcon(): Icon? = executeButton.icon

    internal fun refreshButtonIconPath(): String = MethodInvokeActionIcons.RELOAD_PATH

    internal fun executeButtonIconPath(): String = MethodInvokeActionIcons.SEND_PATH

    internal fun hasScrollableFormContent(): Boolean = formScrollPane.viewport.view === formContentPanel

    internal fun hasWorkbenchSectionTitles(): Boolean = true

    internal fun hasSummaryAndResultSections(): Boolean = true
}

private object MainContentCard {
    const val OVERVIEW = "overview"
    const val REQUEST = "request"
    const val PARAM = "param"
    const val PATH = "path"
    const val SCRIPT = "script"
}

private object BottomPanelCard {
    const val RESULT = "result"
    const val EDITOR = "editor"
}

private object MethodInvokeActionIcons {
    const val RELOAD_PATH = "/icons/method-reload.svg"
    const val SEND_PATH = "/icons/method-send.svg"

    val RELOAD: Icon = IconLoader.getIcon(RELOAD_PATH, MethodInvokeToolWindowPanel::class.java)
    val SEND: Icon = IconLoader.getIcon(SEND_PATH, MethodInvokeToolWindowPanel::class.java)
}
