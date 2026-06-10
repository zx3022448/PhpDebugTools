package com.example.phpdebugtools.execution

import org.junit.Assert.assertEquals
import org.junit.Test

class WebDebugUrlBuilderTest {
    @Test
    fun appendsDebugTriggerAndPayloadFileToUrl() {
        val url = WebDebugUrlBuilder.build(
            baseUrl = "http://127.0.0.1/index.php",
            runtimePath = "/.php-debug-tools/debug-web-entry.php",
            payloadFile = "controller-payload.json",
        )

        assertEquals(
            "http://127.0.0.1/.php-debug-tools/debug-web-entry.php?XDEBUG_TRIGGER=PHPSTORM&payload=controller-payload.json",
            url,
        )
    }
}
