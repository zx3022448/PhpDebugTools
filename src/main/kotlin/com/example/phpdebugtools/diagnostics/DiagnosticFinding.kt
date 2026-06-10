package com.example.phpdebugtools.diagnostics

data class DiagnosticFinding(
    val stage: DiagnosticStage,
    val severity: String,
    val message: String,
    val hint: String,
)
