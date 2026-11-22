package com.aritxonly.bdfun.infra.bili.mapper

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 直播弹幕粉丝牌：字段路径按 PiliPlus / BiliPai 的实现对齐。
 * 结构探针只验证解析分支，不代表真实抓包时间点。
 */
class BiliLiveMessageDecoderMedalTest {
    private fun meta(medal: JSONObject?): JSONArray = JSONArray().apply {
        for (index in 0..14) put(JSONObject.NULL)
        val content = JSONObject().put("extra", "{}")
        if (medal != null) {
            content.put("user", JSONObject().put("uid", 1)
                .put("base", JSONObject().put("name", "阿"))
                .put("medal", medal))
        }
        put(content)
    }

    private fun decode(meta: JSONArray, legacyMedal: JSONArray? = null): String {
        val info = JSONArray().put(meta).put("hello")
            .put(JSONArray().put(1).put("阿"))
        if (legacyMedal != null) {
            while (info.length() < 3) info.put(JSONObject.NULL)
            info.put(legacyMedal)
        }
        val raw = JSONObject().put("cmd", "DANMU_MSG").put("info", info).toString()
        val message = BiliLiveMessageDecoder().decode(raw, 1)!!
        return "${message.medalName}|${message.medalLevel}|${message.medalColor}"
    }

    @Test fun modernMedalUsesUserMedalAndHexColor() {
        val medal = JSONObject().put("name", "小闪电").put("level", 21)
            .put("is_light", 1).put("v2_medal_color_start", "#fb7299")
        assertEquals("小闪电|21|${0xFFFB7299.toInt()}", decode(meta(medal)))
    }

    @Test fun unlitMedalIsIgnored() {
        val medal = JSONObject().put("name", "小闪电").put("level", 21)
            .put("is_light", 0).put("v2_medal_color_start", "#fb7299")
        assertEquals("null|0|0", decode(meta(medal)))
    }

    @Test fun legacyMedalFallsBackToInfoArray() {
        // 没有现代 user 对象时，回退到 info[3] = [level, name, anchor, roomId, color]
        val legacy = JSONArray().put(18).put("老牌子").put("主播").put(1).put(0x66CCFF)
        assertEquals("老牌子|18|${0x66CCFF}", decode(meta(null), legacy))
    }

    @Test fun missingMedalLeavesBadgeEmpty() {
        assertEquals("null|0|0", decode(meta(null)))
        assertNull(BiliLiveMessageDecoder().decode("""{"cmd":"DANMU_MSG","info":[]}""", 2))
    }
}
