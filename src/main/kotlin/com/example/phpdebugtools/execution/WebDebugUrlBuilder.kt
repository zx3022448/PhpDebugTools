package com.example.phpdebugtools.execution

object WebDebugUrlBuilder {
    fun build(baseUrl: String, runtimePath: String, payloadFile: String): String {
        val prefix = baseUrl.substringBefore("/index.php")
        return "$prefix$runtimePath?XDEBUG_TRIGGER=PHPSTORM&payload=$payloadFile"
    }
}
