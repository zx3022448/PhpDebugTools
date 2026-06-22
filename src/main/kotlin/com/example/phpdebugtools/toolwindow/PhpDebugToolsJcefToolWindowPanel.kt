package com.example.phpdebugtools.toolwindow

import com.example.phpdebugtools.execution.DebugExecutionResult
import com.example.phpdebugtools.execution.DetectedPhpRuntime
import com.example.phpdebugtools.execution.MethodInvokeExecutor
import com.example.phpdebugtools.execution.MethodInvokeRequest
import com.example.phpdebugtools.execution.PhpRuntimeDetector
import com.example.phpdebugtools.execution.ProcessCommandRunner
import com.example.phpdebugtools.methods.ControllerRequestSpec
import com.example.phpdebugtools.methods.HttpRequestMethod
import com.example.phpdebugtools.methods.MethodKind
import com.example.phpdebugtools.methods.MethodLookupItem
import com.example.phpdebugtools.methods.MethodParameterSchema
import com.example.phpdebugtools.methods.ProjectMethodCollector
import com.example.phpdebugtools.methods.RequestBodyMode
import com.example.phpdebugtools.persistence.RecentDebugStore
import com.google.gson.Gson
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.ui.components.JBPanel
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.jcef.JBCefJSQuery
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

/**
 * 使用 JCEF 承载参考 UI，并将旧 Swing 面板已有的扫描、运行时探测和方法执行能力接入网页层。
 */
