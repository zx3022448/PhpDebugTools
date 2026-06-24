package com.zx3022448.phpdebugtools.toolwindow

import com.zx3022448.phpdebugtools.methods.MethodParameterSchema

internal fun requestParameterDraftsToJson(rows: List<RequestParameterDraft>): String {
    val filteredRows = rows.filter { it.name.isNotBlank() }
    if (filteredRows.isEmpty()) {
        return "{}"
    }

    return filteredRows.joinToString(prefix = "{", postfix = "}", separator = ",") { row ->
        "\"${escapeJson(row.name)}\":${serializeDraftValue(row)}"
    }
}

internal fun requestParameterDraftsToJsonArray(rows: List<RequestParameterDraft>): String {
    if (rows.isEmpty()) {
        return "[]"
    }

    return rows.joinToString(prefix = "[", postfix = "]", separator = ",") { row ->
        serializeDraftValue(row)
    }
}

internal fun toRequestParameterDraft(parameter: MethodParameterSchema): RequestParameterDraft {
    val declaredType = parameter.declaredType?.takeIf { it.isNotBlank() } ?: "string"
    return RequestParameterDraft(
        name = parameter.name,
        type = normalizeParameterType(declaredType),
        example = defaultDraftExample(parameter),
        description = if (parameter.required) "必填" else "可选",
    )
}

internal fun normalizeParameterType(type: String): String {
    val normalized = type.lowercase()
    return when {
        "int" in normalized -> "integer"
        "float" in normalized || "double" in normalized || "number" in normalized -> "number"
        "bool" in normalized -> "boolean"
        "array" in normalized -> "array"
        "object" in normalized -> "object"
        else -> "string"
    }
}

internal fun defaultDraftExample(parameter: MethodParameterSchema): String {
    val defaultValue = parameter.defaultValue?.trim()
    if (!defaultValue.isNullOrEmpty()) {
        return defaultValue
    }

    return when (normalizeParameterType(parameter.declaredType ?: "string")) {
        "integer" -> "0"
        "number" -> "0.0"
        "boolean" -> "false"
        "array" -> "[]"
        "object" -> "{}"
        else -> ""
    }
}

internal fun serializeDraftValue(row: RequestParameterDraft): String {
    val type = row.type.trim().lowercase()
    val example = row.example.trim()
    if (example.isEmpty()) {
        return when (type) {
            "integer" -> "0"
            "number" -> "0.0"
            "boolean" -> "false"
            "array" -> "[]"
            "object" -> "{}"
            else -> "\"\""
        }
    }

    return when (type) {
        "integer" -> example.toLongOrNull()?.toString() ?: "0"
        "number" -> example.toDoubleOrNull()?.let {
            if (example.contains('.')) example else "${it.toLong()}.0"
        } ?: "0.0"
        "boolean" -> if (example.equals("true", ignoreCase = true) || example == "1") "true" else "false"
        "array", "object" -> example
        else -> "\"${escapeJson(example.trim('"'))}\""
    }
}

internal fun escapeJson(value: String): String {
    return buildString(value.length) {
        value.forEach { char ->
            when (char) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(char)
            }
        }
    }
}
