package com.aritxonly.bdfun.infra.bili.mapper

import com.aritxonly.bdfun.core.model.video.*
import com.aritxonly.bdfun.core.model.library.*
import com.aritxonly.bdfun.core.model.user.*
import com.aritxonly.bdfun.core.domain.relationNextPage
import com.aritxonly.bdfun.core.domain.resolveCloudResumePositionMs
import com.aritxonly.bdfun.infra.bili.biliImage as image
import com.aritxonly.bdfun.infra.bili.biliAvatar as avatar
import com.aritxonly.bdfun.infra.bili.parseBiliDurationText as parseDurationText
import org.json.JSONObject

internal fun mapHistoryPage(data: JSONObject): HistoryPage {
    val items = data.optJSONArray("list").objects().mapNotNull { item ->
        val history = item.optJSONObject("history") ?: return@mapNotNull null
        val bvid = history.s("bvid")
        if (bvid.isBlank()) return@mapNotNull null
        val stat = item.optJSONObject("stat") ?: item.optJSONObject("videos")
        HistoryItem(
            bvid = bvid,
            title = item.s("title"),
            cover = image(item.s("cover"), 420),
            author = item.s("author_name"),
            viewedAt = item.l("view_at"),
            progress = item.optInt("progress", 0),
            duration = item.optInt("duration", 0),
            play = stat?.optInt("view", 0) ?: 0,
            danmaku = stat?.optInt("danmaku", 0) ?: 0,
            aid = history.l("oid"),
            cid = history.l("cid").coerceAtLeast(0L),
            business = history.s("business"),
        )
    }
    val cursor = data.optJSONObject("cursor")
    val cursorHasMore = cursor?.optBoolean("has_more", false) == true ||
        cursor?.optInt("has_more", 0) == 1 ||
        data.optBoolean("has_more", false)
    return HistoryPage(
        items = items,
        max = cursor?.l("max") ?: 0L,
        viewAt = cursor?.l("view_at") ?: items.lastOrNull()?.viewedAt ?: 0L,
        business = cursor?.s("business").orEmpty(),
        hasMore = cursorHasMore || items.size >= 30,
    )
}

internal fun describeHistoryCursor(data: JSONObject): String {
    val cursor = data.optJSONObject("cursor")
    return "cursorMax=${cursor?.l("max")} cursorViewAt=${cursor?.l("view_at")} " +
        "cursorHasMore=${cursor?.opt("has_more")}"
}

internal fun mapLibraryVideoStats(root: JSONObject): Pair<Long, Long>? {
    val stat = root.optJSONObject("data")?.optJSONObject("stat") ?: return null
    return stat.l("view") to stat.l("danmaku")
}

internal fun mapWatchLater(data: JSONObject): List<HistoryItem> =
    data.optJSONArray("list").objects().mapNotNull { item ->
        val bvid = item.s("bvid")
        if (bvid.isBlank()) return@mapNotNull null
        val stat = item.optJSONObject("stat")
        HistoryItem(
            bvid = bvid,
            title = item.s("title"),
            cover = image(item.s("pic"), 420),
            author = item.optJSONObject("owner")?.s("name").orEmpty(),
            viewedAt = item.l("add_at"),
            progress = item.optInt("progress", 0),
            duration = item.optInt("duration", 0),
            play = stat?.optInt("view", 0) ?: 0,
            danmaku = stat?.optInt("danmaku", 0) ?: 0,
            aid = item.l("aid"),
            cid = item.l("cid").coerceAtLeast(0L),
        )
    }

internal fun mapCloudResumePositionMs(data: JSONObject, cid: Long, durationSec: Int): Long =
    resolveCloudResumePositionMs(data.l("last_play_cid"), cid, data.l("last_play_time"), durationSec)