class PhpDebugToolsJcefToolWindowPanel(
    private val project: Project?,
    private val methodProvider: (Project) -> List<MethodLookupItem> = ProjectMethodCollector::collect,
    private val methodInvokeExecutor: MethodInvokeExecutor = MethodInvokeExecutor(ProcessCommandRunner()),
    private val phpRuntimeDetector: PhpRuntimeDetector = PhpRuntimeDetector(ProcessCommandRunner()),
) : JBPanel<JBPanel<*>>(BorderLayout()), Disposable {

    private val gson = Gson()
    private val browser = JBCefBrowser()
    private val readyQuery = JBCefJSQuery.create(browser)
    private val refreshQuery = JBCefJSQuery.create(browser)
    private val executeQuery = JBCefJSQuery.create(browser)
    private var workspaceState = createLazyWorkspaceState()
    private var pageReady = false
    private var pendingStateJson: String? = null
    private var methods: List<MethodLookupItem> = emptyList()
    private var runtimes: List<DetectedPhpRuntime> = emptyList()
    private var selectedMethodSignature: String? = null
    private var selectedRuntimeCommand: String = DEFAULT_RUNTIME_COMMAND
    private var executionState = FrontendExecutionState.idle(
        methodSignature = DEFAULT_METHOD_TITLE,
        parameterCount = DEFAULT_QUERY_ROWS.size,
        preview = DEFAULT_OUTPUT_PREVIEW,
    )

    init {
        border = JBUI.Borders.empty()
        restoreCachedState()
        installBridge()
        add(browser.component, BorderLayout.CENTER)
        render()
        warmupCachesInBackground()
    }

    fun updateWorkspace(state: ToolWindowWorkspaceState) {
        workspaceState = state
        syncSelections()
        pushState()
    }

    override fun dispose() {
        readyQuery.dispose()
        refreshQuery.dispose()
        executeQuery.dispose()
        browser.dispose()
    }

    private fun installBridge() {
        readyQuery.addHandler {
            pageReady = true
            pendingStateJson?.let { stateJson ->
                pendingStateJson = null
                pushStateJson(stateJson)
            }
            JBCefJSQuery.Response("""{"ok":true}""")
        }

        refreshQuery.addHandler {
            refreshDataInBackground()
            JBCefJSQuery.Response("""{"accepted":true}""")
        }

        executeQuery.addHandler { payload ->
            val request = runCatching { gson.fromJson(payload, ExecutePayload::class.java) }.getOrElse { throwable ->
                executionState = FrontendExecutionState.error(
                    methodSignature = selectedMethod()?.targetSignature ?: DEFAULT_METHOD_TITLE,
                    parameterCount = selectedMethod()?.target?.parameters?.size ?: 0,
                    message = throwable.message ?: "执行参数解析失败",
                )
                pushState()
                return@addHandler JBCefJSQuery.Response("""{"accepted":false}""")
            }
            executeInBackground(request)
            JBCefJSQuery.Response("""{"accepted":true}""")
        }
    }

    private fun render() {
        val html = loadTemplate()
            .replace(STATE_PLACEHOLDER, currentStateJson())
            .replace(READY_QUERY_PLACEHOLDER, readyQuery.inject("JSON.stringify(payload)", "onSuccess", "onError"))
            .replace(REFRESH_QUERY_PLACEHOLDER, refreshQuery.inject("JSON.stringify(payload)", "onSuccess", "onError"))
            .replace(EXECUTE_QUERY_PLACEHOLDER, executeQuery.inject("JSON.stringify(payload)", "onSuccess", "onError"))
        browser.loadHTML(html)
    }

    private fun restoreCachedState() {
        val store = project?.service<RecentDebugStore>()
        methods = store?.cachedMethodLookupItems().orEmpty()
        runtimes = store?.cachedPhpRuntimes().orEmpty()
        selectedRuntimeCommand = store?.selectedPhpExecutable().orEmpty().ifBlank {
            runtimes.firstOrNull()?.command ?: DEFAULT_RUNTIME_COMMAND
        }
        selectedMethodSignature = preferredMethodSignature(
            recentMethods = store?.state?.recentMethods.orEmpty(),
            availableMethods = methods,
        )
        syncSelections()
    }

    private fun warmupCachesInBackground() {
        val currentProject = project ?: return
        DumbService.getInstance(currentProject).runWhenSmart {
            refreshDataInBackground()
        }
    }

    private fun refreshDataInBackground() {
        val currentProject = project ?: return
        ApplicationManager.getApplication().executeOnPooledThread {
            val refreshedMethods = runCatching {
                ReadAction.compute<List<MethodLookupItem>, RuntimeException>(
                    ThrowableComputable<List<MethodLookupItem>, RuntimeException> { methodProvider(currentProject) },
                )
            }.getOrDefault(methods)

            val refreshedRuntimes = runCatching { phpRuntimeDetector.detect() }.getOrDefault(runtimes)

            currentProject.service<RecentDebugStore>().apply {
                rememberMethodLookupItems(refreshedMethods)
                rememberPhpRuntimes(refreshedRuntimes)
            }

            ApplicationManager.getApplication().invokeLater {
                methods = refreshedMethods
                runtimes = refreshedRuntimes
                syncSelections()
                pushState()
            }
        }
    }

    private fun syncSelections() {
        if (methods.isNotEmpty() && methods.none { it.targetSignature == selectedMethodSignature }) {
            selectedMethodSignature = preferredMethodSignature(
                recentMethods = project?.service<RecentDebugStore>()?.state?.recentMethods.orEmpty(),
                availableMethods = methods,
            )
        }
        if (selectedMethodSignature == null && methods.isNotEmpty()) {
            selectedMethodSignature = methods.first().targetSignature
        }
        if (selectedRuntimeCommand.isBlank()) {
            selectedRuntimeCommand = runtimes.firstOrNull()?.command ?: DEFAULT_RUNTIME_COMMAND
        }
        if (executionState.status == FrontendExecutionStatus.IDLE) {
            val previewRows = selectedMethod()?.target?.parameters?.map { parameter -> toQueryRow(parameter) } ?: DEFAULT_QUERY_ROWS
            executionState = FrontendExecutionState.idle(
                methodSignature = selectedMethod()?.targetSignature ?: DEFAULT_METHOD_TITLE,
                parameterCount = selectedMethod()?.target?.parameters?.size ?: DEFAULT_QUERY_ROWS.size,
                preview = buildOutputPreview(
                    selectedMethod()?.targetSignature ?: DEFAULT_METHOD_TITLE,
                    selectedRuntimeDisplay(),
                    previewRows,
                ),
            )
        }
    }

    private fun executeInBackground(payload: ExecutePayload) {
        val currentProject = project ?: return
        val method = methods.firstOrNull { it.targetSignature == payload.methodSignature }
        if (method == null) {
            executionState = FrontendExecutionState.error(
                methodSignature = payload.methodSignature.ifBlank { DEFAULT_METHOD_TITLE },
                parameterCount = 0,
                message = "未找到可执行的方法：${payload.methodSignature}",
            )
            pushState()
            return
        }

        val projectBasePath = currentProject.basePath
        if (projectBasePath.isNullOrBlank()) {
            executionState = FrontendExecutionState.error(
                methodSignature = method.targetSignature,
                parameterCount = method.target.parameters.size,
                message = "当前项目没有可用的 basePath，暂时无法执行方法直调。",
            )
            pushState()
            return
        }

        val phpCommand = payload.phpCommand.trim().ifBlank { selectedRuntimeCommand.ifBlank { DEFAULT_RUNTIME_COMMAND } }
        validatePhpCommand(phpCommand)?.let { message ->
            executionState = FrontendExecutionState.error(
                methodSignature = method.targetSignature,
                parameterCount = method.target.parameters.size,
                message = message,
            )
            pushState()
            return
        }

        selectedMethodSignature = method.targetSignature
        selectedRuntimeCommand = phpCommand
        executionState = FrontendExecutionState.running(
            methodSignature = method.targetSignature,
            runtimeLabel = runtimeDisplay(phpCommand),
            parameterCount = method.target.parameters.size,
        )
        pushState()

        ApplicationManager.getApplication().executeOnPooledThread {
            val startedAt = System.nanoTime()
            val result = runCatching {
                methodInvokeExecutor.execute(
                    MethodInvokeRequest(
                        projectRoot = Path.of(projectBasePath),
                        phpExecutable = phpCommand,
                        target = method.target,
                        argsJson = payload.argsJson.ifBlank { buildSelectionState(method).argsTemplate },
                        requestMethod = payload.requestMethod.ifBlank { HttpRequestMethod.GET.wireValue },
                        queryJson = requestRowsToJson(payload.queryRows.map(ExecuteRow::toDraft)),
                        headerJson = requestRowsToJson(payload.headerRows.map(ExecuteRow::toDraft)),
                        bodyMode = payload.bodyMode.ifBlank { RequestBodyMode.JSON.wireValue },
                        bodyJson = when (payload.bodyMode) {
                            RequestBodyMode.FORM_DATA.wireValue,
                            RequestBodyMode.X_WWW_FORM_URLENCODED.wireValue -> requestRowsToJson(payload.bodyRows.map(ExecuteRow::toDraft))
                            else -> payload.bodyJson.ifBlank { "{}" }
                        },
                    ),
                )
            }

            ApplicationManager.getApplication().invokeLater {
                val elapsedMs = ((System.nanoTime() - startedAt) / 1_000_000).coerceAtLeast(1)
                result.onSuccess { execution ->
                    currentProject.service<RecentDebugStore>().apply {
                        rememberMethod(method.targetSignature)
                        rememberPhpExecutable(phpCommand)
                    }
                    executionState = FrontendExecutionState.fromExecutionResult(
                        methodSignature = method.targetSignature,
                        parameterCount = method.target.parameters.size,
                        elapsedMs = elapsedMs,
                        result = execution,
                        outputText = renderExecutionResult(execution),
                    )
                }.onFailure { throwable ->
                    executionState = FrontendExecutionState.error(
                        methodSignature = method.targetSignature,
                        parameterCount = method.target.parameters.size,
                        message = throwable.message ?: throwable::class.java.simpleName,
                    )
                }
                pushState()
            }
        }
    }

    private fun validatePhpCommand(command: String): String? {
        if (command.isBlank()) {
            return "请先选择可执行的 PHP 命令。"
        }
        val detected = runtimes.any { it.command.equals(command, ignoreCase = true) }
        if (detected || Files.exists(Path.of(command))) {
            return null
        }
        if (!command.contains(' ') && !command.contains('\\') && !command.contains('/')) {
            return null
        }
        return "未找到可用的 PHP 命令：$command"
    }

    private fun renderExecutionResult(execution: DebugExecutionResult): String {
        return buildString {
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

            appendLine(if (execution.status.equals("ok", ignoreCase = true)) "方法执行完成，但没有可展示的返回值。" else "方法执行结束，但未解析到明确结果。")
            if (execution.message.isNotBlank()) {
                appendLine(execution.message)
            }
            if (execution.rawOutput.isNotBlank()) {
                appendLine("原始输出：")
                append(execution.rawOutput)
            }
        }
    }

    private fun currentStateJson(): String = gson.toJson(buildViewState())

    private fun pushState() {
        val stateJson = currentStateJson()
        if (!pageReady) {
            pendingStateJson = stateJson
            return
        }
        pushStateJson(stateJson)
    }

    private fun pushStateJson(stateJson: String) {
        browser.runJavaScript("window.__PHP_DEBUG_TOOLS__ && window.__PHP_DEBUG_TOOLS__.hydrate($stateJson);")
    }

    private fun buildViewState(): FrontendViewState {
        val recentMethods = project?.service<RecentDebugStore>()?.state?.recentMethods.orEmpty()
            .ifEmpty { DEFAULT_RECENT_METHODS }
            .take(3)
        val exposedMethods = methods.map(::toFrontendMethod).ifEmpty { DEFAULT_METHODS }

        return FrontendViewState(
            projectSummary = workspaceState.overview.projectSummary,
            runtimeSummary = workspaceState.overview.runtimeSummary,
            recentMethods = if (recentMethods.size < 3) recentMethods + DEFAULT_RECENT_METHODS.drop(recentMethods.size) else recentMethods,
            methods = exposedMethods,
            runtimes = buildFrontendRuntimes(selectedRuntimeCommand.ifBlank { DEFAULT_RUNTIME_COMMAND }),
            selectedMethodSignature = selectedMethod()?.targetSignature ?: DEFAULT_METHOD_TITLE,
            selectedRuntimeCommand = selectedRuntimeCommand.ifBlank { DEFAULT_RUNTIME_COMMAND },
            details = workspaceState.methodInvoke.details,
            execution = executionState,
        )
    }

    private fun buildFrontendRuntimes(currentRuntime: String): List<FrontendRuntime> {
        val items = mutableListOf<FrontendRuntime>()
        val seen = linkedSetOf<String>()

        fun add(command: String, label: String, version: String = "") {
            val normalized = command.trim()
            if (normalized.isBlank() || !seen.add(normalized)) {
                return
            }
            items += FrontendRuntime(
                command = normalized,
                label = label,
                primaryText = buildRuntimePrimaryText(version, label),
            )
        }

        runtimes.forEach { add(it.command, it.displayName, it.version) }
        add(currentRuntime, runtimeDisplay(currentRuntime))

        return if (items.isEmpty()) {
            listOf(FrontendRuntime(DEFAULT_RUNTIME_COMMAND, DEFAULT_RUNTIME_LABEL, "PHP 8.2"))
        } else {
            items
        }
    }

    /**
     * 选中态只显示 PHP 版本号，下拉项继续展示完整来源和命令。
     */
    private fun buildRuntimePrimaryText(version: String, label: String): String {
        if (version.isNotBlank()) {
            return "PHP $version"
        }
        return Regex("""PHP\s+\d+(?:\.\d+){0,2}""")
            .find(label)
            ?.value
            ?: label.substringBefore(" [").substringBefore(" - ").ifBlank { "PHP" }
    }

    private fun toFrontendMethod(item: MethodLookupItem): FrontendMethod {
        val selection = buildSelectionState(item)
        return FrontendMethod(
            signature = item.targetSignature,
            searchableText = item.searchableText,
            kind = item.target.kind.name,
            classFqn = item.target.classFqn,
            methodName = item.target.methodName,
            displayName = item.targetSignature,
            parameterCount = item.target.parameters.size,
            parameterLines = selection.parameterLines,
            argsTemplate = selection.argsTemplate,
            showRequestContext = selection.showRequestContext,
            requestMethod = selection.controllerRequest?.requestMethod?.wireValue ?: HttpRequestMethod.GET.wireValue,
            bodyMode = selection.controllerRequest?.bodyMode?.wireValue ?: RequestBodyMode.JSON.wireValue,
            queryRows = selection.controllerRequest?.queryParameters?.map(::toQueryRow) ?: emptyList(),
            headerRows = selection.controllerRequest?.headerParameters?.map(::toQueryRow) ?: emptyList(),
            bodyRows = selection.controllerRequest?.bodyParameters?.map(::toQueryRow) ?: emptyList(),
            bodyJsonTemplate = selection.controllerRequest?.bodyJsonTemplate ?: "{}",
        )
    }

    private fun buildSelectionState(item: MethodLookupItem): JcefSelectionState {
        val controllerRequest = if (item.target.kind == MethodKind.CONTROLLER) {
            buildControllerRequestState(item)
        } else {
            null
        }
        val parameterLines = buildList {
            controllerRequest?.let {
                add("请求上下文：${it.requestMethod.wireValue} / ${it.bodyMode.wireValue}")
            }
            if (item.target.parameters.isEmpty()) {
                add("当前方法没有显式参数。")
            } else {
                addAll(
                    item.target.parameters.mapIndexed { index, parameter ->
                        "${index + 1}. \$${parameter.name}: ${parameter.declaredType ?: "mixed"} ${if (parameter.required) "必填" else "可选"}"
                    },
                )
            }
        }
        return JcefSelectionState(
            parameterLines = parameterLines,
            argsTemplate = buildArgsTemplate(item.target.parameters),
            controllerRequest = controllerRequest,
            showRequestContext = controllerRequest != null,
        )
    }

    private fun buildControllerRequestState(item: MethodLookupItem): JcefControllerRequestState {
        val requestSpec = item.target.controllerRequestSpec ?: fallbackControllerRequestSpec(item)
        val parameterDrafts = item.target.parameters.map(::toRequestDraft)
        return JcefControllerRequestState(
            requestMethod = requestSpec.method,
            bodyMode = requestSpec.bodyMode,
            queryParameters = if (requestSpec.method.supportsBody) emptyList() else parameterDrafts,
            headerParameters = defaultHeaderDrafts(requestSpec),
            bodyParameters = if (requestSpec.method.supportsBody) parameterDrafts else emptyList(),
            bodyJsonTemplate = buildBodyJsonTemplate(item.target.parameters),
        )
    }

    private fun fallbackControllerRequestSpec(item: MethodLookupItem): ControllerRequestSpec {
        val method = when {
            item.target.methodName.startsWith("show", ignoreCase = true) ||
                item.target.methodName.startsWith("get", ignoreCase = true) ||
                item.target.methodName.startsWith("list", ignoreCase = true) -> HttpRequestMethod.GET
            item.target.methodName.startsWith("update", ignoreCase = true) ||
                item.target.methodName.startsWith("edit", ignoreCase = true) -> HttpRequestMethod.PUT
            item.target.methodName.startsWith("delete", ignoreCase = true) ||
                item.target.methodName.startsWith("remove", ignoreCase = true) -> HttpRequestMethod.DELETE
            else -> HttpRequestMethod.POST
        }
        val bodyMode = if (method.supportsBody) {
            if (method == HttpRequestMethod.PUT || method == HttpRequestMethod.PATCH) {
                RequestBodyMode.JSON
            } else {
                RequestBodyMode.X_WWW_FORM_URLENCODED
            }
        } else {
            RequestBodyMode.NONE
        }
        return ControllerRequestSpec(method = method, bodyMode = bodyMode)
    }

    private fun defaultHeaderDrafts(spec: ControllerRequestSpec): List<RequestParameterDraft> {
        val contentType = when (spec.bodyMode) {
            RequestBodyMode.NONE -> return emptyList()
            RequestBodyMode.FORM_DATA -> "multipart/form-data"
            RequestBodyMode.X_WWW_FORM_URLENCODED -> "application/x-www-form-urlencoded"
            RequestBodyMode.JSON -> "application/json"
        }
        return listOf(
            RequestParameterDraft(
                name = "Content-Type",
                type = "string",
                example = contentType,
                description = "自动推断的请求体类型",
            ),
        )
    }

    private fun buildArgsTemplate(parameters: List<MethodParameterSchema>): String {
        return parameters.joinToString(prefix = "[", postfix = "]") { parameter -> defaultJsonValue(parameter) }
    }

    private fun buildBodyJsonTemplate(parameters: List<MethodParameterSchema>): String {
        return parameters.joinToString(prefix = "{", postfix = "}", separator = ",") { parameter ->
            "\"${parameter.name}\":${defaultJsonValue(parameter)}"
        }.ifEmpty { "{}" }
    }

    private fun defaultJsonValue(parameter: MethodParameterSchema): String {
        if (!parameter.required) {
            return parameter.defaultValue ?: "null"
        }
        return when (normalizeDraftType(parameter.declaredType ?: "string")) {
            "integer" -> "0"
            "number" -> "0.0"
            "boolean" -> "false"
            "array" -> "[]"
            "object" -> "{}"
            else -> "\"\""
        }
    }

    private fun toRequestDraft(parameter: MethodParameterSchema): RequestParameterDraft {
        return RequestParameterDraft(
            name = parameter.name,
            type = normalizeDraftType(parameter.declaredType ?: "string"),
            example = defaultRowExample(parameter),
            description = if (parameter.required) "必填" else "可选",
        )
    }

    private fun toQueryRow(draft: RequestParameterDraft): QueryRow {
        return QueryRow(
            name = draft.name,
            type = draft.type,
            example = draft.example,
            description = draft.description,
        )
    }

    private fun toQueryRow(parameter: MethodParameterSchema): QueryRow {
        return QueryRow(
            name = parameter.name,
            type = normalizeDraftType(parameter.declaredType ?: "string"),
            example = defaultRowExample(parameter),
            description = if (parameter.required) "必填参数" else "可选参数",
        )
    }

    private fun normalizeDraftType(type: String): String {
        val normalized = type.lowercase()
        return when {
            "int" in normalized -> "integer"
            "float" in normalized || "double" in normalized || "number" in normalized -> "number"
            "bool" in normalized -> "boolean"
            "array" in normalized -> "array"
            "object" in normalized -> "object"
            else -> "string"
        }
    }

    private fun defaultRowExample(parameter: MethodParameterSchema): String {
        val defaultValue = parameter.defaultValue?.trim()
        if (!defaultValue.isNullOrEmpty()) {
            return defaultValue
        }
        return when (normalizeDraftType(parameter.declaredType ?: "string")) {
            "integer" -> "0"
            "number" -> "0.0"
            "boolean" -> "false"
            "array" -> "[]"
            "object" -> "{}"
            else -> ""
        }
    }

    private fun requestRowsToJson(rows: List<RequestParameterDraft>): String {
        val filteredRows = rows.filter { it.name.isNotBlank() }
        if (filteredRows.isEmpty()) {
            return "{}"
        }
        return filteredRows.joinToString(prefix = "{", postfix = "}", separator = ",") { row ->
            "\"${escapeJsonText(row.name)}\":${serializeDraftValue(row)}"
        }
    }

    private fun serializeDraftValue(row: RequestParameterDraft): String {
        val type = row.type.trim().lowercase()
        val example = row.example.trim()
        if (example.isEmpty()) {
            return when (type) {
                "integer" -> "0"
                "number" -> "0.0"
                "boolean" -> "false"
                "array" -> "[]"
                "object" -> "{}"
                else -> "\"\""
            }
        }
        return when (type) {
            "integer" -> example.toLongOrNull()?.toString() ?: "0"
            "number" -> example.toDoubleOrNull()?.let {
                if (example.contains('.')) example else "${it.toLong()}.0"
            } ?: "0.0"
            "boolean" -> if (example.equals("true", ignoreCase = true) || example == "1") "true" else "false"
            "array", "object" -> example
            else -> "\"${escapeJsonText(example.trim('"'))}\""
        }
    }

    private fun selectedMethod(): MethodLookupItem? {
        return methods.firstOrNull { it.targetSignature == selectedMethodSignature }
    }

    private fun runtimeDisplay(command: String): String {
        return runtimes.firstOrNull { it.command == command }?.displayName ?: if (command == DEFAULT_RUNTIME_COMMAND) {
            DEFAULT_RUNTIME_LABEL
        } else {
            command
        }
    }

    private fun selectedRuntimeDisplay(): String = runtimeDisplay(selectedRuntimeCommand)

    private fun buildOutputPreview(
        methodTitle: String,
        runtimeLabel: String,
        queryRows: List<QueryRow>,
    ): String {
        val rowLines = if (queryRows.isEmpty()) {
            listOf("    \"debug\": true")
        } else {
            queryRows.mapIndexed { index, row ->
                val suffix = if (index == queryRows.lastIndex) "" else ","
                "    \"${escapeJsonText(row.name)}\": ${serializeDraftValue(row.toDraft())}$suffix"
            }
        }
        return buildString {
            appendLine("{")
            appendLine("  \"status\": \"waiting\",")
            appendLine("  \"runtime\": \"${escapeJsonText(runtimeLabel)}\",")
            appendLine("  \"method\": \"${escapeJsonText(methodTitle)}\",")
            appendLine("  \"query\": {")
            append(rowLines.joinToString(separator = "\n"))
            appendLine()
            appendLine("  }")
            append('}')
        }
    }

    private fun preferredMethodSignature(
        recentMethods: List<String>,
        availableMethods: List<MethodLookupItem>,
    ): String? {
        return recentMethods.firstNotNullOfOrNull { recent ->
            availableMethods.firstOrNull { it.targetSignature == recent }?.targetSignature
        } ?: availableMethods.firstOrNull()?.targetSignature
    }

    private fun escapeJsonText(value: String): String {
        return buildString(value.length) {
            value.forEach { char ->
                when (char) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(char)
                }
            }
        }
    }

    private fun loadTemplate(): String {
        return javaClass.classLoader
            .getResourceAsStream(TEMPLATE_RESOURCE)
            ?.bufferedReader(StandardCharsets.UTF_8)
            ?.use { it.readText() }
            ?: error("Missing resource: $TEMPLATE_RESOURCE")
    }

    private fun createLazyWorkspaceState(): ToolWindowWorkspaceState {
        return ToolWindowWorkspaceState(
            overview = OverviewViewState(
                projectSummary = "等待项目检测",
                runtimeSummary = "等待运行时检查",
            ),
            methodInvoke = ToolWindowDetailState(
                summary = "等待生成方法直调说明",
                details = listOf("这里会展示服务方法和控制器方法的搜索、参数识别与执行入口。"),
            ),
        )
    }

    private companion object {
        const val TEMPLATE_RESOURCE = "web/toolwindow-template.html"
        const val STATE_PLACEHOLDER = "__STATE_JSON__"
        const val READY_QUERY_PLACEHOLDER = "__READY_QUERY__"
        const val REFRESH_QUERY_PLACEHOLDER = "__REFRESH_QUERY__"
        const val EXECUTE_QUERY_PLACEHOLDER = "__EXECUTE_QUERY__"
        const val DEFAULT_METHOD_TITLE = "\\Admin::__construct"
        const val DEFAULT_RUNTIME_COMMAND = "php"
        const val DEFAULT_RUNTIME_LABEL = "PHP 8.2 - Local"
        val DEFAULT_QUERY_ROWS = listOf(
            QueryRow(name = "debug", type = "boolean", example = "true", description = "输出调试信息"),
            QueryRow(name = "trace_id", type = "string", example = "req_6f2a", description = "调用链路标识"),
            QueryRow(name = "limit", type = "integer", example = "20", description = "返回数量限制"),
        )
        val DEFAULT_RECENT_METHODS = listOf("\\Api\\User::login", "\\Task\\Sync::run", "\\Admin::__construct")
        val DEFAULT_OUTPUT_PREVIEW = buildString {
            appendLine("{")
            appendLine("  \"status\": \"waiting\",")
            appendLine("  \"runtime\": \"PHP 8.2\",")
            appendLine("  \"method\": \"\\Admin::__construct\",")
            appendLine("  \"query\": {")
            appendLine("    \"debug\": true,")
            appendLine("    \"trace_id\": \"req_6f2a\",")
            appendLine("    \"limit\": 20")
            appendLine("  }")
            append('}')
        }
        val DEFAULT_METHODS = listOf(
            FrontendMethod(
                signature = "\\Admin::__construct",
                searchableText = "\\admin __construct service",
                kind = MethodKind.SERVICE.name,
                classFqn = "\\Admin",
                methodName = "__construct",
                displayName = "\\Admin::__construct",
                parameterCount = 3,
                parameterLines = listOf(
                    "1. \$debug: boolean 可选",
                    "2. \$trace_id: string 可选",
                    "3. \$limit: int 可选",
                ),
                argsTemplate = "[true,\"req_6f2a\",20]",
                showRequestContext = false,
                requestMethod = HttpRequestMethod.GET.wireValue,
                bodyMode = RequestBodyMode.JSON.wireValue,
                queryRows = DEFAULT_QUERY_ROWS,
                headerRows = emptyList(),
                bodyRows = emptyList(),
                bodyJsonTemplate = "{}",
            ),
        )
    }
}

