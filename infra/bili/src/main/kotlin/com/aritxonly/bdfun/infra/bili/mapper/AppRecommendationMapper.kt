package com.aritxonly.bdfun.infra.bili.mapper

import com.aritxonly.bdfun.core.model.video.*
import com.aritxonly.bdfun.core.model.user.*
import com.aritxonly.bdfun.infra.bili.cancellableCatching
import com.aritxonly.bdfun.infra.bili.biliImage as image
import com.aritxonly.bdfun.infra.bili.biliAvatar as avatar
import com.aritxonly.bdfun.infra.bili.biliAidToBvid as aidToBvid
import com.aritxonly.bdfun.infra.bili.parseBiliCountText as parseCountText
import com.aritxonly.bdfun.infra.bili.parseBiliDurationText as parseDurationText
import org.json.JSONObject
import org.json.JSONArray

internal class AppRecommendationMapping(val items: List<VideoItem>, val rawCount: Int)

internal fun mapAppRecommendations(data: JSONObject, cleanHtml: (String) -> String): AppRecommendationMapping {
    val out = data.optJSONArray("items").objects().mapNotNull { raw ->
        val args = raw.optJSONObject("args")
            ?: cancellableCatching { JSONObject(raw.optString("args")) }.getOrNull()
            ?: JSONObject()
        val player = raw.optJSONObject("player_args")
        val cardGoto = raw.s("card_goto")
        val goto = raw.s("goto")
        // App 推荐里混着直播、广告、横幅、游戏卡和竖屏短视频；
        // 竖屏卡片的 aid 是 12 位以上，标准播放接口打不开，这里只要普通稿件。
        val isVideoCard = cardGoto == "av" || goto == "av" ||
            (cardGoto.isEmpty() && goto.isEmpty() &&
                raw.s("card_type").startsWith("small_cover"))
        if (!isVideoCard) return@mapNotNull null
        val aid = args.l("aid")
            .takeIf { it > 0L }
            ?: player?.l("aid")?.takeIf { it > 0L }
            ?: raw.s("param").toLongOrNull()
            ?: 0L
        // av↔bv 公式只支持 32 位 aid；超出的都是竖屏/广告卡片。
        if (aid !in 1L..2_147_483_647L) return@mapNotNull null
        val directBvid = raw.s("bvid").ifBlank { args.s("bvid") }
            .ifBlank { player?.s("bvid").orEmpty() }
        val bvid = directBvid.ifBlank { aidToBvid(aid) }
        if (bvid.isBlank()) return@mapNotNull null
        VideoItem(
            bvid = bvid,
            aid = aid,
            title = cleanHtml(raw.s("title")),
            cover = image(raw.s("cover"), 480),
            ownerName = args.s("up_name").ifBlank { args.s("name") },
            ownerFace = avatar(args.s("up_face").ifBlank { args.s("face") }, 96),
            ownerMid = args.l("up_id").takeIf { it > 0 } ?: args.l("mid"),
            play = parseCountText(raw.s("cover_left_text_1")),
            danmaku = parseCountText(raw.s("cover_left_text_2")),
            duration = player?.optInt("duration", 0)
                ?.takeIf { it > 0 }
                ?: parseDurationText(raw.s("cover_right_text")),
            pubdate = 0,
            extras = VideoItemExtras(
                cid = player?.l("cid")?.takeIf { it > 0 } ?: 0,
                categoryName = args.s("tname").ifBlank { raw.s("tname") },
                appRecommendation = true,
                dislikeReasons = (raw.optJSONArray("three_point_v2").objects()
                .filter { it.s("type") == "dislike" }.flatMap { it.optJSONArray("reasons").objects() } +
                raw.optJSONObject("three_point")?.optJSONArray("dislike_reasons").objects())
                .map { RecommendationReason(it.l("id"), it.s("name")) }.distinctBy { it.id },
                isFollowed = args.optBoolean("is_followed", false) ||
                raw.optBoolean("is_followed", false),
                isCharge = raw.hasChargeBadge(),
            ),
        )
    }
    val rawCount = data.optJSONArray("items")?.length() ?: 0
    return AppRecommendationMapping(out, rawCount)
}

internal fun mapFeedVideos(data: JSONObject, key: String, parseVideo: (JSONObject) -> VideoItem?): List<VideoItem> =
    data.optJSONArray(key).objects().mapNotNull(parseVideo)

internal fun mapRelatedVideos(root: JSONObject, parseVideo: (JSONObject) -> VideoItem?): List<VideoItem> =
    root.optJSONArray("data").objects().mapNotNull(parseVideo)
