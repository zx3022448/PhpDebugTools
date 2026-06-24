package com.zx3022448.phpdebugtools.runtime

data class RuntimeInstallResult(
    val runtimeRoot: String,
    val installedFiles: List<String>,
    val updated: Boolean,
)
