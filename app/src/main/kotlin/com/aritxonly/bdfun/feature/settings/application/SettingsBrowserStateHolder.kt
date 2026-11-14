package com.aritxonly.bdfun.feature.settings.application

import com.aritxonly.bdfun.core.domain.SettingsIndex
import com.aritxonly.bdfun.core.model.settings.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal data class SettingsBrowserState(
    val route: String? = null,
    val searching: Boolean = false,
    val searchRoute: String? = null,
    val query: String = "",
    val highlight: String? = null,
)

/** 页面拥有自己的搜索和导航；旋转只改变布局，不另建一份选择状态。 */
internal class SettingsBrowserStateHolder(initial: SettingsBrowserState = SettingsBrowserState()) {
    private val mutable = MutableStateFlow(initial)
    val state = mutable.asStateFlow()

    fun open(route: String, prefName: String? = null) {
        mutable.value = SettingsBrowserState(route = route, highlight = prefName)
    }
    fun search(route: String? = null) {
        mutable.update { it.copy(searching = true, searchRoute = route, query = "", highlight = null) }
    }
    fun query(value: String) { mutable.update { it.copy(query = value) } }
    fun closeSearch() { mutable.update { it.copy(searching = false, query = "", searchRoute = null) } }
    fun clearHighlight(key: String) { mutable.update { if (it.highlight == key) it.copy(highlight = null) else it } }
    fun back(): Boolean = when {
        state.value.searching -> { closeSearch(); true }
        state.value.route != null -> { mutable.value = SettingsBrowserState(); true }
        else -> false
    }
}

internal fun settingsVisible(setting: Setting, values: Map<String, SettingValue>, loggedIn: Boolean, debug: Boolean, extensions: Set<String> = emptySet()): Boolean =
    when (val condition = setting.visibleWhen) {
        null -> true
        "logged_in" -> loggedIn
        "debug" -> debug
        else -> if (condition.startsWith("extension:")) condition.removePrefix("extension:") in extensions
        else (values[condition] as? SettingValue.Flag)?.value == true
    }

internal fun visibleSettingsIndex(index: SettingsIndex, values: Map<String, SettingValue>, loggedIn: Boolean, debug: Boolean, extensions: Set<String> = emptySet()) =
    index.categories.mapNotNull { category ->
        val groups = category.groups.mapNotNull { group ->
            group.settings.filter { settingsVisible(it, values, loggedIn, debug, extensions) }.takeIf { it.isNotEmpty() }
                ?.let { group.copy(settings = it) }
        }
        groups.takeIf { it.isNotEmpty() }?.let { category.copy(groups = it) }
    }

internal fun searchSettingsCategories(categories: List<SettingsCategory>, query: String, route: String?): List<SettingsSearchHit> =
    if (categories.isEmpty()) emptyList() else SettingsIndex(categories).search(query, route)
