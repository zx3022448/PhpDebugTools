package com.example.phpdebugtools.ui

import com.example.phpdebugtools.PhpDebugToolsBundle
import com.example.phpdebugtools.methods.MethodParameterSchema
import com.example.phpdebugtools.toolwindow.RequestParameterDraft
import com.example.phpdebugtools.toolwindow.normalizeParameterType
import com.example.phpdebugtools.toolwindow.serializeDraftValue
import com.example.phpdebugtools.toolwindow.toRequestParameterDraft
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JPanel

internal class ServiceMethodInputPanel(
    parameters: List<MethodParameterSchema>,
    private val onParameterExpandRequested: ((name: String, value: String) -> Unit)? = null,
) : JPanel(BorderLayout()) {
    private val parameterEditors = parameters.map { parameter ->
        ServiceMethodArgumentEditor(parameter) { name, value ->
            onParameterExpandRequested?.invoke(name, value)
        }
    }
    private val argsTextArea = JBTextArea("[]")

    init {
        if (parameterEditors.isEmpty()) {
            argsTextArea.minimumSize = Dimension(360, 160)
            add(JBScrollPane(argsTextArea), BorderLayout.CENTER)
        } else {
            add(JBPanel<JBPanel<*>>().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                parameterEditors.forEach { editor ->
                    add(editor)
                }
            }, BorderLayout.CENTER)
        }
    }

    fun argsJson(): String {
        if (parameterEditors.isEmpty()) {
            return argsTextArea.text.trim().ifEmpty { "[]" }
        }

        return parameterEditors.joinToString(prefix = "[", postfix = "]", separator = ",") { editor ->
            serializeDraftValue(
                RequestParameterDraft(
                    name = editor.parameter.name,
                    type = normalizeParameterType(editor.parameter.declaredType ?: "string"),
                    example = editor.text(),
                    description = if (editor.parameter.required) "必填" else "可选",
                ),
            )
        }
    }

    internal fun hasParameterForm(): Boolean = parameterEditors.isNotEmpty()

    internal fun setParameterRows(rows: List<RequestParameterDraft>) {
        rows.forEach { row ->
            setParameterValue(row.name, row.example)
        }
    }

    internal fun setParameterValue(name: String, value: String) {
        parameterEditors.firstOrNull { it.parameter.name == name }?.setText(value)
    }

    internal fun parameterValue(name: String): String? {
        return parameterEditors.firstOrNull { it.parameter.name == name }?.text()
    }
}

private class ServiceMethodArgumentEditor(
    internal val parameter: MethodParameterSchema,
    private val onExpandRequested: (name: String, value: String) -> Unit,
) : JPanel(BorderLayout()) {
    private val singleLineField = JBTextField()
    private val toggleButton = JButton()

    init {
        val parameterDraft = toRequestParameterDraft(parameter)
        singleLineField.text = parameterDraft.example
        toggleButton.addActionListener {
            onExpandRequested(parameter.name, text())
        }

        add(
            JBLabel(
                buildString {
                    append(parameter.name)
                    append(" (")
                    append(parameter.declaredType ?: "mixed")
                    append(")")
                    append(if (parameter.required) " 必填" else " 可选")
                },
            ),
            BorderLayout.NORTH,
        )
        add(
            JPanel(BorderLayout(8, 0)).apply {
                add(singleLineField, BorderLayout.CENTER)
                add(toggleButton, BorderLayout.EAST)
            },
            BorderLayout.CENTER,
        )
        border = javax.swing.BorderFactory.createEmptyBorder(0, 0, 8, 0)
        toggleButton.text = PhpDebugToolsBundle.message("toolwindow.methodInvoke.serviceArg.expand")
        maximumSize = Dimension(Int.MAX_VALUE, preferredSize.height)
    }

    fun text(): String = singleLineField.text

    fun setText(value: String) {
        singleLineField.text = value
    }
}
