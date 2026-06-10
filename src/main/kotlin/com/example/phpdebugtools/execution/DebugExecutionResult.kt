package com.example.phpdebugtools.execution

data class DebugExecutionResult(
    val status: String,
    val stage: String,
    val message: String,
    val rawOutput: String,
)
