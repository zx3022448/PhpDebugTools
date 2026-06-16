package com.example.phpdebugtools.ui

import com.example.phpdebugtools.PhpDebugToolsBundle
import com.example.phpdebugtools.methods.MethodDebugTarget
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import java.awt.BorderLayout
import java.awt.CardLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel

class ServiceMethodDialog(
    project: Project,
    private val target: MethodDebugTarget,
) : DialogWrapper(project) {
    private val inputPanel = ServiceMethodInputPanel(target.parameters, ::startParameterEdit)
    private val bottomCardLayout = CardLayout()
    private val bottomPanel = JPanel(bottomCardLayout)
    private val resultArea = JBTextArea().apply {
        lineWrap = true
        wrapStyleWord = true
        isEditable = false
        rows = 6
        text = PhpDebugToolsBundle.message("serviceMethodDialog.parameterEditor.idle")
    }
    private val parameterEditArea = JBTextArea().apply {
        lineWrap = true
        wrapStyleWord = true
        rows = 6
    }
    private val confirmEditButton = JButton(PhpDebugToolsBundle.message("common.ok"))
    private val cancelEditButton = JButton(PhpDebugToolsBundle.message("common.cancel"))
    private var editingParameterName: String? = null
    private var resultSnapshot: String = resultArea.text

    init {
        title = "调试服务方法: ${target.methodName}"
        buildBottomPanel()
        confirmEditButton.addActionListener { confirmParameterEdit() }
        cancelEditButton.addActionListener { cancelParameterEdit() }
        init()
    }

    fun argsJson(): String = inputPanel.argsJson()

    override fun createCenterPanel(): JComponent {
        return JPanel(BorderLayout(0, 8)).apply {
            add(inputPanel, BorderLayout.CENTER)
            add(bottomPanel, BorderLayout.SOUTH)
        }
    }

    private fun buildBottomPanel() {
        bottomPanel.add(JBScrollPane(resultArea), BottomPanelCard.RESULT)
        bottomPanel.add(
            JPanel(BorderLayout()).apply {
                add(JBLabel(PhpDebugToolsBundle.message("toolwindow.methodInvoke.serviceArg.editorTitle")), BorderLayout.NORTH)
                add(JBScrollPane(parameterEditArea), BorderLayout.CENTER)
                add(
                    JPanel().apply {
                        add(confirmEditButton)
                        add(cancelEditButton)
                    },
                    BorderLayout.SOUTH,
                )
            },
            BottomPanelCard.EDITOR,
        )
    }

    private fun startParameterEdit(name: String, value: String) {
        editingParameterName = name
        resultSnapshot = resultArea.text
        parameterEditArea.text = inputPanel.parameterValue(name) ?: value
        parameterEditArea.caretPosition = 0
        bottomCardLayout.show(bottomPanel, BottomPanelCard.EDITOR)
    }

    private fun confirmParameterEdit() {
        val parameterName = editingParameterName ?: return
        inputPanel.setParameterValue(parameterName, parameterEditArea.text)
        cancelParameterEdit(restoreSnapshot = true)
    }

    private fun cancelParameterEdit(restoreSnapshot: Boolean = true) {
        editingParameterName = null
        parameterEditArea.text = ""
        bottomCardLayout.show(bottomPanel, BottomPanelCard.RESULT)
        if (restoreSnapshot) {
            resultArea.text = resultSnapshot
            resultArea.caretPosition = 0
        }
    }
}

private object BottomPanelCard {
    const val RESULT = "result"
    const val EDITOR = "editor"
}