private data class FrontendViewState(
    val projectSummary: String,
    val runtimeSummary: String,
    val recentMethods: List<String>,
    val methods: List<FrontendMethod>,
    val runtimes: List<FrontendRuntime>,
    val selectedMethodSignature: String,
    val selectedRuntimeCommand: String,
    val details: List<String>,
    val execution: FrontendExecutionState,
)

private data class FrontendMethod(
    val signature: String,
    val searchableText: String,
    val kind: String,
    val classFqn: String,
    val methodName: String,
    val displayName: String,
    val parameterCount: Int,
    val parameterLines: List<String>,
    val argsTemplate: String,
    val showRequestContext: Boolean,
    val requestMethod: String,
    val bodyMode: String,
    val queryRows: List<QueryRow>,
    val headerRows: List<QueryRow>,
    val bodyRows: List<QueryRow>,
    val bodyJsonTemplate: String,
)

private data class FrontendRuntime(
    val command: String,
    val label: String,
    val primaryText: String,
)

private data class QueryRow(
    val name: String,
    val type: String,
    val example: String,
    val description: String,
) {
    fun toDraft(): RequestParameterDraft {
        return RequestParameterDraft(
            name = name,
            type = type,
            example = example,
            description = description,
        )
    }
}

private data class JcefSelectionState(
    val parameterLines: List<String>,
    val argsTemplate: String,
    val controllerRequest: JcefControllerRequestState?,
    val showRequestContext: Boolean,
)

