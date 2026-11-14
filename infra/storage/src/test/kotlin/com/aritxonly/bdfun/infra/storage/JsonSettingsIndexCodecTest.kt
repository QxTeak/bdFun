package com.aritxonly.bdfun.infra.storage

import com.aritxonly.bdfun.core.domain.*
import com.aritxonly.bdfun.core.model.settings.*
import org.junit.Assert.*
import org.junit.Test

class JsonSettingsIndexCodecTest {
    private val codec = JsonSettingsIndexCodec()
    private fun json(setting: String) = """[{"name":"播放","route":"settings/playback","groups":[{"title":"声音","settings":[$setting]}]}]"""
    private val flag = """{"pref_name":"background_play","type":"boolean","title":"后台播放","subtitle":"允许离开视频后播放","default":true}"""

    @Test fun preservesStoredKeyAndDefault() {
        val result = codec.decode(json(flag)) as SettingsIndexImport.Success
        val entry = result.categories.single().groups.single().settings.single()
        assertEquals("background_play", entry.prefName)
        assertEquals(SettingValue.Flag(true), entry.default)
    }

    @Test fun rejectsInvalidIndexAtomically() {
        listOf(json("$flag,$flag"), json(flag.replace("true", "1")),
            json(flag.replace("boolean", "unknown")), json(flag.replace("true", "null")),
            "[]", "not JSON", json(flag.replace("后台播放", ""))).forEach {
            assertTrue(it, codec.decode(it) is SettingsIndexImport.Invalid)
        }
    }

    @Test fun selectRequiresActualValueNotPosition() {
        val raw = """{"pref_name":"quality","type":"select","title":"清晰度","default":64,"options":[{"title":"720P","value":64},{"title":"1080P","value":80}]}"""
        val result = codec.decode(json(raw)) as SettingsIndexImport.Success
        val index = SettingsIndex(result.categories)
        val setting = index.settings.getValue("quality")
        assertTrue(index.accepts(setting, SettingValue.Number(80)))
        assertFalse(index.accepts(setting, SettingValue.Number(1)))
        assertTrue(codec.decode(json(raw.replace("\"default\":64", "\"default\":32"))) is SettingsIndexImport.Invalid)
    }

    @Test fun searchesAllWordsAndScopesByCategory() {
        val index = SettingsIndex((codec.decode(json(flag)) as SettingsIndexImport.Success).categories)
        assertEquals(1, index.search("  声音  后台 ").size)
        assertEquals(1, index.search("BACKGROUND_PLAY").size)
        assertEquals(0, index.search("后台", "settings/appearance").size)
        assertEquals(0, index.search(" ").size)
        assertEquals(0, index.search("不存在").size)
    }

    @Test fun keepsComponentAndVisibilityMetadata() {
        val raw = """{"pref_name":"font_scale","type":"font_scale","title":"全局字号","default":1.0}"""
        val result = codec.decode(json(raw)) as SettingsIndexImport.Success
        assertEquals("font_scale", result.categories.single().groups.single().settings.single().type)
        val action = """{"pref_name":"logs","type":"action","storage":"none","visible_when":"debug","title":"日志抓取","default":""}"""
        val entry = (codec.decode(json(action)) as SettingsIndexImport.Success).categories.single().groups.single().settings.single()
        assertEquals("none", entry.storage)
        assertEquals("debug", entry.visibleWhen)
    }

    @Test fun numericFormattingDoesNotChangeComponentValueTypes() {
        val raw = """{"pref_name":"speed","type":"dropdown","value_type":"decimal","title":"倍速","default":1,"options":[{"title":"1×","value":1},{"title":"1.5×","value":1.5}]}"""
        val result = codec.decode(json(raw)) as SettingsIndexImport.Success
        val setting = result.categories.single().groups.single().settings.single()
        assertEquals(SettingValue.Decimal(1f), setting.default)
        assertEquals(SettingValue.Decimal(1.5f), setting.options[1].value)
        val file = java.io.File("../../app/src/main/assets/settings-index.json")
        val original = codec.decode(file.readText()) as SettingsIndexImport.Success
        val serialized = codec.decode(org.json.JSONArray(file.readText()).toString()) as SettingsIndexImport.Success
        assertEquals(original.categories, serialized.categories)
    }

    @Test fun bundledIndexIsValid() {
        val file = java.io.File("../../app/src/main/assets/settings-index.json")
        val result = codec.decode(file.readText())
        assertTrue(result.toString(), result is SettingsIndexImport.Success)
        assertEquals(61, SettingsIndex((result as SettingsIndexImport.Success).categories).settings.size)
    }
}
