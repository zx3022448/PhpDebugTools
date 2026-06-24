package com.zx3022448.phpdebugtools.methods

data class MethodDebugTarget(
    val kind: MethodKind,
    val classFqn: String,
    val methodName: String,
    val isStatic: Boolean,
    val parameters: List<MethodParameterSchema>,
    val controllerRequestSpec: ControllerRequestSpec? = null,
)
