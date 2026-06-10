package com.example.phpdebugtools.runtime

data class RuntimeInstallResult(
    val runtimeRoot: String,
    val installedFiles: List<String>,
    val updated: Boolean,
)
