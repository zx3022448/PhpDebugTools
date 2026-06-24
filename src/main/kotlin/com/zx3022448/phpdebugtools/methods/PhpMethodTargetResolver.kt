package com.zx3022448.phpdebugtools.methods

import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.lang.psi.elements.Method
import com.jetbrains.php.lang.psi.elements.Parameter

object PhpMethodTargetResolver {
    fun resolve(file: PsiFile, caretOffset: Int): MethodDebugTarget? {
        val element = file.findElementAt(caretOffset) ?: return null
        val method = PsiTreeUtil.getParentOfType(element, Method::class.java) ?: return null
        val containingClass = method.containingClass ?: return null
        val classFqn = containingClass.fqn

        return MethodDebugTarget(
            kind = resolveKind(classFqn, containingClass.name),
            classFqn = classFqn,
            methodName = method.name,
            isStatic = method.modifier.isStatic,
            parameters = method.parameters.map(::toSchema),
        )
    }

    private fun resolveKind(classFqn: String, className: String?): MethodKind {
        return if (classFqn.contains("\\controller\\", ignoreCase = true) || className?.endsWith("Controller") == true) {
            MethodKind.CONTROLLER
        } else {
            MethodKind.SERVICE
        }
    }

    private fun toSchema(parameter: Parameter): MethodParameterSchema {
        return MethodParameterSchema(
            name = parameter.name,
            declaredType = parameter.type.toString(),
            required = !parameter.isOptional,
            defaultValue = parameter.defaultValue?.text,
        )
    }
}
