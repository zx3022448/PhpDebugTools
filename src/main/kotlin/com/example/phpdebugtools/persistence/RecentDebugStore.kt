package com.example.phpdebugtools.persistence

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
        )
    }

    fun rememberUrl(url: String) {
        state = state.copy(recentUrls = (listOf(url) + state.recentUrls).distinct().take(10))
    }

    fun rememberMethod(method: String) {
        state = state.copy(recentMethods = (listOf(method) + state.recentMethods).distinct().take(10))
    }

    data class State(
        var recentUrls: List<String> = emptyList(),
        var recentMethods: List<String> = emptyList(),
    )

    private fun normalizeEntries(entries: List<String>): List<String> {
        return entries
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(10)
    }
}
