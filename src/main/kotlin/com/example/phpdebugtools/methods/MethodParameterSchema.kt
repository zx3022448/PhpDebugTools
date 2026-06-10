package com.example.phpdebugtools.methods

data class MethodParameterSchema(
    val name: String,
    val declaredType: String?,
    val required: Boolean,
    val defaultValue: String?,
)
