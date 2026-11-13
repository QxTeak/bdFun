package com.aritxonly.bdfun.infra.bili.mapper

import org.json.JSONObject
import com.aritxonly.bdfun.core.model.feed.*
import com.aritxonly.bdfun.core.model.video.*
import com.aritxonly.bdfun.infra.bili.biliImage as image
import com.aritxonly.bdfun.infra.bili.biliAvatar as avatar
import com.aritxonly.bdfun.infra.bili.parseBiliCountText as parseCountText
import com.aritxonly.bdfun.infra.bili.parseBiliDurationText as parseDurationText
import com.aritxonly.bdfun.infra.bili.cancellableCatching

internal fun parseFeedLiveRecommendation(liveRcmd: JSONObject?): FeedLiveRecommendation? {
    val liveInfo = liveRcmd?.s("content")?.takeIf { it.isNotBlank() }
        ?.let { cancellableCatching { JSONObject(it).optJSONObject("live_play_info") }.getOrNull() }
    return liveInfo?.let {
        FeedLiveRecommendation(
            roomId = it.l("room_id"), title = it.s("title"), cover = image(it.s("cover"), 720),
            live = it.optInt("live_status") == 1, areaName = it.s("area_name"),
            watched = it.optJSONObject("watched_show")?.s("text_large").orEmpty(),
        )
    }
}
