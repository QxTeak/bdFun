package com.aritxonly.bdfun.infra.storage

import com.aritxonly.bdfun.core.domain.*
import com.aritxonly.bdfun.core.model.settings.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class JsonSettingsSeperationCodec : SettingsSeperationCodec {
    override fun decode(json: String, index: SettingsIndex): SettingsSeperationImport = try {
        require(json.length <= 1_048_576) { "索引超过1MiB" }
        val array = JSONArray(json)
        val groups = (0 until array.length()).map { at ->
            val group = array.getJSONObject(at)
            val entries = group.getJSONArray("entries")
            SettingsSeperation(group.text("id"), group.text("title"), (0 until entries.length()).map { row ->
                val entry = entries.getJSONObject(row)
                SettingsEntry(entry.text("route"), entry.text("icon"), entry.text("color"))
            })
        }
        SettingsSeperationImport.Success(SettingsSeperationIndex(groups, index))
    } catch (error: IllegalArgumentException) {
        SettingsSeperationImport.Invalid(error.message ?: "一级索引不合法")
    } catch (error: JSONException) {
        SettingsSeperationImport.Invalid(error.message ?: "一级JSON不合法")
    }
    private fun JSONObject.text(key: String): String =
        get(key) as? String ?: throw IllegalArgumentException("$key 必须是字符串")
}
