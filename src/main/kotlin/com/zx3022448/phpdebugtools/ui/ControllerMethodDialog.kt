package com.zx3022448.phpdebugtools.ui

import com.zx3022448.phpdebugtools.methods.MethodDebugTarget
import com.zx3022448.phpdebugtools.methods.HttpRequestMethod
import com.zx3022448.phpdebugtools.methods.RequestBodyMode
import com.zx3022448.phpdebugtools.toolwindow.ControllerRequestEditorPanel
import com.zx3022448.phpdebugtools.toolwindow.ControllerRequestViewState
import com.zx3022448.phpdebugtools.toolwindow.RequestParameterDraft
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

class ControllerMethodDialog(project: Project, private val target: MethodDebugTarget) : DialogWrapper(project) {
    private val requestEditorPanel = ControllerRequestEditorPanel()

    init {
        title = "调试控制器方法: ${target.methodName}"
        requestEditorPanel.applyState(
            ControllerRequestViewState(
                requestMethod = target.controllerRequestSpec?.method ?: HttpRequestMethod.GET,
                bodyMode = target.controllerRequestSpec?.bodyMode ?: RequestBodyMode.NONE,
                queryParameters = emptyList(),
                headerParameters = emptyList(),
                bodyParameters = target.parameters.map {
                    RequestParameterDraft(
                        name = it.name,
                        type = it.declaredType ?: "string",
                        description = if (it.required) "必填" else "可选",
                    )
                },
                bodyJsonTemplate = "{}",
            ),
        )
        init()
    }

    override fun createCenterPanel(): JComponent {
        return JPanel(BorderLayout()).apply {
            add(requestEditorPanel, BorderLayout.CENTER)
        }
    }

    fun queryJson(): String = requestEditorPanel.buildRequestInput().queryJson

    fun headerJson(): String = requestEditorPanel.buildRequestInput().headerJson

    fun bodyMode(): String = requestEditorPanel.buildRequestInput().bodyMode

    fun bodyJson(): String = requestEditorPanel.buildRequestInput().bodyJson
}
