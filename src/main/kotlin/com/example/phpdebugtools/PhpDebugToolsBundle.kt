package com.example.phpdebugtools

import com.intellij.DynamicBundle
import org.jetbrains.annotations.PropertyKey

private const val BUNDLE = "messages.MyMessageBundle"

object PhpDebugToolsBundle {
    private val bundle = DynamicBundle(PhpDebugToolsBundle::class.java, BUNDLE)

    @JvmStatic
    fun message(key: @PropertyKey(resourceBundle = BUNDLE) String, vararg params: Any?): String {
        return bundle.getMessage(key, *params)
    }
}
