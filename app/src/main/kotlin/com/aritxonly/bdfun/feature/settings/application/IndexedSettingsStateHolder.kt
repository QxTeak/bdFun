package com.aritxonly.bdfun.feature.settings.application

import com.aritxonly.bdfun.core.domain.SettingsIndex
import com.aritxonly.bdfun.core.domain.SettingsRepository
import com.aritxonly.bdfun.core.model.settings.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

internal data class IndexedSettingsState(
    val values: Map<String, SettingValue> = emptyMap(),
    val query: String = "",
    val route: String? = null,
    val hits: List<SettingsSearchHit> = emptyList(),
)
internal sealed interface SettingUpdate {
    data object Updated : SettingUpdate
    data class Rejected(val reason: String) : SettingUpdate
}

internal class IndexedSettingsStateHolder(
    val index: SettingsIndex,
    repository: SettingsRepository,
    changes: StateFlow<SettingsUiState>,
    scope: CoroutineScope,
) {
    private val bindings = indexedSettingBindings(repository)
    private val stored = index.settings.filterValues { it.storage == "settings" }
    private val mutable = MutableStateFlow(IndexedSettingsState())
    val state = mutable.asStateFlow()

    init {
        require(stored.keys.all { it in bindings }) { "索引包含未注册的存储键" }
        refresh()
        scope.launch { changes.collect { refresh() } }
    }

    fun search(query: String, route: String? = null) {
        mutable.update { it.copy(query = query, route = route, hits = index.search(query, route)) }
    }

    fun update(prefName: String, value: SettingValue): SettingUpdate {
        val setting = index.settings[prefName] ?: return SettingUpdate.Rejected("未知设置")
        if (setting.storage != "settings") return SettingUpdate.Rejected("该组件由独立宿主处理")
        if (!index.accepts(setting, value)) return SettingUpdate.Rejected("值与设置类型或选项不匹配")
        bindings.getValue(prefName).write(value)
        refresh()
        return SettingUpdate.Updated
    }

    fun reset(prefName: String): SettingUpdate {
        val setting = index.settings[prefName] ?: return SettingUpdate.Rejected("未知设置")
        return update(prefName, setting.default)
    }

    private fun refresh() {
        val snapshot = stored.keys.associateWith { bindings.getValue(it).read() }
        mutable.update { it.copy(values = snapshot) }
    }
}
