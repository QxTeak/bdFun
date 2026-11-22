package com.aritxonly.bdfun.infra.bili.mapper

import com.aritxonly.bdfun.infra.bili.mapper.readVideoStats
import com.aritxonly.bdfun.core.model.video.VideoItemExtras
import com.aritxonly.bdfun.core.model.video.VideoItem
import com.aritxonly.bdfun.infra.bili.biliAvatar as avatar
import com.aritxonly.bdfun.infra.bili.biliImage as image
import com.aritxonly.bdfun.infra.bili.dto.*
import org.json.JSONObject

internal fun readFeedVideo(item: JSONObject): VideoFeedDto? {
    val bvid = item.s("bvid")
    if (bvid.isBlank()) return null
    val owner = item.optJSONObject("owner")
    return VideoFeedDto(
        bvid, item.l("id").takeIf { it > 0 } ?: item.l("aid"), item.s("title"), item.s("pic"),
        VideoOwnerDto(owner?.s("name").orEmpty(), owner?.s("face").orEmpty(), owner?.l("mid") ?: 0),
        readVideoStats(item.optJSONObject("stat")),
        VideoTimingDto(item.l("pubdate"), item.optInt("duration", 0), item.l("cid")),
        item.s("tname"), item.s("track_id"), item.optJSONObject("rcmd_reason")?.s("content").orEmpty(),
        item.optBoolean("is_followed", false), item.hasChargeBadge(),
    )
}

internal fun mapFeedVideo(dto: VideoFeedDto, cleanHtml: (String) -> String): VideoItem = VideoItem(
    bvid = dto.bvid,
    aid = dto.aid,
    title = cleanHtml(dto.title),
    cover = image(dto.cover, 480),
    ownerName = dto.owner.name,
    ownerFace = avatar(dto.owner.face, 96),
    ownerMid = dto.owner.mid,
    play = dto.stats.play,
    danmaku = dto.stats.danmaku,
    duration = dto.timing.duration,
    pubdate = dto.timing.publishedAt,
    extras = VideoItemExtras(
        cid = dto.timing.cid,
        categoryName = dto.category,
        recommendationTrackId = dto.trackId,
        reason = dto.reason,
        isFollowed = dto.followed,
        isCharge = dto.isCharge,
    ),
)
