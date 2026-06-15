package com.example.phpdebugtools.methods

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.jetbrains.php.lang.PhpFileType
import com.jetbrains.php.lang.psi.elements.Method

object ProjectMethodCollector {
    fun collect(project: Project): List<MethodLookupItem> {
        val psiManager = PsiManager.getInstance(project)
        val projectFileIndex = ProjectFileIndex.getInstance(project)
        return FileTypeIndex.getFiles(PhpFileType.INSTANCE, GlobalSearchScope.projectScope(project))
            .asSequence()
            .filter { projectFileIndex.isInSourceContent(it) || looksLikeProjectPhpFile(it) }
            .mapNotNull(psiManager::findFile)
            .flatMap { file -> PsiTreeUtil.findChildrenOfType(file, Method::class.java).asSequence() }
            .mapNotNull(::toLookupItem)
            .sortedBy { it.targetSignature }
            .toList()
    }

    private fun looksLikeProjectPhpFile(file: VirtualFile): Boolean {
        val path = file.path.replace('\\', '/')
        return path.contains("/app/") || path.contains("/application/")
    }

    private fun toLookupItem(method: Method): MethodLookupItem? {
        val containingClass = method.containingClass ?: return null
        return MethodLookupItem(
            MethodDebugTarget(
                kind = if (
                    containingClass.fqn.contains("\\controller\\", ignoreCase = true) ||
                    containingClass.name?.endsWith("Controller") == true
                ) {
                    MethodKind.CONTROLLER
                } else {
                    MethodKind.SERVICE
                },
                classFqn = containingClass.fqn,
                methodName = method.name,
                isStatic = method.modifier.isStatic,
                parameters = method.parameters.map { parameter ->
                    MethodParameterSchema(
                        name = parameter.name,
                        declaredType = parameter.type.toString(),
                        required = !parameter.isOptional,
                        defaultValue = parameter.defaultValue?.text,
                    )
                },
                controllerRequestSpec = if (
                    containingClass.fqn.contains("\\controller\\", ignoreCase = true) ||
                    containingClass.name?.endsWith("Controller") == true
                ) {
                    detectControllerRequestSpec(method.name, method.text)
                } else {
                    null
                },
            ),
        )
    }

    private fun detectControllerRequestSpec(methodName: String, methodText: String): ControllerRequestSpec {
        val normalizedName = methodName.lowercase()
        val normalizedText = methodText.lowercase()

        val method = when {
            containsAny(normalizedText, "#[post", "@method post", "ispost(", "->post(", "request()->post(") -> HttpRequestMethod.POST
            containsAny(normalizedText, "#[put", "@method put", "isput(") -> HttpRequestMethod.PUT
            containsAny(normalizedText, "#[patch", "@method patch", "ispatch(") -> HttpRequestMethod.PATCH
            containsAny(normalizedText, "#[delete", "@method delete", "isdelete(") -> HttpRequestMethod.DELETE
            containsAny(normalizedText, "#[head", "@method head", "ishead(") -> HttpRequestMethod.HEAD
            containsAny(normalizedText, "#[options", "@method options", "isoptions(") -> HttpRequestMethod.OPTIONS
            containsAny(normalizedText, "#[get", "@method get", "isget(", "->get(", "request()->get(") -> HttpRequestMethod.GET
            normalizedName.startsWithAny("create", "save", "store", "add", "upload", "submit") -> HttpRequestMethod.POST
            normalizedName.startsWithAny("update", "edit", "put") -> HttpRequestMethod.PUT
            normalizedName.startsWithAny("patch") -> HttpRequestMethod.PATCH
            normalizedName.startsWithAny("delete", "remove", "destroy") -> HttpRequestMethod.DELETE
            normalizedName.startsWithAny("index", "list", "show", "detail", "get", "query", "search", "find") -> HttpRequestMethod.GET
            containsAny(normalizedText, "->file(", "request()->file(", "\$_files", "multipart/form-data") -> HttpRequestMethod.POST
            else -> HttpRequestMethod.GET
        }

        val bodyMode = when {
            !method.supportsBody -> RequestBodyMode.NONE
            containsAny(normalizedText, "->file(", "request()->file(", "\$_files", "multipart/form-data") -> RequestBodyMode.FORM_DATA
            containsAny(normalizedText, "application/json", "php://input", "json_decode(", "getinput(") -> RequestBodyMode.JSON
            containsAny(
                normalizedText,
                "application/x-www-form-urlencoded",
                "->post(",
                "request()->post(",
                "input('post.",
                "input(\"post.",
            ) -> RequestBodyMode.X_WWW_FORM_URLENCODED
            method == HttpRequestMethod.PUT || method == HttpRequestMethod.PATCH -> RequestBodyMode.JSON
            else -> RequestBodyMode.X_WWW_FORM_URLENCODED
        }

        return ControllerRequestSpec(method = method, bodyMode = bodyMode)
    }

    private fun containsAny(text: String, vararg tokens: String): Boolean = tokens.any(text::contains)

    private fun String.startsWithAny(vararg prefixes: String): Boolean = prefixes.any { startsWith(it) }
}
