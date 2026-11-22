package com.aritxonly.bdfun.infra.bili.mapper

import com.aritxonly.bdfun.core.domain.LiveMessageDecoder
import com.aritxonly.bdfun.core.model.live.LiveChatKind
import com.aritxonly.bdfun.core.model.live.LiveChatMessage

import org.json.JSONObject

/**
 * 直播弹幕 JSON → 界面消息。
 *
 * 字段来源参考 BiliPai 的 LiveRealtimeMessagePolicy，只保留详情页需要的四类。
 */
class BiliLiveMessageDecoder : LiveMessageDecoder {

    override fun decode(raw: String, id: Long): LiveChatMessage? {
        val json = runCatching { JSONObject(raw) }.getOrNull() ?: return null
        val cmd = json.optString("cmd").substringBefore(':')
        return when (cmd) {
            "DANMU_MSG" -> parseDanmaku(json, id)
            "SEND_GIFT", "COMBO_SEND" -> parseGift(json, id)
            "SUPER_CHAT_MESSAGE" -> parseSuperChat(json, id)
            "INTERACT_WORD" -> parseEnter(json, id)
            else -> null
        }
    }

    private fun parseDanmaku(json: JSONObject, id: Long): LiveChatMessage? {
        val info = json.optJSONArray("info") ?: return null
        // info[1] 是弹幕正文（字符串），info[2] 是用户数组，info[0] 是样式数组
        val text = info.optString(1)
        if (text.isBlank()) return null
        val user = info.optJSONArray(2)
        val uname = user?.optString(1).orEmpty()
        val meta = info.optJSONArray(0)
        val color = meta?.optInt(3, 0) ?: 0

        // 整条消息就是一张大表情
        val directEmote = meta?.optJSONObject(13)?.optString("url").orEmpty()

        // 文内表情：extra.emots 里是「token → 图片信息」
        val emotes = mutableMapOf<String, String>()
        val emoteHeights = mutableMapOf<String, Int>()
        val extra = meta?.optJSONObject(15)?.optString("extra").orEmpty()
        if (extra.isNotBlank()) {
            val emots = runCatching { JSONObject(extra).optJSONObject("emots") }.getOrNull()
            if (emots != null) {
                emots.keys().forEach { key ->
                    val item = emots.optJSONObject(key) ?: return@forEach
                    val url = item.optString("url")
                    if (url.isNotBlank()) {
                        emotes[key] = url
                        val h = item.optInt("height", 0)
                        if (h > 0) emoteHeights[key] = (h / 2).coerceIn(14, 40)
                    }
                }
            }
        }

        // 粉丝牌：新版协议在 info[0][15].user.medal，老版协议在 info[3] 数组。
        val medal = parseMedal(meta, info)

        return LiveChatMessage(
            id = id,
            kind = LiveChatKind.DANMAKU,
            uname = uname,
            text = text,
            medalName = medal?.name,
            medalLevel = medal?.level ?: 0,
            medalColor = medal?.color ?: 0,
            nameColor = if (color > 0) color.toLong() else null,
            emoteUrl = directEmote.ifBlank { null },
            emotes = emotes,
            emoteHeights = emoteHeights,
        )
    }

    private data class LiveMedal(val name: String, val level: Int, val color: Int)

    /**
     * 参考 PiliPlus / BiliPai 的直播弹幕解析：新协议 `info[0][15].user.medal`
     * （name / level / v2_medal_color_start，只显示已点亮的牌子），
     * 旧协议回退到 `info[3]` 数组（level / name / color 在 [0]/[1]/[4]）。
     */
    private fun parseMedal(meta: org.json.JSONArray?, info: org.json.JSONArray): LiveMedal? {
        val modern = meta?.optJSONObject(15)
            ?.optJSONObject("user")
            ?.optJSONObject("medal")
        if (modern != null) {
            // is_light=1 才是已点亮的牌子；灰牌和老协议才走下面的数组。
            if (modern.optInt("is_light", 0) != 1) return null
            val name = modern.optString("name").ifBlank { modern.optString("medal_name") }
            val level = modern.optInt("level", 0).takeIf { it > 0 }
                ?: modern.optInt("medal_level", 0)
            if (name.isBlank() || level <= 0) return null
            return LiveMedal(name, level, parseMedalHexColor(modern.optString("v2_medal_color_start")))
        }
        val legacy = info.optJSONArray(3) ?: return null
        if (legacy.length() < 2) return null
        val name = legacy.optString(1)
        val level = legacy.optInt(0, 0)
        if (name.isBlank() || level <= 0) return null
        return LiveMedal(name, level, legacy.optInt(4, 0))
    }

    /** `#RRGGBB` / `#RRGGBBAA` → 0xRRGGBB；解析不了返回 0（界面用主题粉）。 */
    private fun parseMedalHexColor(raw: String): Int {
        if (!raw.startsWith("#")) return raw.toIntOrNull() ?: 0
        val hex = raw.removePrefix("#")
        val value = hex.toLongOrNull(16) ?: return 0
        return when (hex.length) {
            6 -> (value or 0xFF000000L).toInt()
            // 8 位是 RGBA，换成 ARGB。
            8 -> ((value ushr 8) or ((value and 0xFFL) shl 24)).toInt()
            else -> 0
        }
    }

    private fun parseGift(json: JSONObject, id: Long): LiveChatMessage? {
        val data = json.optJSONObject("data") ?: return null
        val uname = data.optString("uname")
        val gift = data.optString("giftName")
        val num = data.optInt("num", 1)
        if (uname.isBlank() || gift.isBlank()) return null
        return LiveChatMessage(
            id = id,
            kind = LiveChatKind.GIFT,
            uname = uname,
            text = "赠送 $gift x$num",
        )
    }

    private fun parseSuperChat(json: JSONObject, id: Long): LiveChatMessage? {
        val data = json.optJSONObject("data") ?: return null
        val uname = data.optJSONObject("user_info")?.optString("uname").orEmpty()
        val message = data.optString("message")
        val price = data.optInt("price", 0)
        if (uname.isBlank() || message.isBlank()) return null
        return LiveChatMessage(
            id = id,
            kind = LiveChatKind.SUPER_CHAT,
            uname = uname,
            text = message,
            amountText = if (price > 0) "￥$price" else null,
        )
    }

    private fun parseEnter(json: JSONObject, id: Long): LiveChatMessage? {
        val data = json.optJSONObject("data") ?: return null
        val uname = data.optString("uname")
        if (uname.isBlank()) return null
        return LiveChatMessage(
            id = id,
            kind = LiveChatKind.SYSTEM,
            text = "欢迎 $uname 进入直播间",
        )
    }
}
