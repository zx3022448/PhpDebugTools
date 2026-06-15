package com.example.phpdebugtools.toolwindow

data class ToolWindowWorkspaceState(
    val overview: OverviewViewState,
    val methodInvoke: ToolWindowDetailState,
)

data class ToolWindowDetailState(
    val summary: String,
    val details: List<String>,
)
