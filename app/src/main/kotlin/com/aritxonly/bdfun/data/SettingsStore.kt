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
    internal val indexed = com.aritxonly.bdfun.feature.settings.application.IndexedSettingsStateHolder(
        loadSettingsIndex(context), this, state, scope,
    )

    constructor(context: Context) : this(context, CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate))
}

private fun loadSettingsIndex(context: Context): com.aritxonly.bdfun.core.domain.SettingsIndex {
    val json = context.assets.open("settings-index.json").bufferedReader().use { it.readText() }
    return when (val result = com.aritxonly.bdfun.infra.storage.JsonSettingsIndexCodec().decode(json)) {
        is com.aritxonly.bdfun.core.domain.SettingsIndexImport.Success ->
            com.aritxonly.bdfun.core.domain.SettingsIndex(result.categories)
        is com.aritxonly.bdfun.core.domain.SettingsIndexImport.Invalid -> error(result.reason)
    }
}
