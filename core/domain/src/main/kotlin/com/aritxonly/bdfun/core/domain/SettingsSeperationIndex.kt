package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.model.settings.SettingsSeperation

class SettingsSeperationIndex(val groups: List<SettingsSeperation>, index: SettingsIndex) {
    init {
        require(groups.isNotEmpty() && groups.map { it.id }.distinct().size == groups.size) { "一级分组为空或重复" }
        val routes = groups.flatMap { it.entries }.map { it.route }
        require(routes.distinct().size == routes.size) { "一级入口重复" }
        val expected = index.categories.filter { it.layout != "root_footer" }.map { it.route }.toSet()
        require(routes.toSet() == expected) { "一级入口必须恰好引用全部二级页面，不能包含退出登录" }
        groups.forEach { group ->
            require(group.id.matches(Regex("[a-z][a-z0-9_]*")) && group.title.isNotBlank() && group.entries.isNotEmpty())
            group.entries.forEach { entry ->
                require(entry.icon in Icons) { "未知Miuix图标" }
                require(entry.color.matches(Regex("#[0-9a-fA-F]{6}"))) { "图标底色必须是RGB颜色" }
            }
        }
    }
    companion object { val Icons = setOf("Background", "Play", "Messages", "Home", "ContactsCircle", "Info", "Settings") }
}

interface SettingsSeperationCodec {
    fun decode(json: String, index: SettingsIndex): SettingsSeperationImport
}
sealed interface SettingsSeperationImport {
    data class Success(val index: SettingsSeperationIndex) : SettingsSeperationImport
    data class Invalid(val reason: String) : SettingsSeperationImport
}
