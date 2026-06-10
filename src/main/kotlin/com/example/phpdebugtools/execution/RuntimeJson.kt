package com.example.phpdebugtools.execution

object RuntimeJson {
    fun servicePayload(
        classFqn: String,
        methodName: String,
        isStatic: Boolean,
        argsJson: String,
    ): String {
        val normalizedArgs = argsJson.trim().ifEmpty { "[]" }
        require(isValidJsonArray(normalizedArgs)) {
            "argsJson must be a JSON array"
        }

        return buildString {
            append("{")
            append("\"type\":\"service\",")
            append("\"class\":\"").append(escape(classFqn)).append("\",")
            append("\"method\":\"").append(escape(methodName)).append("\",")
            append("\"static\":").append(isStatic).append(",")
            append("\"args\":").append(normalizedArgs)
            append("}")
        }
    }

    private fun isValidJsonArray(value: String): Boolean {
        if (value.length < 2 || value.first() != '[' || value.last() != ']') {
            return false
        }

        var inString = false
        var escaped = false
        var bracketDepth = 0
        var braceDepth = 0

        for (index in value.indices) {
            val char = value[index]
            if (inString) {
                if (escaped) {
                    escaped = false
                } else if (char == '\\') {
                    escaped = true
                } else if (char == '"') {
                    inString = false
                }
                continue
            }

            when (char) {
                '"' -> inString = true
                '[' -> bracketDepth++
                ']' -> {
                    bracketDepth--
                    if (bracketDepth < 0) {
                        return false
                    }
                }
                '{' -> braceDepth++
                '}' -> {
                    braceDepth--
                    if (braceDepth < 0) {
                        return false
                    }
                }
            }
        }

        if (inString || escaped || bracketDepth != 0 || braceDepth != 0) {
            return false
        }

        val inner = value.substring(1, value.length - 1).trim()
        if (inner.isEmpty()) {
            return true
        }

        return inner.splitTopLevel(',')
            .all { token ->
                val trimmed = token.trim()
                trimmed.isNotEmpty() && isValidJsonValue(trimmed)
            }
    }

    private fun isValidJsonValue(value: String): Boolean {
        if (value.isEmpty()) {
            return false
        }

        return when {
            value == "null" || value == "true" || value == "false" -> true
            value.startsWith("\"") -> isValidJsonString(value)
            value.startsWith("[") -> isValidJsonArray(value)
            value.startsWith("{") -> isValidJsonObject(value)
            else -> isValidJsonNumber(value)
        }
    }

    private fun isValidJsonObject(value: String): Boolean {
        if (value.length < 2 || value.first() != '{' || value.last() != '}') {
            return false
        }

        val inner = value.substring(1, value.length - 1).trim()
        if (inner.isEmpty()) {
            return true
        }

        return inner.splitTopLevel(',')
            .all { entry ->
                val separatorIndex = findTopLevelColon(entry) ?: return false
                val key = entry.substring(0, separatorIndex).trim()
                val fieldValue = entry.substring(separatorIndex + 1).trim()
                isValidJsonString(key) && isValidJsonValue(fieldValue)
            }
    }

    private fun isValidJsonString(value: String): Boolean {
        if (value.length < 2 || value.first() != '"' || value.last() != '"') {
            return false
        }

        var escaped = false
        for (index in 1 until value.length - 1) {
            val char = value[index]
            if (escaped) {
                escaped = false
                continue
            }
            if (char == '\\') {
                escaped = true
            } else if (char == '"') {
                return false
            }
        }

        return !escaped
    }

    private fun isValidJsonNumber(value: String): Boolean {
        return value.matches(Regex("""-?(0|[1-9]\d*)(\.\d+)?([eE][+-]?\d+)?"""))
    }

    private fun String.splitTopLevel(separator: Char): List<String> {
        val result = mutableListOf<String>()
        var start = 0
        var inString = false
        var escaped = false
        var bracketDepth = 0
        var braceDepth = 0

        forEachIndexed { index, char ->
            if (inString) {
                if (escaped) {
                    escaped = false
                } else if (char == '\\') {
                    escaped = true
                } else if (char == '"') {
                    inString = false
                }
                return@forEachIndexed
            }

            when (char) {
                '"' -> inString = true
                '[' -> bracketDepth++
                ']' -> bracketDepth--
                '{' -> braceDepth++
                '}' -> braceDepth--
                separator -> if (bracketDepth == 0 && braceDepth == 0) {
                    result += substring(start, index)
                    start = index + 1
                }
            }
        }

        result += substring(start)
        return result
    }

    private fun findTopLevelColon(value: String): Int? {
        var inString = false
        var escaped = false
        var bracketDepth = 0
        var braceDepth = 0

        value.forEachIndexed { index, char ->
            if (inString) {
                if (escaped) {
                    escaped = false
                } else if (char == '\\') {
                    escaped = true
                } else if (char == '"') {
                    inString = false
                }
                return@forEachIndexed
            }

            when (char) {
                '"' -> inString = true
                '[' -> bracketDepth++
                ']' -> bracketDepth--
                '{' -> braceDepth++
                '}' -> braceDepth--
                ':' -> if (bracketDepth == 0 && braceDepth == 0) {
                    return index
                }
            }
        }

        return null
    }

    private fun escape(value: String): String {
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
}
