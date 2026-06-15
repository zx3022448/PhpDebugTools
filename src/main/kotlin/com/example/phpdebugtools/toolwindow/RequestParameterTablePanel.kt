package com.example.phpdebugtools.toolwindow

import com.example.phpdebugtools.PhpDebugToolsBundle
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import java.awt.BorderLayout
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.table.AbstractTableModel

internal class RequestParameterTablePanel : JBPanel<JBPanel<*>>(BorderLayout()) {
    private val tableModel = RequestParameterTableModel()
    private val table = JBTable(tableModel)

    init {
        add(
            JPanel().apply {
                add(JButton(PhpDebugToolsBundle.message("toolwindow.methodInvoke.paramTable.add")).apply {
                    addActionListener { tableModel.addRow(RequestParameterDraft()) }
                })
                add(JButton(PhpDebugToolsBundle.message("toolwindow.methodInvoke.paramTable.remove")).apply {
                    addActionListener {
                        val selectedRow = table.selectedRow
                        if (selectedRow >= 0) {
                            tableModel.removeRow(selectedRow)
                        }
                    }
                })
            },
            BorderLayout.NORTH,
        )
        add(JBScrollPane(table), BorderLayout.CENTER)
        table.fillsViewportHeight = true
    }

    fun setRows(rows: List<RequestParameterDraft>) {
        tableModel.setRows(rows)
    }

    fun rows(): List<RequestParameterDraft> = tableModel.rows()
}

private class RequestParameterTableModel : AbstractTableModel() {
    private val columns = arrayOf(
        PhpDebugToolsBundle.message("toolwindow.methodInvoke.paramTable.column.name"),
        PhpDebugToolsBundle.message("toolwindow.methodInvoke.paramTable.column.type"),
        PhpDebugToolsBundle.message("toolwindow.methodInvoke.paramTable.column.example"),
        PhpDebugToolsBundle.message("toolwindow.methodInvoke.paramTable.column.description"),
    )
    private val rows = mutableListOf<RequestParameterDraft>()

    override fun getRowCount(): Int = rows.size

    override fun getColumnCount(): Int = columns.size

    override fun getColumnName(column: Int): String = columns[column]

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
        val row = rows[rowIndex]
        return when (columnIndex) {
            0 -> row.name
            1 -> row.type
            2 -> row.example
            else -> row.description
        }
    }

    override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean = true

    override fun setValueAt(aValue: Any?, rowIndex: Int, columnIndex: Int) {
        val row = rows[rowIndex]
        rows[rowIndex] = when (columnIndex) {
            0 -> row.copy(name = aValue?.toString().orEmpty())
            1 -> row.copy(type = aValue?.toString().orEmpty().ifBlank { "string" })
            2 -> row.copy(example = aValue?.toString().orEmpty())
            else -> row.copy(description = aValue?.toString().orEmpty())
        }
        fireTableCellUpdated(rowIndex, columnIndex)
    }

    fun setRows(items: List<RequestParameterDraft>) {
        rows.clear()
        rows += items.ifEmpty { listOf(RequestParameterDraft()) }
        fireTableDataChanged()
    }

    fun addRow(item: RequestParameterDraft) {
        rows += item
        fireTableRowsInserted(rows.lastIndex, rows.lastIndex)
    }

    fun removeRow(index: Int) {
        rows.removeAt(index)
        if (rows.isEmpty()) {
            rows += RequestParameterDraft()
        }
        fireTableDataChanged()
    }

    fun rows(): List<RequestParameterDraft> = rows.toList()
}
