package com.example.phpdebugtools.startup

import com.example.phpdebugtools.execution.PhpRuntimeDetector
import com.example.phpdebugtools.execution.ProcessCommandRunner
import com.example.phpdebugtools.methods.MethodLookupItem
import com.example.phpdebugtools.methods.ProjectMethodCollector
import com.example.phpdebugtools.persistence.RecentDebugStore
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class PhpDebugToolsProjectActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        logger.debug("PhpDebugTools startup cache warmup scheduled for project '${project.name}'")
        DumbService.getInstance(project).runWhenSmart {
            ApplicationManager.getApplication().executeOnPooledThread {
                warmupCaches(project)
            }
        }
    }

    private companion object {
        private val logger = Logger.getInstance(PhpDebugToolsProjectActivity::class.java)

        private fun warmupCaches(project: Project) {
            val store = project.service<RecentDebugStore>()
            runCatching {
                val methods = ReadAction.compute<List<MethodLookupItem>, RuntimeException> {
                    ProjectMethodCollector.collect(project)
                }
                store.rememberMethodLookupItems(methods)
                logger.debug("PhpDebugTools cached ${methods.size} method entries for project '${project.name}'")
            }.onFailure {
                logger.debug("PhpDebugTools method cache warmup failed for project '${project.name}'", it)
            }

            runCatching {
                val runtimes = PhpRuntimeDetector(ProcessCommandRunner()).detect()
                store.rememberPhpRuntimes(runtimes)
                logger.debug("PhpDebugTools cached ${runtimes.size} PHP runtimes for project '${project.name}'")
            }.onFailure {
                logger.debug("PhpDebugTools PHP runtime cache warmup failed for project '${project.name}'", it)
            }
        }
    }
}
