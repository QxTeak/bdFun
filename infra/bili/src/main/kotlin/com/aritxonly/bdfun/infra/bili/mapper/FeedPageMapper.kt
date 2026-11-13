package com.aritxonly.bdfun.infra.bili.mapper

import com.aritxonly.bdfun.core.model.user.*
import com.aritxonly.bdfun.core.model.video.*
import com.aritxonly.bdfun.core.model.feed.*
import com.aritxonly.bdfun.infra.bili.biliImage as image
import com.aritxonly.bdfun.infra.bili.biliAvatar as avatar
import com.aritxonly.bdfun.infra.bili.parseBiliDurationText as parseDurationText
import org.json.JSONObject

internal fun mapFeedPage(data: JSONObject): FeedPage =
    FeedPage(
        items = data.optJSONArray("items").objects().mapNotNull { parseFeedItem(it) },
        offset = data.s("offset"),
        hasMore = data.optBoolean("has_more", false),
    )

internal fun mapFeedPortal(data: JSONObject): FeedPortal {
    val live = data.optJSONObject("live_users")
    return FeedPortal(
        upList = parseFeedUpPage(data.optJSONObject("up_list")),
        liveUsers = live?.optJSONArray("items").objects().mapNotNull {
            val roomId = it.l("room_id")
            if (roomId <= 0) null else FeedLiveUser(
                mid = it.l("mid"), name = it.s("uname"), face = avatar(it.s("face"), 120),
                roomId = roomId, title = it.s("title"),
            )
        },
        liveCount = live?.optInt("count", 0) ?: 0,
    )
}

internal fun parseFeedUpPage(data: JSONObject?): FeedUpPage = FeedUpPage(
    items = data?.optJSONArray("items").objects().mapNotNull {
        val mid = it.l("mid")
        if (mid <= 0) null else FeedAuthor(mid, it.s("uname"), avatar(it.s("face"), 120), it.optBoolean("has_update"))
    }.distinctBy { it.mid },
    offset = data?.s("offset").orEmpty(),
    hasMore = data?.optBoolean("has_more", false) ?: false,
)

internal fun mapFeedDetail(data: JSONObject): FeedItem? =
    data.optJSONObject("item")?.let { parseFeedItem(it) }
