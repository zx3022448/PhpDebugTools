package com.example.phpdebugtools.toolwindow

import com.intellij.openapi.util.SystemInfoRt
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import java.awt.Color
import javax.swing.AbstractButton
import javax.swing.BorderFactory
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JScrollPane
import javax.swing.JTable
import javax.swing.JTextArea
import javax.swing.SwingConstants
import javax.swing.border.Border

internal object ToolWindowUiStyles {
    private val panelSurface = JBColor(Color(0xF6F7FB), Color(0x1F2227))
    private val shellSurface = JBColor(Color(0xFFFFFF), Color(0x20242A))
    private val cardSurface = JBColor(Color(0xFFFFFF), Color(0x22262C))
    private val toolbarSurface = JBColor(Color(0xFFFFFF), Color(0x1D2126))
    private val innerSurface = JBColor(Color(0xF4F6FA), Color(0x262A31))
    private val inputSurface = JBColor(Color(0xFFFFFF), Color(0x1F2329))
    private val borderColor = JBColor(Color(0xD7DEE9), Color(0x3A4049))
    private val subtleBorderColor = JBColor(Color(0xE6EAF0), Color(0x2F343C))
    private val mutedTextColor = JBColor(Color(0x667085), Color(0x9AA4B2))
    private val successColor = JBColor(Color(0x0F9D58), Color(0x55D88A))
    private val warningColor = JBColor(Color(0xC47F00), Color(0xF7B955))
    private val errorColor = JBColor(Color(0xD93025), Color(0xFF7B72))
    private val idleForeground = JBColor(Color(0x596579), Color(0xC8CDD8))

    fun applyWorkbenchSurface(component: JComponent) {
        component.isOpaque = true
        component.background = panelSurface
        component.border = JBUI.Borders.empty(12)
    }

    fun applyShellSurface(component: JComponent) {
        component.isOpaque = true
        component.background = shellSurface
        component.border = JBUI.Borders.empty(0)
    }

    fun applyCard(component: JComponent) {
        component.isOpaque = true
        component.background = cardSurface
        component.border = JBUI.Borders.compound(
            BorderFactory.createLineBorder(borderColor, 1, true),
            JBUI.Borders.empty(14),
        )
    }

    fun applyToolbarCard(component: JComponent) {
        component.isOpaque = true
        component.background = toolbarSurface
        component.border = JBUI.Borders.compound(
            BorderFactory.createLineBorder(borderColor, 1, true),
            JBUI.Borders.empty(12, 12, 10, 12),
        )
    }

    fun applyInnerSurface(component: JComponent) {
        component.isOpaque = true
        component.background = innerSurface
        component.border = JBUI.Borders.compound(
            BorderFactory.createLineBorder(subtleBorderColor, 1, true),
            JBUI.Borders.empty(8),
        )
    }

    fun applyPrimaryButton(button: AbstractButton) {
        button.background = JBColor.namedColor("Button.default.startBackground", warningColor)
        button.foreground = JBColor.namedColor("Button.default.foreground", JBColor.WHITE)
        button.border = JBUI.Borders.empty(7, 12)
        button.isFocusPainted = false
        button.isOpaque = true
    }

    fun applySecondaryButton(button: AbstractButton) {
        button.background = inputSurface
        button.foreground = JBColor.foreground()
        button.border = JBUI.Borders.compound(
            BorderFactory.createLineBorder(borderColor, 1, true),
            JBUI.Borders.empty(6, 10),
        )
        button.isFocusPainted = false
        button.isOpaque = true
    }

    fun applyIconButton(button: AbstractButton, primary: Boolean = false) {
        if (primary) {
            applyPrimaryButton(button)
        } else {
            applySecondaryButton(button)
        }
        button.horizontalAlignment = SwingConstants.CENTER
        button.border = JBUI.Borders.empty(0)
        button.background = if (primary) {
            JBColor(Color(0x3A7BFF), Color(0x3F76F6))
        } else {
            JBColor(Color(0xF3F5F9), Color(0x262A31))
        }
        button.foreground = JBColor(Color(0xFFFFFF), Color(0xDCE3F1))
    }

