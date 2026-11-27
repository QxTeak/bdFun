package com.aritxonly.bdfun.infra.bili.mapper

import com.aritxonly.bdfun.core.model.video.VideoDetailOwner
import com.aritxonly.bdfun.core.model.video.VideoDetailInteraction
import com.aritxonly.bdfun.core.model.video.VideoDetailInfo
import com.aritxonly.bdfun.core.model.video.VideoDetailCounts
import com.aritxonly.bdfun.core.model.video.*
import com.aritxonly.bdfun.infra.bili.biliAvatar as avatar
import com.aritxonly.bdfun.infra.bili.biliImage as image
import com.aritxonly.bdfun.infra.bili.dto.*
import org.json.JSONObject

internal fun readVideoDetail(data: JSONObject): VideoDetailDto {
    val owner = data.optJSONObject("owner")
    val req = data.optJSONObject("req_user")
    return VideoDetailDto(
        bvid = data.s("bvid"), aid = data.l("aid"), title = data.s("title"),
        cover = data.s("pic"), description = data.s("desc"),
        owner = VideoOwnerDto(owner?.s("name").orEmpty(), owner?.s("face").orEmpty(), owner?.l("mid") ?: 0),
        stats = readVideoStats(data.optJSONObject("stat")),
        timing = VideoTimingDto(data.l("pubdate"), data.optInt("duration", 0), data.l("cid")),
        category = data.s("tname"), collection = readCollection(data),
        copyright = data.optInt("copyright", 0),
        interaction = VideoInteractionDto(
            req?.optInt("like", 0) == 1,
            (req?.optInt("coin", 0) ?: 0) > 0,
            (req?.optInt("favorite", 0) ?: 0) > 0,
        ),
    )
}

internal fun mapVideoDetail(dto: VideoDetailDto, cleanHtml: (String) -> String): VideoDetail = VideoDetail(
    bvid = dto.bvid,
    aid = dto.aid,
    title = cleanHtml(dto.title),
    cover = image(dto.cover, 1200),
    desc = dto.description,
    owner = VideoDetailOwner(
        ownerName = dto.owner.name,
        ownerFace = avatar(dto.owner.face, 160),
        ownerMid = dto.owner.mid,
        ownerSign = "",
    ),
    counts = VideoDetailCounts(
        play = dto.stats.play,
        danmaku = dto.stats.danmaku,
        like = dto.stats.like,
        coin = dto.stats.coin,
        favorite = dto.stats.favorite,
        share = dto.stats.share,
        reply = dto.stats.reply,
    ),
    info = VideoDetailInfo(
        pubdate = dto.timing.publishedAt,
        duration = dto.timing.duration,
        cid = dto.timing.cid,
        tname = dto.category,
        pages = dto.collection.pages.map {
            // 每 P 的首帧地址由接口 pages[].first_frame 提供；换了清晰度/尺寸参数，和其它封面一样走 image()。
            VideoPage(
                cid = it.cid, page = it.page, part = it.part, duration = it.duration,
                firstFrame = it.firstFrame.takeIf(String::isNotBlank)?.let { url -> image(url, 480) }.orEmpty(),
            )
        },
        episodes = dto.collection.episodes.map {
        VideoEpisode(it.bvid, it.aid, it.cid, it.title, image(it.cover, 480), it.duration,
            it.play, it.danmaku, it.publishedAt)
    },
        seasonTitle = dto.collection.title,
        copyright = dto.copyright,
    ),
    interaction = VideoDetailInteraction(
        liked = dto.interaction.liked,
        coined = dto.interaction.coined,
        favorited = dto.interaction.favorited,
    ),
)

internal fun readVideoStats(stat: JSONObject?): VideoStatsDto = VideoStatsDto(
    stat?.l("view") ?: 0, stat?.l("danmaku") ?: 0, stat?.l("like") ?: 0,
    stat?.l("coin") ?: 0, stat?.l("favorite") ?: 0, stat?.l("share") ?: 0, stat?.l("reply") ?: 0,
)

private fun readCollection(data: JSONObject): VideoCollectionDto {
    val pages = data.optJSONArray("pages").objects().map {
        VideoPageDto(
            it.l("cid"), it.optInt("page", 1), it.s("part"), it.optInt("duration", 0),
            firstFrame = it.s("first_frame"),
        )
    }
    val season = data.optJSONObject("ugc_season")
    val episodes = season?.optJSONArray("sections").objects()
        .flatMap { it.optJSONArray("episodes").objects() }.mapNotNull(::readEpisode)
    return VideoCollectionDto(pages, episodes, season?.s("title").orEmpty())
}

private fun readEpisode(ep: JSONObject): VideoEpisodeDto? {
    val arc = ep.optJSONObject("arc")
    val bvid = ep.s("bvid").ifBlank { arc?.s("bvid").orEmpty() }
    val cid = ep.l("cid")
    if (bvid.isBlank() || cid <= 0) return null
    val stat = arc?.optJSONObject("stat") ?: ep.optJSONObject("stat")
    return VideoEpisodeDto(
        bvid, ep.l("aid").takeIf { it > 0 } ?: (arc?.l("aid") ?: 0L), cid,
        ep.s("title").ifBlank { arc?.s("title").orEmpty() }, arc?.s("pic").orEmpty(),
        arc?.optInt("duration", 0) ?: 0, stat?.l("view") ?: 0L, stat?.l("danmaku") ?: 0L,
        arc?.l("pubdate") ?: ep.l("pubdate"),
    )
}
