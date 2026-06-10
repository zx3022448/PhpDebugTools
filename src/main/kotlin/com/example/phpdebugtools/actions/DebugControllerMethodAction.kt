package com.example.phpdebugtools.actions

import com.example.phpdebugtools.methods.MethodKind
import com.example.phpdebugtools.methods.PhpMethodTargetResolver
import com.example.phpdebugtools.ui.ControllerMethodDialog
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys

class DebugControllerMethodAction : AnAction("调试控制器方法") {
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
