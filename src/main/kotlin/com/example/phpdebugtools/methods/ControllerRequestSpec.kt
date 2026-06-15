package com.example.phpdebugtools.methods

enum class HttpRequestMethod(
    val wireValue: String,
    val supportsBody: Boolean,
) {
    GET("GET", false),
    POST("POST", true),
    PUT("PUT", true),
    PATCH("PATCH", true),
    DELETE("DELETE", false),
    HEAD("HEAD", false),
    OPTIONS("OPTIONS", false),
    ;

    override fun toString(): String = wireValue
}

enum class RequestBodyMode(
    val wireValue: String,
) {
    NONE("none"),
    FORM_DATA("form-data"),
    X_WWW_FORM_URLENCODED("x-www-form-urlencoded"),
    JSON("json"),
    ;

    override fun toString(): String = when (this) {
        NONE -> "none"
        FORM_DATA -> "form-data"
        X_WWW_FORM_URLENCODED -> "x-www-form-urlencoded"
        JSON -> "JSON"
    }
}

data class ControllerRequestSpec(
    val method: HttpRequestMethod,
    val bodyMode: RequestBodyMode,
)
