package com.example.phpdebugtools.methods

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class PhpMethodTargetResolverTest : BasePlatformTestCase() {
    fun testResolvesControllerMethodUnderCaret() {
        myFixture.configureByText(
            "UserController.php",
            """
            <?php
            namespace app\controller;
            class UserController {
                public static function sh<caret>ow(\DateTime ${'$'}id, \stdClass ${'$'}payload = null) {}
            }
            """.trimIndent()
        )

        val target = PhpMethodTargetResolver.resolve(myFixture.file, myFixture.caretOffset)

        assertEquals(MethodKind.CONTROLLER, target?.kind)
        assertEquals("\\app\\controller\\UserController", target?.classFqn)
        assertEquals("show", target?.methodName)
        assertEquals(true, target?.isStatic)
        assertEquals(2, target?.parameters?.size)
        assertEquals("id", target?.parameters?.get(0)?.name)
        assertEquals("\\DateTime", target?.parameters?.get(0)?.declaredType)
        assertEquals(true, target?.parameters?.get(0)?.required)
        assertEquals(null, target?.parameters?.get(0)?.defaultValue)
        assertEquals("payload", target?.parameters?.get(1)?.name)
        assertEquals("null|\\stdClass", target?.parameters?.get(1)?.declaredType)
        assertEquals(false, target?.parameters?.get(1)?.required)
        assertEquals("null", target?.parameters?.get(1)?.defaultValue)
    }

    fun testResolvesServiceMethodUnderCaret() {
        myFixture.configureByText(
            "UserService.php",
            """
            <?php
            namespace app\service;
            class UserService {
                public function lo<caret>ad(string ${'$'}id) {}
            }
            """.trimIndent()
        )

        val target = PhpMethodTargetResolver.resolve(myFixture.file, myFixture.caretOffset)

        assertEquals(MethodKind.SERVICE, target?.kind)
        assertEquals("\\app\\service\\UserService", target?.classFqn)
        assertEquals("load", target?.methodName)
        assertEquals(false, target?.isStatic)
        assertEquals(1, target?.parameters?.size)
    }

    fun testReturnsNullOutsideMethod() {
        myFixture.configureByText(
            "UserController.php",
            """
            <?php
            namespace app\controller;
            cl<caret>ass UserController {
                public function show() {}
            }
            """.trimIndent()
        )

        val target = PhpMethodTargetResolver.resolve(myFixture.file, myFixture.caretOffset)

        assertNull(target)
    }
}
