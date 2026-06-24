package com.zx3022448.phpdebugtools.methods

data class MethodLookupItem(
    val target: MethodDebugTarget,
) {
    val targetSignature: String = "${target.classFqn}::${target.methodName}"
    val searchableText: String = buildString {
        append(target.classFqn)
        append(' ')
        append(target.methodName)
        append(' ')
        append(target.kind.name)
    }.lowercase()

    override fun toString(): String = targetSignature
}
