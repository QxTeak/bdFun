package com.aritxonly.bdfun.core.model.settings

/** 索引只描述展示与值约束，不承载 Android 动作或存储实现。 */
data class SettingsCategory(val name: String, val route: String, val icon: String?, val groups: List<SettingsGroup>)
data class SettingsGroup(val title: String, val settings: List<Setting>)
data class Setting(
    val prefName: String,
    val type: String,
    val title: String,
    val subtitle: String?,
    val default: SettingValue,
    val options: List<SettingOption> = emptyList(),
)
data class SettingOption(val title: String, val value: SettingValue)
data class SettingsSearchHit(val category: SettingsCategory, val group: SettingsGroup, val setting: Setting)