    fun applyInputSurface(component: JComponent) {
        component.isOpaque = true
        component.background = inputSurface
        component.border = JBUI.Borders.compound(
            BorderFactory.createLineBorder(borderColor, 1, true),
            JBUI.Borders.empty(6, 10),
        )
    }

    fun applyResultArea(area: JTextArea) {
        area.background = inputSurface
        area.border = JBUI.Borders.empty(8)
        area.foreground = JBColor.foreground()
        area.caretColor = JBColor.foreground()
    }

    fun applyScrollPane(scrollPane: JScrollPane) {
        scrollPane.border = JBUI.Borders.empty()
        scrollPane.isOpaque = false
        scrollPane.viewport.background = scrollPane.background
        if (SystemInfoRt.isMac) {
            scrollPane.putClientProperty("JScrollPane.style", "overlay")
        }
    }

    fun applyMutedLabel(label: JLabel) {
        label.foreground = mutedTextColor
    }

    fun applyStatusBadge(label: JLabel, status: MethodInvokeVisualStatus) {
        label.isOpaque = true
        label.horizontalAlignment = SwingConstants.CENTER
        label.border = JBUI.Borders.empty(4, 10)
        when (status) {
            MethodInvokeVisualStatus.IDLE -> {
                label.background = JBColor(Color(0xEEF1F6), Color(0x343944))
                label.foreground = idleForeground
            }

            MethodInvokeVisualStatus.RUNNING -> {
                label.background = JBColor(Color(0xFFF4DD), Color(0x44351B))
                label.foreground = warningColor
            }

            MethodInvokeVisualStatus.SUCCESS -> {
                label.background = JBColor(Color(0xE7F7EE), Color(0x1E4030))
                label.foreground = successColor
            }

            MethodInvokeVisualStatus.ERROR -> {
                label.background = JBColor(Color(0xFCEBEA), Color(0x4A2625))
                label.foreground = errorColor
            }
        }
    }

    fun tabBorder(selected: Boolean): Border =
        if (selected) {
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borderColor, 1, true),
                BorderFactory.createEmptyBorder(6, 14, 6, 14),
            )
        } else {
            BorderFactory.createEmptyBorder(7, 15, 7, 15)
        }

    fun selectedTabBackground(): Color = JBColor(Color(0xEEF3FF), Color(0x2B3038))

    fun activeBlue(): Color = JBColor(Color(0x3A7BFF), Color(0x4F86FF))

    fun warningRed(): Color = JBColor(Color(0xEB5757), Color(0xFF6B6B))

    fun applyTable(table: JTable) {
        table.background = inputSurface
        table.foreground = JBColor.foreground()
        table.selectionBackground = JBColor(Color(0xE8F0FE), Color(0x32486F))
        table.selectionForeground = JBColor.foreground()
        table.gridColor = subtleBorderColor
        table.setShowGrid(false)
        table.intercellSpacing = java.awt.Dimension(0, 0)
        table.tableHeader.background = toolbarSurface
        table.tableHeader.foreground = mutedTextColor
        table.tableHeader.border = BorderFactory.createMatteBorder(0, 0, 1, 0, subtleBorderColor)
        table.tableHeader.reorderingAllowed = false
    }

    fun statusColor(status: MethodInvokeVisualStatus): Color = when (status) {
        MethodInvokeVisualStatus.IDLE -> idleForeground
        MethodInvokeVisualStatus.RUNNING -> warningColor
        MethodInvokeVisualStatus.SUCCESS -> successColor
        MethodInvokeVisualStatus.ERROR -> errorColor
    }
}

internal enum class MethodInvokeVisualStatus {
    IDLE,
    RUNNING,
    SUCCESS,
    ERROR,
}
