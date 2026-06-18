package com.example.phpdebugtools.execution

data class DebugExecutionResult(
    val status: String,
    val stage: String,
    val message: String,
    val resultText: String = "",
    val resultType: String = "",
    val exceptionText: String = "",
    val rawOutput: String,
)
