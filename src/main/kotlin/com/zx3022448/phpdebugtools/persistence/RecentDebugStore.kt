package com.zx3022448.phpdebugtools.persistence

import com.zx3022448.phpdebugtools.execution.DetectedPhpRuntime
import com.zx3022448.phpdebugtools.methods.ControllerRequestSpec
import com.zx3022448.phpdebugtools.methods.HttpRequestMethod
import com.zx3022448.phpdebugtools.methods.MethodDebugTarget
import com.zx3022448.phpdebugtools.methods.MethodKind
import com.zx3022448.phpdebugtools.methods.MethodLookupItem
import com.zx3022448.phpdebugtools.methods.MethodParameterSchema
import com.zx3022448.phpdebugtools.methods.RequestBodyMode
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@Service(Service.Level.PROJECT)
@State(
    name = "PhpDebugToolsRecentDebugStore",
    storages = [Storage("php-debug-tools.xml")],
)
class RecentDebugStore : PersistentStateComponent<RecentDebugStore.State> {

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = State(
            recentUrls = normalizeEntries(state.recentUrls),
            recentMethods = normalizeEntries(state.recentMethods),
            recentPhpExecutables = normalizeEntries(state.recentPhpExecutables),
            selectedPhpExecutable = state.selectedPhpExecutable.trim(),
            cachedMethods = state.cachedMethods,
            cachedPhpRuntimes = state.cachedPhpRuntimes,
        )
    }

    fun rememberUrl(url: String) {
        state = state.copy(recentUrls = (listOf(url) + state.recentUrls).distinct().take(10))
    }

    fun rememberMethod(method: String) {
        state = state.copy(recentMethods = (listOf(method) + state.recentMethods).distinct().take(10))
    }

    fun rememberPhpExecutable(command: String) {
        val normalized = command.trim()
        if (normalized.isEmpty()) {
            return
        }
        state = state.copy(
            recentPhpExecutables = (listOf(normalized) + state.recentPhpExecutables).distinct().take(10),
            selectedPhpExecutable = normalized,
        )
    }

    fun selectedPhpExecutable(): String = state.selectedPhpExecutable

    fun recentPhpExecutables(): List<String> = state.recentPhpExecutables

    fun cachedMethodLookupItems(): List<MethodLookupItem> {
        return state.cachedMethods.mapNotNull { it.toLookupItem() }
    }

    fun rememberMethodLookupItems(items: List<MethodLookupItem>) {
        state = state.copy(
            cachedMethods = items
                .take(MAX_CACHED_METHODS)
                .map(CachedMethodState::fromLookupItem),
        )
    }

    fun cachedPhpRuntimes(): List<DetectedPhpRuntime> {
        return state.cachedPhpRuntimes.map {
            DetectedPhpRuntime(
                command = it.command,
                version = it.version,
                source = it.source,
            )
        }
    }

    fun rememberPhpRuntimes(runtimes: List<DetectedPhpRuntime>) {
        state = state.copy(
            cachedPhpRuntimes = runtimes
                .take(MAX_CACHED_RUNTIMES)
                .map {
                    CachedPhpRuntimeState(
                        command = it.command,
                        version = it.version,
                        source = it.source,
                    )
                },
        )
    }

    data class State(
        var recentUrls: List<String> = emptyList(),
        var recentMethods: List<String> = emptyList(),
        var recentPhpExecutables: List<String> = emptyList(),
        var selectedPhpExecutable: String = "",
        var cachedMethods: List<CachedMethodState> = emptyList(),
        var cachedPhpRuntimes: List<CachedPhpRuntimeState> = emptyList(),
    )

    data class CachedPhpRuntimeState(
        var command: String = "",
        var version: String = "",
        var source: String = "",
    )

    data class CachedMethodState(
        var kind: String = MethodKind.SERVICE.name,
        var classFqn: String = "",
        var methodName: String = "",
        var isStatic: Boolean = false,
        var parameters: List<CachedParameterState> = emptyList(),
        var requestMethod: String = "",
        var requestBodyMode: String = "",
    ) {
        fun toLookupItem(): MethodLookupItem? {
            if (classFqn.isBlank() || methodName.isBlank()) {
                return null
            }
            val methodKind = runCatching { MethodKind.valueOf(kind) }.getOrDefault(MethodKind.SERVICE)
            val controllerSpec = if (requestMethod.isNotBlank() && requestBodyMode.isNotBlank()) {
                val httpMethod = HttpRequestMethod.entries.firstOrNull { it.wireValue == requestMethod }
                val bodyMode = RequestBodyMode.entries.firstOrNull { it.wireValue == requestBodyMode }
                if (httpMethod != null && bodyMode != null) {
                    ControllerRequestSpec(method = httpMethod, bodyMode = bodyMode)
                } else {
                    null
                }
            } else {
                null
            }
            return MethodLookupItem(
                MethodDebugTarget(
                    kind = methodKind,
                    classFqn = classFqn,
                    methodName = methodName,
                    isStatic = isStatic,
                    parameters = parameters.map {
                        MethodParameterSchema(
                            name = it.name,
                            declaredType = it.declaredType.ifBlank { null },
                            required = it.required,
                            defaultValue = it.defaultValue.ifBlank { null },
                        )
                    },
                    controllerRequestSpec = controllerSpec,
                ),
            )
        }

        companion object {
            fun fromLookupItem(item: MethodLookupItem): CachedMethodState {
                val target = item.target
                return CachedMethodState(
                    kind = target.kind.name,
                    classFqn = target.classFqn,
                    methodName = target.methodName,
                    isStatic = target.isStatic,
                    parameters = target.parameters.map {
                        CachedParameterState(
                            name = it.name,
                            declaredType = it.declaredType.orEmpty(),
                            required = it.required,
                            defaultValue = it.defaultValue.orEmpty(),
                        )
                    },
                    requestMethod = target.controllerRequestSpec?.method?.wireValue.orEmpty(),
                    requestBodyMode = target.controllerRequestSpec?.bodyMode?.wireValue.orEmpty(),
                )
            }
        }
    }

    data class CachedParameterState(
        var name: String = "",
        var declaredType: String = "",
        var required: Boolean = false,
        var defaultValue: String = "",
    )

    private fun normalizeEntries(entries: List<String>): List<String> {
        return entries
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(10)
    }

    private companion object {
        const val MAX_CACHED_METHODS = 1_000
        const val MAX_CACHED_RUNTIMES = 20
    }
}
