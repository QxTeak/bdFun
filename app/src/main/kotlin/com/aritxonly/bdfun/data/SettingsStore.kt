package com.aritxonly.bdfun.data

import android.content.Context
import com.aritxonly.bdfun.feature.settings.application.SettingsStateHolder
import com.aritxonly.bdfun.feature.settings.presentation.SettingsBindings
import com.aritxonly.bdfun.infra.storage.AndroidSettingsPreferences
import com.aritxonly.bdfun.infra.storage.JsonSearchHistoryCodec
import kotlinx.coroutines.*

/** 旧构造入口仅负责 Android 存储装配，界面使用 SettingsBindings。 */
class SettingsStore private constructor(context: Context, scope: CoroutineScope) : SettingsBindings(
    scope, SettingsStateHolder.create(AndroidSettingsPreferences(context), JsonSearchHistoryCodec(), scope),
) {
    private val index = loadSettingsIndex(context)
    internal val indexed = com.aritxonly.bdfun.feature.settings.application.IndexedSettingsStateHolder(
        index, this, state, scope, loadSettingsSeperation(context, index),
    )

    constructor(context: Context) : this(context, CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate))
}

private fun loadSettingsIndex(context: Context): com.aritxonly.bdfun.core.domain.SettingsIndex = runCatching {
    val json = context.assets.open("settings-index.json").bufferedReader().use { it.readText() }
    when (val result = com.aritxonly.bdfun.infra.storage.JsonSettingsIndexCodec().decode(json)) {
        is com.aritxonly.bdfun.core.domain.SettingsIndexImport.Success ->
            com.aritxonly.bdfun.core.domain.SettingsIndex(result.categories)
        is com.aritxonly.bdfun.core.domain.SettingsIndexImport.Invalid -> error(result.reason)
    }
}.getOrElse {
    // 设置清单坏了也不能让 App 起不来：退化成空设置页，至少还能用。
    LogCollector.record("SETTINGS", "settings-index 解析失败：${it.message}")
    com.aritxonly.bdfun.core.domain.SettingsIndex(emptyList())
}

private fun loadSettingsSeperation(context: Context, index: com.aritxonly.bdfun.core.domain.SettingsIndex):
    List<com.aritxonly.bdfun.core.model.settings.SettingsSeperation> = runCatching {
    val json = context.assets.open("settings-seperation.json").bufferedReader().use { it.readText() }
    when (val result = com.aritxonly.bdfun.infra.storage.JsonSettingsSeperationCodec().decode(json, index)) {
        is com.aritxonly.bdfun.core.domain.SettingsSeperationImport.Success -> result.index.groups
        is com.aritxonly.bdfun.core.domain.SettingsSeperationImport.Invalid -> error(result.reason)
    }
}.getOrElse {
    LogCollector.record("SETTINGS", "settings-seperation 解析失败：${it.message}")
    emptyList()
}
