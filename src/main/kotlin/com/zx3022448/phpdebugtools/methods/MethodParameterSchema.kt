package com.zx3022448.phpdebugtools.methods

data class MethodParameterSchema(
    val name: String,
    val declaredType: String?,
    val required: Boolean,
    val defaultValue: String?,
)
