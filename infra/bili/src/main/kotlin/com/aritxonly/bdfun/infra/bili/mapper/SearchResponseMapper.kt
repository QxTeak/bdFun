package com.aritxonly.bdfun.infra.bili.mapper

import com.aritxonly.bdfun.core.model.pgc.SearchPgcItem
import com.aritxonly.bdfun.core.model.video.*
import com.aritxonly.bdfun.core.model.library.*
import com.aritxonly.bdfun.core.model.user.*
import com.aritxonly.bdfun.core.domain.relationNextPage
import com.aritxonly.bdfun.infra.bili.biliImage as image
import com.aritxonly.bdfun.infra.bili.biliAvatar as avatar
import com.aritxonly.bdfun.infra.bili.parseBiliDurationText as parseDurationText
import org.json.JSONObject

internal fun mapSearchSuggestions(root: JSONObject, cleanHtml: (String) -> String): List<String> =
    root.optJSONObject("result")
        ?.optJSONArray("tag")
        .objects()
        .mapNotNull { item ->
            item.s("term")
                .ifBlank { cleanHtml(item.s("name")) }
                .takeIf { it.isNotBlank() }
        }
        .distinct()
        .take(8)

internal fun mapSearchDiscovery(data: JSONObject): List<String> {
    val list = data.optJSONObject("trending")?.optJSONArray("list")
    val out = ArrayList<String>()
    if (list != null) {
        for (i in 0 until list.length()) {
            val word = list.optJSONObject(i)?.optString("keyword").orEmpty()
            if (word.isNotBlank()) out.add(word)
        }
    }
    return out
}

internal fun mapHotwordSearch(root: JSONObject): List<String> {
    val array = root.optJSONArray("data") ?: return emptyList<String>()
    val result = ArrayList<String>(array.length())
    for (i in 0 until array.length()) {
        val item = array.optJSONObject(i) ?: continue
        val word = item.optString("keyword").ifBlank { item.optString("name") }
        if (word.isNotBlank()) result.add(word)
    }
    return result
}

internal fun mapSearchVideos(data: JSONObject, cleanHtml: (String) -> String): List<VideoItem> =
    data.optJSONArray("result").objects().mapNotNull { item ->
        val bvid = item.s("bvid")
        if (bvid.isBlank()) return@mapNotNull null
        VideoItem(
            bvid = bvid,
            aid = item.l("aid"),
            title = cleanHtml(item.s("title")),
            cover = image(item.s("pic"), 420),
            ownerName = cleanHtml(item.s("author")),
            ownerFace = "",
            ownerMid = item.l("mid"),
            play = item.s("play").toLongOrNull() ?: 0,
            danmaku = item.s("danmaku").toLongOrNull() ?: 0,
            duration = parseDurationText(item.s("duration")),
            pubdate = item.l("pubdate"),
        )
    }

internal fun mapSearchUsers(data: JSONObject, cleanHtml: (String) -> String): List<SearchUser> =
    data.optJSONArray("result").objects().mapNotNull { item ->
        val mid = item.l("mid")
        if (mid <= 0L) return@mapNotNull null
        SearchUser(
            mid = mid,
            name = cleanHtml(item.s("uname")),
            // 头像必须按 1:1 裁切；沿用视频封面的 16:9 @..._1c 会把头像上下切掉。
            face = avatar(item.s("upic"), 160),
            sign = cleanHtml(item.s("usign")),
            fans = item.s("fans").toLongOrNull() ?: 0,
            videos = item.s("videos").toLongOrNull() ?: 0,
            level = item.optInt("level", 0),
        )
    }

/** 番剧/影视搜索：`data.result[]` 是 PGC 卡片，不是 VideoItem。 */
internal fun mapSearchPgc(data: JSONObject, cleanHtml: (String) -> String): List<SearchPgcItem> =
    data.optJSONArray("result").objects().mapNotNull { item ->
        val seasonId = item.l("season_id")
        if (seasonId <= 0L) return@mapNotNull null
        val score = item.optJSONObject("media_score")?.optString("score").orEmpty()
        SearchPgcItem(
            seasonId = seasonId,
            title = cleanHtml(item.s("title")),
            cover = image(item.s("cover"), 360),
            typeName = item.s("season_type_name"),
            areas = item.s("areas"),
            styles = item.s("styles"),
            indexShow = item.s("index_show"),
            score = score,
            pubdate = item.l("pubtime"),
            desc = cleanHtml(item.s("desc")),
        )
    }

internal fun mapHotSearch(data: JSONObject): List<String> =
    data.optJSONObject("trending")?.optJSONArray("list").objects()
        ?.mapNotNull { it.s("keyword").takeIf(String::isNotBlank) }
        .orEmpty()
