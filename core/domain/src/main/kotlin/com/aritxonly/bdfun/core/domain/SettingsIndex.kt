package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.model.settings.*

interface SettingsIndexCodec {
    /** 非法索引整体拒绝，不能部分导入后改变用户配置。 */
    fun decode(json: String): SettingsIndexImport
}
sealed interface SettingsIndexImport {
    data class Success(val categories: List<SettingsCategory>) : SettingsIndexImport
    data class Invalid(val reason: String) : SettingsIndexImport
}

class SettingsIndex(val categories: List<SettingsCategory>) {
    val settings = categories.flatMap { it.groups }.flatMap { it.settings }.associateBy { it.prefName }

    init {
        require(categories.isNotEmpty()) { "索引不能为空" }
        require(categories.map { it.route }.distinct().size == categories.size) { "路由重复" }
        val entries = categories.flatMap { it.groups }.flatMap { it.settings }
        require(entries.size == settings.size) { "pref_name 重复" }
        categories.forEach { category ->
            require(category.name.isNotBlank() && category.route.isNotBlank())
            category.groups.forEach { group ->
                require(group.title.isNotBlank())
                group.settings.forEach { setting ->
                    require(setting.prefName.matches(Regex("[a-z][a-z0-9_]*")) && setting.title.isNotBlank())
                    require(setting.options.map { it.value }.distinct().size == setting.options.size)
                    require(accepts(setting, setting.default)) { "默认值与类型或选项不匹配：${setting.prefName}" }
                }
            }
        }
    }

    fun search(query: String, route: String? = null): List<SettingsSearchHit> {
        val words = query.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.isEmpty()) return emptyList()
        return categories.filter { route == null || it.route == route }.flatMap { category ->
            category.groups.flatMap { group ->
                group.settings.filter { setting ->
                    val text = listOf(category.name, group.title, setting.title, setting.subtitle.orEmpty(), setting.prefName)
                        .joinToString(" ")
                    words.all { text.contains(it, ignoreCase = true) }
                }.map { SettingsSearchHit(category, group, it) }
            }
        }
    }

    fun accepts(setting: Setting, value: SettingValue): Boolean = when (setting.type) {
        "boolean" -> value is SettingValue.Flag
        "string" -> value is SettingValue.Text
        "integer" -> value is SettingValue.Number
        "decimal" -> value is SettingValue.Decimal && value.value.isFinite()
        "string_set" -> value is SettingValue.TextSet
        "select" -> setting.options.isNotEmpty() && setting.options.any { it.value == value } &&
            setting.options.all { it.value::class == setting.default::class }
        else -> false
    }
}
