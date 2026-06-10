package com.example.phpdebugtools.actions

import com.example.phpdebugtools.methods.MethodDebugTarget
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.project.Project
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class DebugMethodActionUpdateTest : BasePlatformTestCase() {
    private val shownTargets = mutableListOf<MethodDebugTarget>()
    private val shownProjects = mutableListOf<Project>()
    private val controllerAction = DebugControllerMethodAction(::recordShownDialog)
    private val serviceAction = DebugServiceMethodAction(::recordShownDialog)

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

    fun testControllerActionPerformedShowsDialogForControllerMethod() {
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

        controllerAction.actionPerformed(createEvent())

        assertEquals(1, shownTargets.size)
        assertEquals("show", shownTargets.single().methodName)
        assertEquals(project, shownProjects.single())
    }

    fun testServiceActionPerformedShowsDialogForServiceMethod() {
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

        serviceAction.actionPerformed(createEvent())

        assertEquals(1, shownTargets.size)
        assertEquals("load", shownTargets.single().methodName)
        assertEquals(project, shownProjects.single())
    }

    fun testControllerActionPerformedDoesNotShowDialogForServiceMethod() {
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

        controllerAction.actionPerformed(createEvent())

        assertTrue(shownTargets.isEmpty())
        assertTrue(shownProjects.isEmpty())
    }

    fun testControllerActionPerformedDoesNotShowDialogOutsideMethod() {
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

        controllerAction.actionPerformed(createEvent())

        assertTrue(shownTargets.isEmpty())
        assertTrue(shownProjects.isEmpty())
    }

    fun testServiceActionPerformedDoesNotShowDialogForControllerMethod() {
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

        serviceAction.actionPerformed(createEvent())

        assertTrue(shownTargets.isEmpty())
        assertTrue(shownProjects.isEmpty())
    }

    fun testServiceActionPerformedDoesNotShowDialogOutsideMethod() {
        configurePhpFile(
            "UserService.php",
            """
            <?php
            namespace app\service;
            cl<caret>ass UserService {
                public function load() {}
            }
            """.trimIndent(),
        )

        serviceAction.actionPerformed(createEvent())

        assertTrue(shownTargets.isEmpty())
        assertTrue(shownProjects.isEmpty())
    }

    override fun setUp() {
        super.setUp()
        shownTargets.clear()
        shownProjects.clear()
    }

    private fun updatePresentation(action: AnAction): Presentation {
        val presentation = Presentation()
        action.update(createEvent(presentation))
        return presentation
    }

    private fun createEvent(presentation: Presentation = Presentation()): AnActionEvent {
        return AnActionEvent.createFromDataContext(
            ActionPlaces.UNKNOWN,
            presentation,
            SimpleDataContext.builder()
                .add(CommonDataKeys.PROJECT, project)
                .add(CommonDataKeys.PSI_FILE, myFixture.file)
                .add(CommonDataKeys.EDITOR, myFixture.editor)
                .build(),
        )
    }

    private fun recordShownDialog(project: Project, target: MethodDebugTarget) {
        shownProjects += project
        shownTargets += target
    }

    private fun configurePhpFile(path: String, content: String) {
        val file = myFixture.tempDirFixture.createFile(path, content)
        myFixture.configureFromExistingVirtualFile(file)
    }
}
