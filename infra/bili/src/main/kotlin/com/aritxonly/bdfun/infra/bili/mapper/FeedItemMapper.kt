package com.aritxonly.bdfun.infra.bili.mapper

import com.aritxonly.bdfun.core.model.feed.FeedItemAuthor
import com.aritxonly.bdfun.core.model.feed.FeedCounts
import com.aritxonly.bdfun.core.model.feed.FeedAuthor
import com.aritxonly.bdfun.core.model.feed.FeedAttachments
import org.json.JSONObject
import com.aritxonly.bdfun.core.model.feed.*
import com.aritxonly.bdfun.core.model.video.*
import com.aritxonly.bdfun.infra.bili.biliImage as image
import com.aritxonly.bdfun.infra.bili.biliAvatar as avatar
import com.aritxonly.bdfun.infra.bili.parseBiliCountText as parseCountText
import com.aritxonly.bdfun.infra.bili.parseBiliDurationText as parseDurationText
import com.aritxonly.bdfun.infra.bili.cancellableCatching

internal fun parseFeedItem(item: JSONObject, allowOrig: Boolean = true): FeedItem? {
    val modules = item.optJSONObject("modules") ?: return null
    val author = modules.optJSONObject("module_author") ?: return null
    val dynamic = modules.optJSONObject("module_dynamic")
    val desc = dynamic?.optJSONObject("desc")?.s("text").orEmpty()
    val major = FeedMajor(dynamic?.optJSONObject("major"))
    val pgc = major.pgc
    val ugcSeason = major.ugcSeason
    val video = parseFeedVideo(major.archive, ugcSeason, author)
    val content = parseFeedContent(major, desc)
    val title = content.title
    val text = content.text
    val images = content.images
    val liveRecommendation = parseFeedLiveRecommendation(major.liveRcmd)
    val stat = modules.optJSONObject("module_stat")
    val basic = item.optJSONObject("basic")
    val orig = if (allowOrig) {
        item.optJSONObject("orig")?.let { parseFeedItem(it, allowOrig = false) }
    } else {
        null
    }
    return FeedItem(
        id = item.s("id_str"),
        author = FeedItemAuthor(
            name = author.s("name"),
            face = avatar(author.s("face"), 120),
            mid = author.l("mid"),
            authorVip = author.optJSONObject("vip")?.optInt("vipType", 0) == 2,
            ipLocation = author.s("pub_location_text")
            .ifBlank { author.s("pub_location") }
            .ifBlank { author.s("ip_location") }
            .ifBlank { author.s("location") },
        ),
        time = author.s("pub_time"),
        text = text,
        images = images,
        video = video,
        counts = FeedCounts(
            like = stat?.optJSONObject("like")?.optInt("count", 0) ?: 0,
            comment = stat?.optJSONObject("comment")?.optInt("count", 0) ?: 0,
            forward = stat?.optJSONObject("forward")?.optInt("count", 0) ?: 0,
            liked = stat?.optJSONObject("like")?.optBoolean("status", false) ?: false,
        ),
        commentOid = basic?.s("comment_id_str").orEmpty()
            .ifBlank { item.s("id_str") },
        commentType = (basic?.optInt("comment_type", 17) ?: 17)
            .takeIf { it > 0 } ?: 17,
        attachments = FeedAttachments(
            title = title,
            orig = orig,
            isCollection = !ugcSeason?.s("bvid").isNullOrBlank(),
            isCharge = major.archiveBadge.contains("充电"),
            pgcCover = pgc?.s("cover").orEmpty().takeIf { it.isNotBlank() }
            ?.let { image(it, 720) }.orEmpty(),
            pgcTitle = pgc?.s("title").orEmpty(),
            pgcLabel = when (pgc?.optInt("sub_type", 0)) {
            3 -> "纪录片"
            1 -> "番剧"
            2 -> "电影"
            4 -> "国创"
            5 -> "电视剧"
            else -> "剧集"
        },
            pgcSeasonId = pgcId(pgc, "season_id", "seasonId", "season"),
            pgcEpId = pgcId(pgc, "epid", "ep_id", "epId", "ep"),
            moduleTag = modules.optJSONObject("module_tag")?.s("text").orEmpty(),
            liveRecommendation = liveRecommendation,
        ),
    )
}

/**
 * 动态 PGC 卡的季/集 id：优先用接口字段，缺失时从 `uri` 里取。
 * 字段名在不同版本会变，这里只是兜底，不改变其它字段的读取顺序。
 */
private val pgcSeasonPathPattern = Regex("season/(\\d+)")
private val pgcSeasonQueryPattern = Regex("[?&](?:season_id|seasonId)=(\\d+)")
private val pgcEpisodePathPattern = Regex("(?:ep|epid|episode)/(\\d+)")
private val pgcEpisodeQueryPattern = Regex("[?&](?:ep_id|epid)=(\\d+)")

private fun pgcId(pgc: JSONObject?, vararg keys: String): Long {
    if (pgc == null) return 0L
    for (key in keys) {
        val value = pgc.optLong(key, 0L)
        if (value > 0L) return value
    }
    val uri = pgc.s("uri").ifBlank { pgc.s("jump_url") }
    if (uri.isBlank()) return 0L
    val pattern = when (keys.first()) {
        "season_id" -> pgcSeasonPathPattern.find(uri) ?: pgcSeasonQueryPattern.find(uri)
        else -> pgcEpisodePathPattern.find(uri) ?: pgcEpisodeQueryPattern.find(uri)
    }
    return pattern?.groupValues?.getOrNull(1)?.toLongOrNull() ?: 0L
}
