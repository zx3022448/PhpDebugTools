package com.example.phpdebugtools.ui

import com.example.phpdebugtools.methods.MethodDebugTarget
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBTextArea
import java.awt.GridLayout
import javax.swing.JComponent
import javax.swing.JPanel

class ControllerMethodDialog(project: Project, private val target: MethodDebugTarget) : DialogWrapper(project) {
    private val queryArea = JBTextArea("{}")
    private val postArea = JBTextArea("{}")
    private val argsArea = JBTextArea("[]")

    init {
        title = "调试控制器方法: ${target.methodName}"
        init()
    }

    override fun createCenterPanel(): JComponent {
        return JPanel(GridLayout(3, 1)).apply {
            add(queryArea)
            add(postArea)
            add(argsArea)
        }
    }

    fun queryJson(): String = queryArea.text

    fun postJson(): String = postArea.text

    fun argsJson(): String = argsArea.text
}
