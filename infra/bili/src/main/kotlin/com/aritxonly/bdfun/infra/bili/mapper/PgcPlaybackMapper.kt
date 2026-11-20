package com.aritxonly.bdfun.infra.bili.mapper

import com.aritxonly.bdfun.core.common.biliImageThumbUrl
import com.aritxonly.bdfun.core.model.pgc.BangumiEpisode
import com.aritxonly.bdfun.core.model.pgc.BangumiSeason
import com.aritxonly.bdfun.core.model.pgc.BangumiStat
import com.aritxonly.bdfun.core.model.pgc.BangumiUpInfo
import com.aritxonly.bdfun.core.model.pgc.BangumiUserStatus
import com.aritxonly.bdfun.core.model.video.VideoDetail
import com.aritxonly.bdfun.core.model.video.VideoDetailCounts
import com.aritxonly.bdfun.core.model.video.VideoDetailInfo
import com.aritxonly.bdfun.core.model.video.VideoDetailInteraction
import com.aritxonly.bdfun.core.model.video.VideoDetailOwner
import com.aritxonly.bdfun.core.model.video.VideoEpisode
import com.aritxonly.bdfun.infra.bili.biliAvatar as avatar
import com.aritxonly.bdfun.infra.bili.biliImage as image
import org.json.JSONObject

/**
 * `/pgc/view/web/season` 的 result → BangumiSeason。
 *
 * 字段来自 2026-11-19 的真实响应（匿名请求 season_id=33378、38867）：
 * episodes[].ep_id/aid/cid/bvid/title/long_title/show_title/cover/duration/pub_time/stat，
 * 预告花絮在 `section[]`（不是 `sections`），每项有 title + episodes；
 * 统计在 stat.{views,danmakus,likes,coins,favorite,share,reply}；
 * 出品方在 up_info.{mid,uname,avatar,is_follow}。
 */
internal fun mapBangumiSeason(result: JSONObject): BangumiSeason {
    val main = result.optJSONArray("episodes").objects().mapNotNull { it.toEpisode("正片") }
    val extra = ArrayList<BangumiEpisode>()
    // 真实响应里 key 是 section；保留 sections 兜底，兼容旧缓存/结构探针。
    val sections = result.optJSONArray("section") ?: result.optJSONArray("sections")
    sections.objects().forEach { section ->
        val title = section.s("title").ifBlank { "其他" }
        section.optJSONArray("episodes").objects().forEach { item ->
            item.toEpisode(title)?.let(extra::add)
        }
    }
    val stat = result.optJSONObject("stat")
    val up = result.optJSONObject("up_info")
    return BangumiSeason(
        seasonId = result.l("season_id"),
        title = result.s("title").ifBlank { result.s("season_title") },
        cover = biliImageThumbUrl(result.s("cover"), 480),
        rating = result.optJSONObject("rating")?.s("score").orEmpty(),
        evaluate = result.s("evaluate"),
        subtitle = result.s("subtitle"),
        episodes = dedupeById(main + extra),
        type = result.optInt("type", 0),
        stat = BangumiStat(
            play = stat?.l("views") ?: 0L,
            danmaku = stat?.l("danmakus") ?: 0L,
            like = stat?.l("likes") ?: 0L,
            coin = stat?.l("coins") ?: 0L,
            favorite = stat?.l("favorite") ?: 0L,
            share = stat?.l("share") ?: 0L,
            reply = stat?.l("reply") ?: 0L,
        ),
        up = BangumiUpInfo(
            mid = up?.l("mid") ?: 0L,
            name = up?.s("uname").orEmpty(),
            avatar = up?.s("avatar").orEmpty(),
        ),
        userStatus = BangumiUserStatus(
            // up_info.is_follow 是「是否关注出品方」；番剧追番状态在 user_status，这里不混用。
            followed = (up?.optInt("is_follow", 0) ?: 0) == 1,
            // 登录时 season 会带观看进度；未看过/未登录为 0。
            lastEpId = result.optJSONObject("user_status")
                ?.optJSONObject("progress")?.l("last_ep_id") ?: 0L,
        ),
    )
}

