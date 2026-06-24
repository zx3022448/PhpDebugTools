package com.zx3022448.phpdebugtools.execution

/**
 * 表示一个可用于方法直调的 PHP 解释器候选项。
 */
data class DetectedPhpRuntime(
    val command: String,
    val version: String,
    val source: String,
) {
    val displayName: String
        get() = buildString {
            if (version.isNotBlank()) {
                append("PHP ")
                append(version)
            } else {
                append("PHP")
            }
            if (source.isNotBlank()) {
                append(" [")
                append(source)
                append(']')
            }
            append(" - ")
            append(command)
        }
}
