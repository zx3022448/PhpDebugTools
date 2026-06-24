package com.zx3022448.phpdebugtools.project

data class ThinkPhpProjectInfo(
    val isThinkPhp: Boolean,
    val majorVersion: String? = null,
    val detectionSource: String = "none",
    val entryFile: String? = null,
    val confidence: Int = 0,
)
