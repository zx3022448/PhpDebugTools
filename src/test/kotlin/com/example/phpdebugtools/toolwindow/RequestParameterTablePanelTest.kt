package com.example.phpdebugtools.toolwindow

import org.junit.Assert.assertEquals
import org.junit.Test

class RequestParameterTablePanelTest {
    @Test
    fun `参数表格包含两个工具按钮并保留一行默认数据`() {
        val panel = RequestParameterTablePanel()

        assertEquals(2, panel.toolbarButtonCount())
        assertEquals(1, panel.rows().size)
    }
}
