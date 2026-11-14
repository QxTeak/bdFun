package com.aritxonly.bdfun.core.model.settings

/** 一级入口索引不包含设置值，route引用二级索引。命名沿用资产协议seperation。 */
data class SettingsSeperation(val id: String, val title: String, val entries: List<SettingsEntry>)
data class SettingsEntry(val route: String, val icon: String, val color: String)
