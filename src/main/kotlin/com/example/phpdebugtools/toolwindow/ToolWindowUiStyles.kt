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
    private val pageSurface = JBColor(Color(0xF3F4F8), Color(0x12141A))
    private val windowSurface = JBColor(Color(0xFDFDFE), Color(0x191C22))
    private val titleBarSurface = JBColor(Color(0xF5F5F7), Color(0x1E2129))
    private val sidebarSurface = JBColor(Color(0xF6F7FA), Color(0x171A21))
    private val cardSurface = JBColor(Color(0xFFFFFF), Color(0x20242C))
    private val softCardSurface = JBColor(Color(0xFAFAFC), Color(0x262A33))
    private val innerSurface = JBColor(Color(0xFFFFFF), Color(0x1E222A))
    private val inputSurface = JBColor(Color(0xFAFAFC), Color(0x242831))
    private val codeSurface = JBColor(Color(0x0F1720), Color(0x0B0E14))
    private val codeToolbarSurface = JBColor(Color(0x121A23), Color(0x131A24))
    private val borderColor = JBColor(Color(0xD9DCE3), Color(0x323744))
    private val subtleBorderColor = JBColor(Color(0xE9EAF0), Color(0x2A2F3A))
    private val chipSurface = JBColor(Color(0xEEF0F3), Color(0x2B303A))
    private val hoverSurface = JBColor(Color(0xF0F2F6), Color(0x2E333E))
    private val selectedSurface = JBColor(Color(0xE8F2FF), Color(0x17304C))
    private val primarySurface = JBColor(Color(0x0A84FF), Color(0x2997FF))
    private val primaryHoverSurface = JBColor(Color(0x3F9BFF), Color(0x4AA3FF))
    private val primaryPressedSurface = JBColor(Color(0x0066CC), Color(0x0A84FF))
    private val mutedTextColor = JBColor(Color(0x6E6E73), Color(0xA1A1A6))
    private val faintTextColor = JBColor(Color(0x86868B), Color(0x73737A))
    private val idleForeground = JBColor(Color(0x4C4C51), Color(0x9AA3AF))
    private val successColor = JBColor(Color(0x30D158), Color(0x32D74B))
    private val warningColor = JBColor(Color(0xFFCC00), Color(0xFFD60A))
    private val errorColor = JBColor(Color(0xFF453A), Color(0xFF6961))
    private val titleColor = JBColor(Color(0x1D1D1F), Color(0xF5F5F7))
    private val codeTextColor = JBColor(Color(0xD8E0EA), Color(0xD8E0EA))
    private val codeAccentTextColor = JBColor(Color(0xD7F7BD), Color(0xD7F7BD))

    fun applyWorkbenchSurface(component: JComponent) {
        component.isOpaque = true
        component.background = pageSurface
        component.border = JBUI.Borders.empty(14)
    }

    fun applyWindowShell(component: JComponent) {
        component.isOpaque = true
        component.background = windowSurface
        component.border = JBUI.Borders.compound(
            BorderFactory.createLineBorder(subtleBorderColor, 1, true),
            JBUI.Borders.empty(0),
        )
    }

    fun applyShellSurface(component: JComponent) {
        component.isOpaque = true
        component.background = windowSurface
        component.border = JBUI.Borders.empty(0)
    }

    fun applyTitleBar(component: JComponent) {
        component.isOpaque = true
        component.background = titleBarSurface
        component.border = BorderFactory.createMatteBorder(0, 0, 1, 0, subtleBorderColor)
    }

    fun applySidebarSurface(component: JComponent) {
        component.isOpaque = true
        component.background = sidebarSurface
        component.border = BorderFactory.createMatteBorder(0, 0, 1, 0, subtleBorderColor)
    }

    fun applyCard(component: JComponent) {
        applyGlassCard(component)
    }

    fun applyGlassCard(component: JComponent) {
        component.isOpaque = true
        component.background = cardSurface
        component.border = JBUI.Borders.compound(
            BorderFactory.createLineBorder(subtleBorderColor, 1, true),
            JBUI.Borders.empty(16),
        )
    }

    fun applySoftCard(component: JComponent) {
        component.isOpaque = true
        component.background = softCardSurface
        component.border = JBUI.Borders.compound(
            BorderFactory.createLineBorder(subtleBorderColor, 1, true),
            JBUI.Borders.empty(12),
        )
    }

    fun applyToolbarCard(component: JComponent) {
        applySoftCard(component)
    }

    fun applyInnerSurface(component: JComponent) {
        component.isOpaque = true
        component.background = innerSurface
        component.border = JBUI.Borders.compound(
            BorderFactory.createLineBorder(subtleBorderColor, 1, true),
            JBUI.Borders.empty(10),
        )
    }

    fun applyCodeSurface(component: JComponent) {
        component.isOpaque = true
        component.background = codeSurface
        component.border = JBUI.Borders.compound(
            BorderFactory.createLineBorder(Color(0x202531), 1, true),
            JBUI.Borders.empty(0),
        )
    }

    fun applyCodeToolbar(component: JComponent) {
        component.isOpaque = true
        component.background = codeToolbarSurface
        component.border = BorderFactory.createMatteBorder(0, 0, 1, 0, Color(0x25303A))
    }

    fun applyMetricCard(component: JComponent) {
        component.isOpaque = true
        component.background = softCardSurface
        component.border = JBUI.Borders.compound(
            BorderFactory.createLineBorder(subtleBorderColor, 1, true),
            JBUI.Borders.empty(12),
        )
    }

    fun applyStatusCard(component: JComponent) {
        component.isOpaque = true
        component.background = cardSurface
        component.border = JBUI.Borders.compound(
            BorderFactory.createLineBorder(subtleBorderColor, 1, true),
            JBUI.Borders.empty(16),
        )
    }

    fun applyPrimaryButton(button: AbstractButton) {
        button.background = primarySurface
        button.foreground = JBColor(Color.WHITE, Color.WHITE)
        button.border = JBUI.Borders.compound(
            BorderFactory.createLineBorder(primaryPressedSurface, 1, true),
            JBUI.Borders.empty(8, 16),
        )
        button.isFocusPainted = false
        button.isOpaque = true
        installHoverTransition(button, primarySurface, primaryHoverSurface)
    }

    fun applySecondaryButton(button: AbstractButton) {
        button.background = inputSurface
        button.foreground = titleColor
        button.border = JBUI.Borders.compound(
            BorderFactory.createLineBorder(borderColor, 1, true),
            JBUI.Borders.empty(7, 12),
        )
        button.isFocusPainted = false
        button.isOpaque = true
        installHoverTransition(button, inputSurface, hoverSurface)
    }

    fun applyIconButton(button: AbstractButton, primary: Boolean = false) {
        applyRoundIconButton(button, primary)
    }

    fun applyRoundIconButton(button: AbstractButton, primary: Boolean = false) {
        button.horizontalAlignment = SwingConstants.CENTER
        button.border = BorderFactory.createLineBorder(if (primary) primaryPressedSurface else borderColor, 1, true)
        button.background = if (primary) primarySurface else inputSurface
        button.foreground = if (primary) JBColor(Color.WHITE, Color.WHITE) else mutedTextColor
        button.isContentAreaFilled = true
        button.isBorderPainted = true
        button.isFocusPainted = false
        button.isOpaque = true
        installHoverTransition(button, button.background, if (primary) primaryHoverSurface else hoverSurface)
    }

    fun applyCapsuleButton(button: AbstractButton, selected: Boolean = false) {
        button.border = JBUI.Borders.compound(
            BorderFactory.createLineBorder(if (selected) selectedSurface else subtleBorderColor, 1, true),
            JBUI.Borders.empty(6, 12),
        )
        button.background = if (selected) selectedSurface else chipSurface
        button.foreground = if (selected) primaryPressedSurface else mutedTextColor
        button.isOpaque = true
        button.isFocusPainted = false
    }

    fun applyInputSurface(component: JComponent) {
        component.isOpaque = true
        component.background = inputSurface
        component.border = JBUI.Borders.compound(
            BorderFactory.createLineBorder(borderColor, 1, true),
            JBUI.Borders.empty(6, 10),
        )
        component.font = interFont(component.font)
        component.foreground = titleColor
    }

    fun applyResultArea(area: JTextArea) {
        area.background = inputSurface
        area.border = JBUI.Borders.empty(8)
        area.foreground = titleColor
        area.caretColor = titleColor
        area.font = area.font.deriveFont(Font.PLAIN, JBUI.scaleFontSize(12f).toFloat())
    }

    fun applyCodeArea(area: JTextArea, accent: Boolean = false) {
        area.background = codeSurface
        area.border = JBUI.Borders.empty(16)
        area.foreground = if (accent) codeAccentTextColor else codeTextColor
        area.caretColor = codeTextColor
        area.font = area.font.deriveFont(Font.PLAIN, JBUI.scaleFontSize(12f).toFloat())
    }

    fun applyTitleLabel(label: JLabel, sizeDelta: Float = 0f) {
        label.font = interFont(label.font).deriveFont(Font.BOLD, label.font.size2D + sizeDelta)
        label.foreground = titleColor
    }

    fun applySectionEyebrow(label: JLabel) {
        label.font = interFont(label.font).deriveFont(Font.BOLD, (label.font.size2D - 1f).coerceAtLeast(10f))
        label.foreground = mutedTextColor
    }

    fun applyTabLabel(label: JLabel, selected: Boolean) {
        label.font = interFont(label.font).deriveFont(if (selected) Font.BOLD else Font.PLAIN)
    }

    fun applyTabularLabel(label: JLabel) {
        label.font = label.font.deriveFont(label.font.style)
    }

    fun applyStatusStrip(component: JComponent, status: MethodInvokeVisualStatus) {
        component.isOpaque = true
        component.background = cardSurface
        component.border = JBUI.Borders.compound(
            BorderFactory.createMatteBorder(0, 4, 0, 0, statusColor(status)),
            JBUI.Borders.empty(0, 12),
        )
    }

    fun applyScrollPane(scrollPane: JScrollPane) {
        scrollPane.border = JBUI.Borders.empty()
        scrollPane.isOpaque = false
        scrollPane.viewport.isOpaque = true
        if (scrollPane.viewport.background == null || scrollPane.viewport.background.alpha == 0) {
            scrollPane.viewport.background = scrollPane.background
        }
        if (SystemInfoRt.isMac) {
            scrollPane.putClientProperty("JScrollPane.style", "overlay")
        }
    }

    fun applyMutedLabel(label: JLabel) {
        label.font = interFont(label.font)
        label.foreground = mutedTextColor
    }

    fun applyFaintLabel(label: JLabel) {
        label.font = interFont(label.font)
        label.foreground = faintTextColor
    }

    fun applyStatusBadge(label: JLabel, status: MethodInvokeVisualStatus) {
        label.isOpaque = true
        label.horizontalAlignment = SwingConstants.CENTER
        label.border = JBUI.Borders.compound(
            BorderFactory.createLineBorder(subtleBorderColor, 1, true),
            JBUI.Borders.empty(4, 10),
        )
        applyTabularLabel(label)
        when (status) {
            MethodInvokeVisualStatus.IDLE -> {
                label.background = chipSurface
                label.foreground = idleForeground
            }

            MethodInvokeVisualStatus.RUNNING -> {
                label.background = JBColor(Color(0xFFF8D6), Color(0x3B3314))
                label.foreground = warningColor
            }

            MethodInvokeVisualStatus.SUCCESS -> {
                label.background = JBColor(Color(0xE6F8ED), Color(0x173323))
                label.foreground = successColor
            }

            MethodInvokeVisualStatus.ERROR -> {
                label.background = JBColor(Color(0xFFE9E7), Color(0x3A1F1D))
                label.foreground = errorColor
            }
        }
    }

    fun tabBorder(selected: Boolean): Border =
        BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(if (selected) selectedSurface else subtleBorderColor, 1, true),
            BorderFactory.createEmptyBorder(7, 14, 7, 14),
        )

    fun selectedTabBackground(): Color = selectedSurface

    fun activeBlue(): Color = primaryPressedSurface

    fun idleTextColor(): Color = idleForeground

    fun applyTable(table: JTable) {
        table.background = innerSurface
        table.foreground = titleColor
        table.font = interFont(table.font)
        table.selectionBackground = selectedSurface
        table.selectionForeground = titleColor
        table.gridColor = subtleBorderColor
        table.setShowGrid(true)
        table.showVerticalLines = false
        table.intercellSpacing = java.awt.Dimension(0, 1)
        table.tableHeader.font = interFont(table.tableHeader.font).deriveFont(Font.BOLD)
        table.tableHeader.background = softCardSurface
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
