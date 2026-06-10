package com.example.phpdebugtools.methods

data class MethodDebugTarget(
    val kind: MethodKind,
    val classFqn: String,
    val methodName: String,
    val isStatic: Boolean,
    val parameters: List<MethodParameterSchema>,
)
