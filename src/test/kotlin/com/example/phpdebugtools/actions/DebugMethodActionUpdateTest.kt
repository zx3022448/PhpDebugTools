package com.example.phpdebugtools.actions

import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class DebugMethodActionUpdateTest : BasePlatformTestCase() {
    private val controllerAction = DebugControllerMethodAction()
    private val serviceAction = DebugServiceMethodAction()

    fun testControllerMethodCaretOnlyShowsControllerAction() {
        configurePhpFile(
            "UserController.php",
            """
            <?php
            namespace app\controller;
            class UserController {
                public function sh<caret>ow() {}
            }
            """.trimIndent(),
        )

        val controllerPresentation = updatePresentation(controllerAction)
        val servicePresentation = updatePresentation(serviceAction)

        assertTrue(controllerPresentation.isEnabledAndVisible)
        assertFalse(servicePresentation.isEnabledAndVisible)
    }

    fun testServiceMethodCaretOnlyShowsServiceAction() {
        configurePhpFile(
            "UserService.php",
            """
            <?php
            namespace app\service;
            class UserService {
                public function lo<caret>ad() {}
            }
            """.trimIndent(),
        )

        val controllerPresentation = updatePresentation(controllerAction)
        val servicePresentation = updatePresentation(serviceAction)

        assertFalse(controllerPresentation.isEnabledAndVisible)
        assertTrue(servicePresentation.isEnabledAndVisible)
    }

    fun testOutsideMethodHidesBothActions() {
        configurePhpFile(
            "UserController.php",
            """
            <?php
            namespace app\controller;
            cl<caret>ass UserController {
                public function show() {}
            }
            """.trimIndent(),
        )

        val controllerPresentation = updatePresentation(controllerAction)
        val servicePresentation = updatePresentation(serviceAction)

        assertFalse(controllerPresentation.isEnabledAndVisible)
        assertFalse(servicePresentation.isEnabledAndVisible)
    }

    private fun updatePresentation(action: AnAction): Presentation {
        val presentation = Presentation()
        action.update(
            AnActionEvent.createFromDataContext(
                ActionPlaces.UNKNOWN,
                presentation,
                SimpleDataContext.builder()
                    .add(CommonDataKeys.PROJECT, project)
                    .add(CommonDataKeys.PSI_FILE, myFixture.file)
                    .add(CommonDataKeys.EDITOR, myFixture.editor)
                    .build(),
            ),
        )
        return presentation
    }

    private fun configurePhpFile(path: String, content: String) {
        val file = myFixture.tempDirFixture.createFile(path, content)
        myFixture.configureFromExistingVirtualFile(file)
    }
}
