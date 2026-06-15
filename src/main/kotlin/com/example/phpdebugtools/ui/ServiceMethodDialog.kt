package com.example.phpdebugtools.ui

import com.example.phpdebugtools.methods.MethodDebugTarget
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import javax.swing.JComponent

class ServiceMethodDialog(
    project: Project,
    private val target: MethodDebugTarget,
) : DialogWrapper(project) {
    private val inputPanel = ServiceMethodInputPanel(target.parameters)

    init {
        title = "调试服务方法: ${target.methodName}"
        init()
    }

    fun argsJson(): String = inputPanel.argsJson()

    override fun createCenterPanel(): JComponent {
        return inputPanel
    }
}
