package com.aritxonly.bdfun.infra.storage

import com.aritxonly.bdfun.core.domain.*
import com.aritxonly.bdfun.core.model.settings.*
import org.json.JSONArray
import org.json.JSONObject

class JsonSettingsIndexCodec : SettingsIndexCodec {
    override fun decode(json: String): SettingsIndexImport = try {
        require(json.length <= 1_048_576) { "索引超过 1MiB" }
        val categories = JSONArray(json).objects().map { category ->
            SettingsCategory(category.requiredText("name"), category.requiredText("route"),
                category.optionalText("icon"), category.getJSONArray("groups").objects().map { group ->
                    SettingsGroup(group.requiredText("title"), group.getJSONArray("settings").objects().map(::setting))
                })
        }
        SettingsIndex(categories)
        SettingsIndexImport.Success(categories)
    } catch (error: IllegalArgumentException) {
        SettingsIndexImport.Invalid(error.message ?: "索引不合法")
    } catch (error: org.json.JSONException) {
        SettingsIndexImport.Invalid(error.message ?: "JSON 不合法")
    }

    private fun setting(json: JSONObject): Setting = Setting(
        json.requiredText("pref_name"), json.requiredText("type"), json.requiredText("title"),
        json.optionalText("subtitle"), value(json.get("default")),
        if (json.has("options")) json.getJSONArray("options").objects().map {
            SettingOption(it.requiredText("title"), value(it.get("value")))
        } else emptyList(),
    )

    private fun value(raw: Any): SettingValue = when (raw) {
        is Boolean -> SettingValue.Flag(raw)
        is Int -> SettingValue.Number(raw)
        is Long -> { require(raw in Int.MIN_VALUE..Int.MAX_VALUE); SettingValue.Number(raw.toInt()) }
        is Number -> SettingValue.Decimal(raw.toFloat().also { require(it.isFinite()) })
        is String -> SettingValue.Text(raw)
        is JSONArray -> SettingValue.TextSet((0 until raw.length()).map {
            (raw.get(it) as? String) ?: throw IllegalArgumentException("集合仅允许字符串")
        }.toSet())
        else -> throw IllegalArgumentException("不支持的设置值")
    }

    private fun JSONArray.objects() = (0 until length()).map { getJSONObject(it) }
    private fun JSONObject.requiredText(key: String): String =
        (get(key) as? String)?.also { require(it.isNotBlank()) } ?: throw IllegalArgumentException("$key 必须为字符串")
    private fun JSONObject.optionalText(key: String): String? =
        if (!has(key) || isNull(key)) null else (get(key) as? String
            ?: throw IllegalArgumentException("$key 必须为字符串"))
}
