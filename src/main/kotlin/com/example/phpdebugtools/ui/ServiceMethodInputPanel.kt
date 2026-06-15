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
import java.awt.Rectangle
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JViewport
import javax.swing.SwingUtilities

internal class ServiceMethodInputPanel(parameters: List<MethodParameterSchema>) : JPanel(BorderLayout()) {
    private val parameterEditors = parameters.map { ServiceMethodArgumentEditor(it) }
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

    internal fun toggleParameterExpanded(name: String) {
        parameterEditors.firstOrNull { it.parameter.name == name }?.toggleExpanded()
    }

    internal fun isParameterExpanded(name: String): Boolean {
        return parameterEditors.firstOrNull { it.parameter.name == name }?.isExpanded() == true
    }

    internal fun editorPreferredHeight(name: String): Int {
        return parameterEditors.firstOrNull { it.parameter.name == name }?.preferredSize?.height ?: 0
    }
}

private class ServiceMethodArgumentEditor(
    internal val parameter: MethodParameterSchema,
) : JPanel(BorderLayout()) {
    private val singleLineField = JBTextField()
    private val multiLineArea = JBTextArea().apply {
        rows = 4
        lineWrap = true
        wrapStyleWord = true
    }
    private val multiLineScrollPane = JBScrollPane(multiLineArea).apply {
        preferredSize = Dimension(200, 96)
    }
    private val editorHost = JPanel(BorderLayout())
    private val toggleButton = JButton()
    private var expanded = false

    init {
        val parameterDraft = toRequestParameterDraft(parameter)
        singleLineField.text = parameterDraft.example
        multiLineArea.text = parameterDraft.example

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
                add(editorHost, BorderLayout.CENTER)
                add(toggleButton, BorderLayout.EAST)
            },
            BorderLayout.CENTER,
        )
        border = javax.swing.BorderFactory.createEmptyBorder(0, 0, 8, 0)
        applyExpandedState()
    }

    fun text(): String = if (expanded) multiLineArea.text else singleLineField.text

    fun setText(value: String) {
        singleLineField.text = value
        multiLineArea.text = value
    }

    fun toggleExpanded() {
        setExpanded(!expanded)
    }

    fun isExpanded(): Boolean = expanded

    private fun setExpanded(value: Boolean) {
        val current = text()
        expanded = value
        setText(current)
        applyExpandedState()
    }

    private fun applyExpandedState() {
        editorHost.removeAll()
        editorHost.add(if (expanded) multiLineScrollPane else singleLineField, BorderLayout.CENTER)
        toggleButton.text = if (expanded) {
            PhpDebugToolsBundle.message("toolwindow.methodInvoke.serviceArg.collapse")
        } else {
            PhpDebugToolsBundle.message("toolwindow.methodInvoke.serviceArg.expand")
        }
        maximumSize = Dimension(Int.MAX_VALUE, preferredSize.height)
        revalidateAncestors()
        if (expanded) {
            scrollIntoView()
        }
    }

    private fun revalidateAncestors() {
        var current: java.awt.Component? = this
        while (current != null) {
            if (current is JComponent) {
                current.revalidate()
                current.repaint()
            }
            current = current.parent
        }
    }

    private fun scrollIntoView() {
        SwingUtilities.invokeLater {
            val viewport = SwingUtilities.getAncestorOfClass(JViewport::class.java, this) as? JViewport ?: return@invokeLater
            val boundsInViewport = SwingUtilities.convertRectangle(parent, bounds, viewport.view)
            viewport.scrollRectToVisible(Rectangle(boundsInViewport.x, boundsInViewport.y, boundsInViewport.width, boundsInViewport.height))
        }
    }
}
