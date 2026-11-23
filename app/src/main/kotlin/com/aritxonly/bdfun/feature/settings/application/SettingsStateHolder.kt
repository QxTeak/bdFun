package com.aritxonly.bdfun.feature.settings.application

import com.aritxonly.bdfun.core.domain.*
import kotlinx.coroutines.CoroutineScope

internal class SettingsStateHolder private constructor(private val groups: SettingsGroups) : SettingsRepository,
    AppearanceSettings by groups.appearance,
    PlaybackSettings by groups.playback,
    DanmakuSettings by groups.danmaku,
    MessageSettings by groups.message,
    GeneralSettings by groups.general {
    val state get() = groups.values.state
    override val appearance: AppearanceSettings get() = groups.appearance
    override val playback: PlaybackSettings get() = groups.playback
    override val danmaku: DanmakuSettings get() = groups.danmaku
    override val message: MessageSettings get() = groups.message
    override val general: GeneralSettings get() = groups.general
    fun flush() = groups.floats.flush()

    companion object {
        fun create(prefs: SettingsPreferences, codec: SearchHistoryCodec, scope: CoroutineScope): SettingsStateHolder {
            // 历史版本的脏数据不应该让 App 每次冷启动都崩在首帧之前：迁移失败就跳过。
            runCatching {
                migrateSettings0(prefs)
                migrateSettings1(prefs)
                migrateSettings2(prefs)
            }.onFailure {
                com.aritxonly.bdfun.data.LogCollector.record("SETTINGS", "迁移失败，已跳过：${it.message}")
            }
            val values = SettingsValuesStateHolder()
            val floats = FloatSettingsWriter(prefs, scope)
            return SettingsStateHolder(SettingsGroups(
                values, floats,
                AppearanceSettingsStateHolder(prefs, values, floats, codec),
                PlaybackSettingsStateHolder(prefs, values, floats, codec),
                DanmakuSettingsStateHolder(prefs, values, floats, codec),
                MessageSettingsStateHolder(prefs, values, floats, codec),
                GeneralSettingsStateHolder(prefs, values, floats, codec),
            ))
        }
    }
}

private class SettingsGroups(
    val values: SettingsValuesStateHolder,
    val floats: FloatSettingsWriter,
    val appearance: AppearanceSettingsStateHolder,
    val playback: PlaybackSettingsStateHolder,
    val danmaku: DanmakuSettingsStateHolder,
    val message: MessageSettingsStateHolder,
    val general: GeneralSettingsStateHolder,
)
