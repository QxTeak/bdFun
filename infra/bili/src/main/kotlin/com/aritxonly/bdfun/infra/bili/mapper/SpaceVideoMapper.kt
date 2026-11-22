package com.aritxonly.bdfun.infra.bili.mapper

import com.aritxonly.bdfun.core.model.user.*
import com.aritxonly.bdfun.core.model.video.*
import com.aritxonly.bdfun.core.model.feed.*
import com.aritxonly.bdfun.infra.bili.biliImage as image
import com.aritxonly.bdfun.infra.bili.biliAvatar as avatar
import com.aritxonly.bdfun.infra.bili.parseBiliDurationText as parseDurationText
import org.json.JSONObject

internal fun mapSpaceVideo(item: JSONObject, cleanHtml: (String) -> String): VideoItem? {
    val bvid = item.s("bvid")
    if (bvid.isBlank()) return null
    return VideoItem(
        bvid = bvid,
        aid = item.l("aid"),
        title = cleanHtml(item.s("title")),
        cover = image(item.s("pic"), 480),
        ownerName = item.s("author"),
        ownerFace = "",
        ownerMid = item.l("mid"),
        play = item.l("play"),
        danmaku = item.l("video_review"),
        duration = parseDurationText(item.s("length")),
        pubdate = item.l("created"),
        extras = VideoItemExtras(
            cid = item.l("cid"),
            isCharge = item.hasChargeBadge(),
        ),
    )
}


internal fun mapSpaceVideos(data: JSONObject, parseVideo: (JSONObject) -> VideoItem?): List<VideoItem> =
    data.optJSONObject("list")?.optJSONArray("vlist").objects().mapNotNull(parseVideo)
