package com.example.phpdebugtools.actions

import com.example.phpdebugtools.methods.MethodKind
import com.example.phpdebugtools.methods.PhpMethodTargetResolver
import com.example.phpdebugtools.ui.ServiceMethodDialog
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys

class DebugServiceMethodAction(
    private val dialogLauncher: MethodDebugDialogLauncher = MethodDebugDialogLauncher { project, target ->
        ServiceMethodDialog(project, target).show()
    },
) : AnAction("调试服务方法") {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(event: AnActionEvent) {
        val psiFile = event.getData(CommonDataKeys.PSI_FILE)
        val editor = event.getData(CommonDataKeys.EDITOR)
        val target = if (psiFile != null && editor != null) {
            PhpMethodTargetResolver.resolve(psiFile, editor.caretModel.offset)
        } else {
            null
        }
        val enabled = target?.kind == MethodKind.SERVICE
        event.presentation.isEnabledAndVisible = enabled
    }

    override fun actionPerformed(event: AnActionEvent) {
        val psiFile = event.getData(CommonDataKeys.PSI_FILE) ?: return
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return
        val target = PhpMethodTargetResolver.resolve(psiFile, editor.caretModel.offset) ?: return
        if (target.kind != MethodKind.SERVICE) {
            return
        }

        dialogLauncher.show(event.project ?: return, target)
    }
}
