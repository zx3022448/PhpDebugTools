package com.example.phpdebugtools.toolwindow

import org.junit.Assert.assertEquals
import org.junit.Test

class OverviewViewStateTest {

    @Test
    fun formatsDetectedProjectSummary() {
        val state = OverviewViewState(
            projectSummary = "ThinkPHP 6",
            runtimeSummary = ".php-debug-tools installed",
            diagnosticsSummary = "1 warning",
        )

        assertEquals("ThinkPHP 6", state.projectSummary)
    }
}
