package com.example.phpdebugtools.toolwindow

import com.intellij.openapi.util.SystemInfoRt
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import java.awt.Color
import java.awt.Font
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.AbstractButton
import javax.swing.BorderFactory
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JScrollPane
import javax.swing.JTable
import javax.swing.JTextArea
import javax.swing.SwingConstants
import javax.swing.Timer
import javax.swing.border.Border

internal object ToolWindowUiStyles {
    private val panelSurface = JBColor(Color(0xFAFAFA), Color(0x15161A))
    private val shellSurface = JBColor(Color(0xFFFFFF), Color(0x1B1D22))
    private val cardSurface = JBColor(Color(0xFFFFFF), Color(0x202329))
    private val toolbarSurface = JBColor(Color(0xFFFFFF), Color(0x20232A))
    private val innerSurface = JBColor(Color(0xFFFFFF), Color(0x181A1F))
    private val inputSurface = JBColor(Color(0xFFFFFF), Color(0x23262D))
    private val borderColor = JBColor(Color(0xE5E5E5), Color(0x343841))
    private val subtleBorderColor = JBColor(Color(0xEEEEEE), Color(0x2B2F37))
    private val hoverSurface = JBColor(Color(0xF5F5F5), Color(0x2B2F38))
    private val pressedSurface = JBColor(Color(0xEDEDED), Color(0x363B45))
    private val selectedSurface = JBColor(Color(0xFFF4ED), Color(0x34251E))
    private val primarySurface = JBColor(Color(0xFF6A2A), Color(0xFF7A3D))
    private val primaryHoverSurface = JBColor(Color(0xF05A1A), Color(0xFF8B52))
    private val primaryPressedSurface = JBColor(Color(0xD94D12), Color(0xE86427))
    private val mutedTextColor = JBColor(Color(0x737373), Color(0xAEB4BF))
    private val successColor = JBColor(Color(0x16A34A), Color(0x5BD38A))
    private val warningColor = JBColor(Color(0xD97706), Color(0xFFB86B))
    private val errorColor = JBColor(Color(0xDC2626), Color(0xFF6B6B))
    private val idleForeground = JBColor(Color(0x525252), Color(0x9AA3AF))
    private val accentColor = JBColor(Color(0xFF6A2A), Color(0xFF8A4C))

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
            BorderFactory.createLineBorder(borderColor, 1),
            JBUI.Borders.empty(16),
        )
    }

    fun applyToolbarCard(component: JComponent) {
        component.isOpaque = true
        component.background = toolbarSurface
        component.border = JBUI.Borders.compound(
            BorderFactory.createLineBorder(borderColor, 1),
            JBUI.Borders.empty(9, 12, 9, 12),
        )
    }

    fun applyInnerSurface(component: JComponent) {
        component.isOpaque = true
        component.background = innerSurface
        component.border = JBUI.Borders.compound(
            BorderFactory.createLineBorder(subtleBorderColor, 1),
            JBUI.Borders.empty(8),
        )
    }

    fun applyPrimaryButton(button: AbstractButton) {
        button.background = primarySurface
        button.foreground = JBColor(Color.WHITE, Color.WHITE)
        button.border = JBUI.Borders.compound(
            BorderFactory.createLineBorder(primaryPressedSurface, 1),
            JBUI.Borders.empty(7, 12),
        )
        button.isFocusPainted = false
        button.isOpaque = true
        installHoverTransition(button, primarySurface, primaryHoverSurface)
    }

    fun applySecondaryButton(button: AbstractButton) {
        button.background = inputSurface
        button.foreground = JBColor.foreground()
        button.border = JBUI.Borders.compound(
            BorderFactory.createLineBorder(borderColor, 1),
            JBUI.Borders.empty(6, 10),
        )
        button.isFocusPainted = false
        button.isOpaque = true
        installHoverTransition(button, inputSurface, hoverSurface)
    }

    fun applyIconButton(button: AbstractButton, primary: Boolean = false) {
        button.horizontalAlignment = SwingConstants.CENTER
        button.border = BorderFactory.createLineBorder(if (primary) primaryPressedSurface else borderColor, 1)
        button.background = if (primary) primarySurface else inputSurface
        button.foreground = if (primary) JBColor(Color.WHITE, Color.WHITE) else JBColor.foreground()
        button.isContentAreaFilled = true
        button.isBorderPainted = true
        button.isFocusPainted = false
        button.isOpaque = true
        installHoverTransition(button, button.background, if (primary) primaryHoverSurface else hoverSurface)
    }


    fun applyInputSurface(component: JComponent) {
        component.isOpaque = true
        component.background = inputSurface
        component.border = JBUI.Borders.compound(
            BorderFactory.createLineBorder(borderColor, 1),
            JBUI.Borders.empty(6, 10),
        )
        component.font = interFont(component.font)
    }


    fun applyResultArea(area: JTextArea) {
        area.background = inputSurface
        area.border = JBUI.Borders.empty(8)
        area.foreground = JBColor.foreground()
        area.caretColor = JBColor.foreground()
        // 使用 IDE 默认字体，确保中文文案和路径在不同系统字体环境下都能正常回退显示
        area.font = area.font.deriveFont(Font.PLAIN, JBUI.scaleFontSize(12f).toFloat())
    }

    fun applyTitleLabel(label: JLabel, sizeDelta: Float = 0f) {
        label.font = interFont(label.font).deriveFont(Font.BOLD, label.font.size2D + sizeDelta)
    }

    fun applyTabLabel(label: JLabel, selected: Boolean) {
        label.font = interFont(label.font).deriveFont(if (selected) Font.BOLD else Font.PLAIN)
    }

    fun applyTabularLabel(label: JLabel) {
        label.font = label.font.deriveFont(label.font.style)
    }

    /** 根据当前状态为状态条应用左侧 accent 色条 */
    fun applyStatusStrip(component: JComponent, status: MethodInvokeVisualStatus) {
        component.isOpaque = true
        component.background = shellSurface
        component.border = JBUI.Borders.compound(
            BorderFactory.createMatteBorder(0, 3, 0, 0, statusColor(status)),
            JBUI.Borders.empty(0, 10),
        )
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
        label.font = interFont(label.font)
        label.foreground = mutedTextColor
    }

    fun applyStatusBadge(label: JLabel, status: MethodInvokeVisualStatus) {
        label.isOpaque = true
        label.horizontalAlignment = SwingConstants.CENTER
        label.border = JBUI.Borders.compound(
            BorderFactory.createLineBorder(subtleBorderColor, 1),
            JBUI.Borders.empty(3, 9),
        )
        applyTabularLabel(label)
        when (status) {
            MethodInvokeVisualStatus.IDLE -> {
                label.background = JBColor(Color(0xF5F5F5), Color(0x262A31))
                label.foreground = idleForeground
            }

            MethodInvokeVisualStatus.RUNNING -> {
                label.background = JBColor(Color(0xFFFBEB), Color(0x34251E))
                label.foreground = warningColor
            }

            MethodInvokeVisualStatus.SUCCESS -> {
                label.background = JBColor(Color(0xF0FDF4), Color(0x173323))
                label.foreground = successColor
            }

            MethodInvokeVisualStatus.ERROR -> {
                label.background = JBColor(Color(0xFEF2F2), Color(0x371D1D))
                label.foreground = errorColor
            }
        }
    }

    fun tabBorder(selected: Boolean): Border =
        if (selected) {
            BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 2, 0, activeBlue()),
                BorderFactory.createEmptyBorder(8, 14, 7, 14),
            )
        } else {
            BorderFactory.createEmptyBorder(8, 14, 9, 14)
        }

    fun selectedTabBackground(): Color = selectedSurface

    fun activeBlue(): Color = accentColor

    fun idleTextColor(): Color = idleForeground

    fun applyTable(table: JTable) {
        table.background = inputSurface
        table.foreground = JBColor.foreground()
        table.font = interFont(table.font)
        table.selectionBackground = JBColor(Color(0xFFF4ED), Color(0x33271F))
        table.selectionForeground = JBColor.foreground()
        table.gridColor = subtleBorderColor
        table.setShowGrid(true)
        table.showVerticalLines = false
        table.intercellSpacing = java.awt.Dimension(0, 1)
        table.tableHeader.font = interFont(table.tableHeader.font).deriveFont(Font.BOLD)
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

    private fun interFont(font: Font): Font = font

    private fun installHoverTransition(button: AbstractButton, normal: Color, hover: Color) {
        var animationTimer: Timer? = null
        var progress = 0f
        var target = 0f

        fun step() {
            val delta = if (target > progress) 0.2f else -0.2f
            progress = (progress + delta).coerceIn(0f, 1f)
            button.background = blend(normal, hover, progress)
            if (progress == target) {
                animationTimer?.stop()
            }
        }

        animationTimer = Timer(30) { step() }
        button.addMouseListener(
            object : MouseAdapter() {
                override fun mouseEntered(event: MouseEvent) {
                    target = 1f
                    animationTimer?.restart()
                }

                override fun mouseExited(event: MouseEvent) {
                    target = 0f
                    animationTimer?.restart()
                }
            },
        )
    }

    private fun blend(from: Color, to: Color, amount: Float): Color = Color(
        (from.red + (to.red - from.red) * amount).toInt().coerceIn(0, 255),
        (from.green + (to.green - from.green) * amount).toInt().coerceIn(0, 255),
        (from.blue + (to.blue - from.blue) * amount).toInt().coerceIn(0, 255),
    )
}

internal enum class MethodInvokeVisualStatus {
    IDLE,
    RUNNING,
    SUCCESS,
    ERROR,
}