/** 同一集可能同时出现在 episodes 和 section 里，按 ep_id 保留第一次出现。 */
private fun dedupeById(items: List<BangumiEpisode>): List<BangumiEpisode> {
    val seen = HashSet<Long>()
    return items.filter { it.epId > 0L && seen.add(it.epId) }
}

private fun JSONObject.toEpisode(sectionTitle: String): BangumiEpisode? {
    val epId = l("ep_id").takeIf { it > 0L } ?: l("id").takeIf { it > 0L } ?: return null
    val cid = l("cid")
    if (cid <= 0L) return null
    val stat = optJSONObject("stat")
    return BangumiEpisode(
        epId = epId,
        aid = l("aid"),
        cid = cid,
        bvid = s("bvid"),
        title = s("title"),
        longTitle = s("long_title"),
        cover = biliImageThumbUrl(s("cover"), 320),
        durationMs = l("duration"),
        sectionTitle = sectionTitle,
        showTitle = s("show_title"),
        stat = BangumiStat(
            play = stat?.l("play") ?: 0L,
            danmaku = stat?.l("danmakus") ?: 0L,
            like = stat?.l("likes") ?: 0L,
            coin = stat?.l("coin") ?: 0L,
            reply = stat?.l("reply") ?: 0L,
        ),
        pubTime = l("pub_time"),
    )
}

/**
 * 把 PGC season + 当前集映射成视频详情页使用的 [VideoDetail]。
 *
 * 只负责字段搬运：不猜字段、不做播放选择，评论身份由 core:domain 的
 * `videoCommentTarget` 统一决定（ep_id + type=1）。
 */
internal fun mapPgcVideoDetail(
    season: BangumiSeason,
    episode: BangumiEpisode,
    cleanHtml: (String) -> String,
): VideoDetail {
    val episodeLabel = episode.showTitle.ifBlank {
        episode.longTitle.ifBlank { episode.title }
    }
    val title = listOf(season.title, episodeLabel)
        .filter { it.isNotBlank() }
        .joinToString(" ")
        .ifBlank { episode.title }
    val stat = season.stat
    // 存储的是列表缩略图（@320w）；播放器封面要从原图重新按 1200w 取，不能继承缩略尺寸。
    val detailCover = episode.cover.ifBlank { season.cover }.substringBefore('@')
    return VideoDetail(
        bvid = episode.bvid,
        aid = episode.aid,
        title = cleanHtml(title),
        cover = image(detailCover, 1200),
        desc = cleanHtml(season.evaluate),
        owner = VideoDetailOwner(
            ownerName = season.up.name,
            ownerFace = avatar(season.up.avatar, 160),
            ownerMid = season.up.mid,
            ownerSign = "",
        ),
        counts = VideoDetailCounts(
            play = stat.play,
            danmaku = stat.danmaku,
            like = stat.like,
            coin = stat.coin,
            favorite = stat.favorite,
            share = stat.share,
            reply = stat.reply,
        ),
        info = VideoDetailInfo(
            pubdate = episode.pubTime,
            duration = (episode.durationMs / 1000L).toInt(),
            cid = episode.cid,
            tname = pgcTypeLabel(season.type),
            // PGC 没有「分 P」；剧集统一走合集列表，避免同一个视频出现两种选集入口。
            pages = emptyList(),
            episodes = season.episodes.map { item ->
                VideoEpisode(
                    bvid = item.bvid,
                    aid = item.aid,
                    cid = item.cid,
                    title = item.displayName,
                    cover = item.cover,
                    duration = (item.durationMs / 1000L).toInt(),
                    play = item.stat.play,
                    danmaku = item.stat.danmaku,
                    pubdate = item.pubTime,
                    epId = item.epId,
                )
            },
            seasonTitle = season.title,
            epId = episode.epId,
            seasonId = season.seasonId,
        ),
        interaction = VideoDetailInteraction(isFollowed = season.userStatus.followed),
    )
}

private fun pgcTypeLabel(type: Int): String = when (type) {
    2 -> "电影"
    3 -> "纪录片"
    4 -> "国创"
    5 -> "电视剧"
    7 -> "综艺"
    else -> "番剧"
}
