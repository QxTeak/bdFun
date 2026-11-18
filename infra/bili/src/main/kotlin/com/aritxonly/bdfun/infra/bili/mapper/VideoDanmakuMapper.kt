package com.aritxonly.bdfun.infra.bili.mapper

import com.aritxonly.bdfun.core.model.video.*
import com.aritxonly.bdfun.core.model.user.*
import com.aritxonly.bdfun.infra.bili.cancellableCatching
import com.aritxonly.bdfun.infra.bili.biliImage as image
import com.aritxonly.bdfun.infra.bili.biliAvatar as avatar
import com.aritxonly.bdfun.infra.bili.biliAidToBvid as aidToBvid
import com.aritxonly.bdfun.infra.bili.parseBiliCountText as parseCountText
import com.aritxonly.bdfun.infra.bili.parseBiliDurationText as parseDurationText
import org.json.JSONObject
import org.json.JSONArray

import org.xmlpull.v1.XmlPullParser

internal fun mapDanmakuSegments(parsed: List<ByteArray>, ownHash: String = ""): List<DanmakuItem> {
    val result = ArrayList<DanmakuItem>(1024)
    for (bytes in parsed) {
        val elems = cancellableCatching {
            com.android.purebilibili.danmaku.parser.DanmakuProto.parse(bytes)
        }.getOrDefault(emptyList())
        for (elem in elems) {
            val text = elem.content.trim()
            val maxLength = if (elem.mode == 7 || elem.mode == 9) 65_536 else 90
            if (text.isBlank() || text.length > maxLength || elem.mode !in 1..9 || elem.mode == 8) continue
            result += DanmakuItem(
                timeMs = elem.progress.toLong(),
                mode = elem.mode,
                color = elem.color,
                size = if (elem.fontsize > 0) elem.fontsize.toFloat() else 25f,
                text = text,
                id = elem.id,
                likeCount = elem.like,
                count = elem.count,
                userHash = elem.midHash,
                vipColor = elem.colorful == com.android.purebilibili.danmaku.parser.DanmakuProto.DmColorfulTypeVipGradualColor,
                self = ownHash.isNotEmpty() && ownHash.equals(elem.midHash, ignoreCase = true),
            )
        }
    }
    return result.sortedBy { it.timeMs }
}

internal fun parseDanmakuXml(xml: String, ownHash: String = ""): List<DanmakuItem> {
    if (xml.isBlank()) return emptyList()
    val result = ArrayList<DanmakuItem>(512)
    cancellableCatching {
        val parser = org.xmlpull.v1.XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(java.io.StringReader(xml))
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT && result.size < 4000) {
            if (event == XmlPullParser.START_TAG && parser.name == "d") {
                val p = parser.getAttributeValue(null, "p").orEmpty()
                val text = parser.nextText().orEmpty().trim()
                val parts = p.split(',')
                if (parts.size >= 4 && text.isNotBlank() && text.length <= if (modeForLength(parts)) 65_536 else 90) {
                    val seconds = parts[0].toFloatOrNull() ?: 0f
                    val mode = parts[1].toIntOrNull() ?: 1
                    if (mode == 1 || mode == 4 || mode == 5 || mode == 7 || mode == 9) {
                        result += DanmakuItem(
                            timeMs = (seconds * 1000).toLong(),
                            mode = mode,
                            color = parts[3].toIntOrNull() ?: 0xFFFFFF,
                            size = parts.getOrNull(2)?.toFloatOrNull() ?: 25f,
                            text = text,
                            id = parts.getOrNull(7)?.toLongOrNull() ?: 0L,
                            userHash = parts.getOrNull(6).orEmpty(),
                            self = ownHash.isNotEmpty() && ownHash.equals(parts.getOrNull(6), ignoreCase = true),
                        )
                    }
                }
            }
            event = parser.next()
        }
    }
    return result.sortedBy { it.timeMs }
}

private fun modeForLength(parts: List<String>): Boolean = parts.getOrNull(1) in setOf("7", "9")
