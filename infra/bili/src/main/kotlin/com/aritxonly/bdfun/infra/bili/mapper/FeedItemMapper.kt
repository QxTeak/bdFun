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
            moduleTag = modules.optJSONObject("module_tag")?.s("text").orEmpty(),
            liveRecommendation = liveRecommendation,
        ),
    )
}
