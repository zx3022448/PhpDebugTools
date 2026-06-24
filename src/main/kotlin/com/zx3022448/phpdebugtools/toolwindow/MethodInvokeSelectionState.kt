package com.zx3022448.phpdebugtools.toolwindow

import com.zx3022448.phpdebugtools.methods.ControllerRequestSpec
import com.zx3022448.phpdebugtools.methods.HttpRequestMethod
import com.zx3022448.phpdebugtools.methods.MethodKind
import com.zx3022448.phpdebugtools.methods.MethodLookupItem
import com.zx3022448.phpdebugtools.methods.MethodParameterSchema
import com.zx3022448.phpdebugtools.methods.RequestBodyMode

data class MethodInvokeSelectionState(
    val targetSignature: String,
    val parameterLines: List<String>,
    val argsTemplate: String,
    val controllerRequest: ControllerRequestViewState?,
    val showRequestContext: Boolean,
)

data class ControllerRequestViewState(
    val requestMethod: HttpRequestMethod,
    val bodyMode: RequestBodyMode,
    val queryParameters: List<RequestParameterDraft>,
    val headerParameters: List<RequestParameterDraft>,
    val bodyParameters: List<RequestParameterDraft>,
    val bodyJsonTemplate: String,
)

data class RequestParameterDraft(
    val name: String = "",
    val type: String = "string",
    val example: String = "",
    val description: String = "",
)

internal fun filterMethodLookupItems(items: List<MethodLookupItem>, keyword: String): List<MethodLookupItem> {
    val normalizedKeyword = keyword.trim().lowercase()
    if (normalizedKeyword.isEmpty()) {
        return items.take(20)
    }

    return items
        .mapNotNull { item ->
            val score = scoreLookupItem(item, normalizedKeyword)
            if (score == Int.MIN_VALUE) {
                null
            } else {
                score to item
            }
        }
        .sortedWith(compareByDescending<Pair<Int, MethodLookupItem>> { it.first }.thenBy { it.second.targetSignature })
        .map { it.second }
        .take(20)
}

internal fun buildMethodInvokeSelectionState(item: MethodLookupItem): MethodInvokeSelectionState {
    val controllerRequest = if (item.target.kind == MethodKind.CONTROLLER) {
        buildControllerRequestState(item)
    } else {
        null
    }

    val parameterLines = buildList {
        controllerRequest?.let {
            add("请求上下文：${it.requestMethod.wireValue} / ${it.bodyMode.wireValue}")
        }
        if (item.target.parameters.isEmpty()) {
            add("当前方法没有显式参数。")
        } else {
            addAll(
                item.target.parameters.mapIndexed { index, parameter ->
                    "${index + 1}. \$${parameter.name}: ${parameter.declaredType ?: "mixed"} ${if (parameter.required) "必填" else "可选"}"
                },
            )
        }
    }

    return MethodInvokeSelectionState(
        targetSignature = item.targetSignature,
        parameterLines = parameterLines,
        argsTemplate = buildArgsTemplate(item),
        controllerRequest = controllerRequest,
        showRequestContext = controllerRequest != null,
    )
}

private fun scoreLookupItem(item: MethodLookupItem, keyword: String): Int {
    val methodName = item.target.methodName.lowercase()
    val classFqn = item.target.classFqn.lowercase()
    val signature = item.targetSignature.lowercase()
    return when {
        methodName == keyword -> 400
        methodName.startsWith(keyword) -> 300
        signature.contains(keyword) -> 200
        classFqn.contains(keyword) -> 100
        item.searchableText.contains(keyword) -> 50
        else -> Int.MIN_VALUE
    }
}

private fun buildControllerRequestState(item: MethodLookupItem): ControllerRequestViewState {
    val requestSpec = item.target.controllerRequestSpec ?: fallbackControllerRequestSpec(item)
    val parameterDrafts = item.target.parameters.map(::toRequestParameterDraft)
    return ControllerRequestViewState(
        requestMethod = requestSpec.method,
        bodyMode = requestSpec.bodyMode,
        queryParameters = if (requestSpec.method.supportsBody) emptyList() else parameterDrafts,
        headerParameters = buildDefaultHeaderParameters(requestSpec),
        bodyParameters = if (requestSpec.method.supportsBody) parameterDrafts else emptyList(),
        bodyJsonTemplate = buildBodyJsonTemplate(item.target.parameters),
    )
}

private fun fallbackControllerRequestSpec(item: MethodLookupItem): ControllerRequestSpec {
    val method = when {
        item.target.methodName.startsWith("show", ignoreCase = true) ||
            item.target.methodName.startsWith("get", ignoreCase = true) ||
            item.target.methodName.startsWith("list", ignoreCase = true) -> HttpRequestMethod.GET
        item.target.methodName.startsWith("update", ignoreCase = true) ||
            item.target.methodName.startsWith("edit", ignoreCase = true) -> HttpRequestMethod.PUT
        item.target.methodName.startsWith("delete", ignoreCase = true) ||
            item.target.methodName.startsWith("remove", ignoreCase = true) -> HttpRequestMethod.DELETE
        else -> HttpRequestMethod.POST
    }
    val bodyMode = if (method.supportsBody) {
        if (method == HttpRequestMethod.PUT || method == HttpRequestMethod.PATCH) {
            RequestBodyMode.JSON
        } else {
            RequestBodyMode.X_WWW_FORM_URLENCODED
        }
    } else {
        RequestBodyMode.NONE
    }
    return ControllerRequestSpec(method = method, bodyMode = bodyMode)
}

private fun buildDefaultHeaderParameters(spec: ControllerRequestSpec): List<RequestParameterDraft> {
    val contentType = when (spec.bodyMode) {
        RequestBodyMode.NONE -> return emptyList()
        RequestBodyMode.FORM_DATA -> "multipart/form-data"
        RequestBodyMode.X_WWW_FORM_URLENCODED -> "application/x-www-form-urlencoded"
        RequestBodyMode.JSON -> "application/json"
    }
    return listOf(
        RequestParameterDraft(
            name = "Content-Type",
            type = "string",
            example = contentType,
            description = "自动推断的请求体类型",
        ),
    )
}

private fun buildArgsTemplate(item: MethodLookupItem): String {
    return item.target.parameters.joinToString(prefix = "[", postfix = "]") { parameter ->
        defaultJsonValue(parameter)
    }
}

private fun buildBodyJsonTemplate(parameters: List<MethodParameterSchema>): String {
    return parameters.joinToString(prefix = "{", postfix = "}", separator = ",") { parameter ->
        "\"${parameter.name}\":${defaultJsonValue(parameter)}"
    }.ifEmpty { "{}" }
}

private fun defaultJsonValue(parameter: MethodParameterSchema): String {
    if (!parameter.required) {
        return parameter.defaultValue ?: "null"
    }

    return when (normalizeParameterType(parameter.declaredType ?: "string")) {
        "integer" -> "0"
        "number" -> "0.0"
        "boolean" -> "false"
        "array" -> "[]"
        "object" -> "{}"
        "string" -> "\"\""
        else -> "null"
    }
}
