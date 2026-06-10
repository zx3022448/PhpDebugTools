package com.example.phpdebugtools.actions

import com.example.phpdebugtools.methods.MethodDebugTarget
import com.intellij.openapi.project.Project

fun interface MethodDebugDialogLauncher {
    fun show(project: Project, target: MethodDebugTarget)
}
