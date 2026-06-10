package com.example.phpdebugtools.ui

import com.example.phpdebugtools.methods.MethodDebugTarget
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import java.awt.Dimension
import javax.swing.JComponent

class ServiceMethodDialog(
    project: Project,
    private val target: MethodDebugTarget,
) : DialogWrapper(project) {
    private val argsTextArea = JBTextArea("[]")

    init {
        title = "调试服务方法: ${target.methodName}"
        init()
    }

    fun argsJson(): String = argsTextArea.text

    override fun createCenterPanel(): JComponent {
        argsTextArea.minimumSize = Dimension(360, 160)
        return JBScrollPane(argsTextArea)
    }
}