private data class JcefControllerRequestState(
    val requestMethod: HttpRequestMethod,
    val bodyMode: RequestBodyMode,
    val queryParameters: List<RequestParameterDraft>,
    val headerParameters: List<RequestParameterDraft>,
    val bodyParameters: List<RequestParameterDraft>,
    val bodyJsonTemplate: String,
)

private enum class FrontendExecutionStatus {
    IDLE,
    RUNNING,
    SUCCESS,
    ERROR,
}

private data class FrontendExecutionState(
    val status: FrontendExecutionStatus,
    val title: String,
    val subtitle: String,
    val elapsedLabel: String,
    val stateLabel: String,
    val parameterLabel: String,
    val outputStatus: String,
    val outputRuntime: String,
    val outputText: String,
) {
    companion object {
        fun idle(
            methodSignature: String,
            parameterCount: Int,
            preview: String,
        ): FrontendExecutionState {
            return FrontendExecutionState(
                status = FrontendExecutionStatus.IDLE,
                title = "待执行",
                subtitle = "已选择方法：$methodSignature",
                elapsedLabel = "18 ms",
                stateLabel = "Ready",
                parameterLabel = parameterCount.toString(),
                outputStatus = "status: waiting",
                outputRuntime = "18 ms",
                outputText = preview,
            )
        }

        fun running(
            methodSignature: String,
            runtimeLabel: String,
            parameterCount: Int,
        ): FrontendExecutionState {
            return FrontendExecutionState(
                status = FrontendExecutionStatus.RUNNING,
                title = "执行中",
                subtitle = "正在通过 $runtimeLabel 执行：$methodSignature",
                elapsedLabel = "--",
                stateLabel = "Running",
                parameterLabel = parameterCount.toString(),
                outputStatus = "status: running",
                outputRuntime = runtimeLabel,
                outputText = "正在执行方法直调，请稍候...",
            )
        }

        fun error(
            methodSignature: String,
            parameterCount: Int,
            message: String,
        ): FrontendExecutionState {
            return FrontendExecutionState(
                status = FrontendExecutionStatus.ERROR,
                title = "执行异常",
                subtitle = message,
                elapsedLabel = "--",
                stateLabel = "Error",
                parameterLabel = parameterCount.toString(),
                outputStatus = "status: error",
                outputRuntime = message,
                outputText = message,
            )
        }

        fun fromExecutionResult(
            methodSignature: String,
            parameterCount: Int,
            elapsedMs: Long,
            result: DebugExecutionResult,
            outputText: String,
        ): FrontendExecutionState {
            val hasError = result.exceptionText.isNotBlank()
            return FrontendExecutionState(
                status = if (hasError) FrontendExecutionStatus.ERROR else FrontendExecutionStatus.SUCCESS,
                title = if (hasError) "执行异常" else "执行完成",
                subtitle = if (hasError) {
                    if (result.message.isBlank()) "方法执行出现异常" else result.message
                } else {
                    "已执行方法：$methodSignature"
                },
                elapsedLabel = "${elapsedMs} ms",
                stateLabel = if (hasError) "Error" else "Success",
                parameterLabel = parameterCount.toString(),
                outputStatus = "status: ${result.status.ifBlank { if (hasError) "error" else "ok" }}",
                outputRuntime = "${elapsedMs} ms",
                outputText = outputText,
            )
        }
    }
}

private data class ExecutePayload(
    val methodSignature: String = "",
    val phpCommand: String = "",
    val requestMethod: String = "",
    val bodyMode: String = "",
    val argsJson: String = "[]",
    val bodyJson: String = "{}",
    val queryRows: List<ExecuteRow> = emptyList(),
    val headerRows: List<ExecuteRow> = emptyList(),
    val bodyRows: List<ExecuteRow> = emptyList(),
)

private data class ExecuteRow(
    val name: String = "",
    val type: String = "string",
    val example: String = "",
    val description: String = "",
) {
    fun toDraft(): RequestParameterDraft {
        return RequestParameterDraft(
            name = name,
            type = type,
            example = example,
            description = description,
        )
    }
}
