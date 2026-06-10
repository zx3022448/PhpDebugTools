package com.example.phpdebugtools.actions

import com.example.phpdebugtools.methods.MethodKind
import com.example.phpdebugtools.methods.PhpMethodTargetResolver
import com.example.phpdebugtools.ui.ControllerMethodDialog
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys

class DebugControllerMethodAction : AnAction("调试控制器方法") {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(event: AnActionEvent) {
        val psiFile = event.getData(CommonDataKeys.PSI_FILE)
        val editor = event.getData(CommonDataKeys.EDITOR)
        val target = if (psiFile != null && editor != null) {
            PhpMethodTargetResolver.resolve(psiFile, editor.caretModel.offset)
        } else {
            null
        }
        val enabled = target?.kind == MethodKind.CONTROLLER
        event.presentation.isEnabledAndVisible = enabled
    }

    override fun actionPerformed(event: AnActionEvent) {
        val file = event.getData(CommonDataKeys.PSI_FILE) ?: return
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return
        val target = PhpMethodTargetResolver.resolve(file, editor.caretModel.offset) ?: return
        if (target.kind != MethodKind.CONTROLLER) {
            return
        }
        ControllerMethodDialog(event.project ?: return, target).show()
    }
}
