package com.zx3022448.phpdebugtools.actions

import com.zx3022448.phpdebugtools.methods.MethodDebugTarget
import com.intellij.openapi.project.Project

fun interface MethodDebugDialogLauncher {
    fun show(project: Project, target: MethodDebugTarget)
}
