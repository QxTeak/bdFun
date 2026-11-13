package com.aritxonly.bdfun.infra.storage

import com.aritxonly.bdfun.core.model.feed.FeedItemAuthor

import com.aritxonly.bdfun.core.model.feed.FeedCounts

import com.aritxonly.bdfun.core.model.feed.FeedAuthor

import com.aritxonly.bdfun.core.model.feed.FeedAttachments

import com.aritxonly.bdfun.core.model.video.VideoItemExtras

import com.aritxonly.bdfun.core.navigation.Route
import com.aritxonly.bdfun.core.model.video.*
import com.aritxonly.bdfun.core.model.feed.*
import org.json.JSONArray
import org.json.JSONObject

/** 只编码本应用的导航快照，不使用 Java 序列化或进程内的全局传参槽。 */
class RouteJsonCodec {
    fun encode(route: Route): String = JSONObject().apply {
        put("version", 1)
        when (route) {
            is Route.MessageInbox -> { put("type", "message-inbox"); put("pageType", route.pageType); put("title", route.title) }
            is Route.MessageTools -> {
                put("type", "message-tools"); put("kind", route.kind); put("id", route.id); put("settingType", route.type); put("count", route.count)
                put("navigation", messageNavigation(route.navigation))
            }
            is Route.Space -> { put("type", "space"); put("mid", route.mid) }
            is Route.PrivateChat -> {
                put("type", "private-chat"); put("talkerId", route.talkerId); put("sessionType", route.sessionType)
                put("name", route.name); put("avatar", route.avatar); put("unread", route.unread); put("maxSeqno", route.maxSeqno)
            }
            is Route.Video -> {
                put("type", "video"); put("bvid", route.bvid); put("aid", route.aid)
                put("snapshot", route.snapshot?.let(::video))
                put("fromVideoPage", route.fromVideoPage); put("commentRoot", route.commentRoot)
                put("commentTarget", route.commentTarget); put("seekMs", route.seekMs)
            }
            is Route.Feed -> {
                put("type", "dynamic"); put("id", route.id); put("snapshot", route.snapshot?.let(::dynamic))
            }
        }
    }.toString()

    fun decode(raw: String): Route? = try {
        val value = JSONObject(raw)
        if (value.optInt("version") != 1) null else when (value.optString("type")) {
            "message-inbox" -> Route.MessageInbox(value.getInt("pageType"), value.getString("title"))
            "message-tools" -> Route.MessageTools(value.getString("kind"), value.optString("id"), value.optInt("settingType"),
                value.optInt("count"), readMessageNavigation(value.getJSONObject("navigation")))
            "space" -> Route.Space(value.getLong("mid"))
            "private-chat" -> Route.PrivateChat(value.getLong("talkerId"), value.optInt("sessionType", 1),
                value.optString("name"), value.optString("avatar"), value.optInt("unread"), value.optLong("maxSeqno"))
            "video" -> Route.Video(value.optString("bvid"), value.optLong("aid"),
                value.optJSONObject("snapshot")?.let(::readVideo), value.optBoolean("fromVideoPage"),
                value.optLong("commentRoot"), value.optLong("commentTarget"), value.optLong("seekMs"))
            "dynamic" -> Route.Feed(value.getString("id"), value.optJSONObject("snapshot")?.let(::readFeed))
            else -> null
        }
    } catch (_: Exception) { null }

    private fun messageNavigation(value: com.aritxonly.bdfun.core.model.message.MessageNavigation) = JSONObject().apply {
        put("bvid", value.bvid); put("aid", value.aid); put("root", value.rootRpid); put("target", value.targetRpid)
        put("source", value.sourceRpid); put("business", value.businessId); put("seek", value.seekMs); put("uri", value.nativeUri)
    }
    private fun readMessageNavigation(value: JSONObject) = com.aritxonly.bdfun.core.model.message.MessageNavigation(
        value.optString("bvid"), value.optLong("aid"), value.optLong("root"), value.optLong("target"),
        value.optLong("source"), value.optInt("business"), value.optLong("seek"), value.optString("uri"))

    private fun video(v: VideoItem): JSONObject = JSONObject().apply {
        put("bvid", v.bvid); put("aid", v.aid); put("title", v.title); put("cover", v.cover)
        put("ownerName", v.ownerName); put("ownerFace", v.ownerFace); put("ownerMid", v.ownerMid)
        put("play", v.play); put("danmaku", v.danmaku); put("duration", v.duration); put("pubdate", v.pubdate)
        put("cid", v.cid); put("reason", v.reason); put("isFollowed", v.isFollowed)
        put("startPositionMs", v.startPositionMs); put("categoryName", v.categoryName)
        put("recommendationTrackId", v.recommendationTrackId); put("appRecommendation", v.appRecommendation)
        put("dislikeReasons", JSONArray(v.dislikeReasons.map { JSONObject().put("id", it.id).put("name", it.name) }))
    }

    private fun readVideo(v: JSONObject) = VideoItem(
        bvid = v.getString("bvid"),
        aid = v.getLong("aid"),
        title = v.getString("title"),
        cover = v.getString("cover"),
        ownerName = v.getString("ownerName"),
        ownerFace = v.getString("ownerFace"),
        ownerMid = v.getLong("ownerMid"),
        play = v.getLong("play"),
        danmaku = v.getLong("danmaku"),
        duration = v.getInt("duration"),
        pubdate = v.getLong("pubdate"),
        extras = VideoItemExtras(
            cid = v.optLong("cid"),
            reason = v.optString("reason"),
            isFollowed = v.optBoolean("isFollowed"),
            startPositionMs = v.optLong("startPositionMs"),
            categoryName = v.optString("categoryName"),
            recommendationTrackId = v.optString("recommendationTrackId"),
            appRecommendation = v.optBoolean("appRecommendation"),
            dislikeReasons = v.optJSONArray("dislikeReasons").objects().map { RecommendationReason(it.getLong("id"), it.getString("name")) },
        ),
    )

    private fun dynamic(d: FeedItem): JSONObject = JSONObject().apply {
        put("id", d.id); put("name", d.name); put("face", d.face); put("mid", d.mid); put("time", d.time)
        put("text", d.text); put("images", JSONArray(d.images)); put("video", d.video?.let(::video))
        put("like", d.like); put("comment", d.comment); put("forward", d.forward); put("liked", d.liked)
        put("commentOid", d.commentOid); put("commentType", d.commentType); put("title", d.title)
        put("orig", d.orig?.let(::dynamic)); put("authorVip", d.authorVip); put("isCollection", d.isCollection)
        put("pgcCover", d.pgcCover); put("pgcTitle", d.pgcTitle); put("pgcLabel", d.pgcLabel)
        put("moduleTag", d.moduleTag); put("ipLocation", d.ipLocation)
        put("liveRecommendation", d.liveRecommendation?.let {
            JSONObject().put("roomId", it.roomId).put("title", it.title).put("cover", it.cover)
                .put("live", it.live).put("areaName", it.areaName).put("watched", it.watched)
        })
    }

    private fun readFeed(d: JSONObject): FeedItem = FeedItem(
        id = d.getString("id"),
        author = FeedItemAuthor(
            name = d.getString("name"),
            face = d.getString("face"),
            mid = d.getLong("mid"),
            authorVip = d.optBoolean("authorVip"),
            ipLocation = d.optString("ipLocation"),
        ),
        time = d.getString("time"),
        text = d.getString("text"),
        images = d.getJSONArray("images").let { a -> List(a.length()) { a.getString(it) } },
        video = d.optJSONObject("video")?.let(::readVideo),
        counts = FeedCounts(
            like = d.getInt("like"),
            comment = d.getInt("comment"),
            forward = d.getInt("forward"),
            liked = d.optBoolean("liked"),
        ),
        commentOid = d.getString("commentOid"),
        commentType = d.getInt("commentType"),
        attachments = FeedAttachments(
            title = d.optString("title"),
            orig = d.optJSONObject("orig")?.let(::readFeed),
            isCollection = d.optBoolean("isCollection"),
            pgcCover = d.optString("pgcCover"),
            pgcTitle = d.optString("pgcTitle"),
            pgcLabel = d.optString("pgcLabel"),
            moduleTag = d.optString("moduleTag"),
            liveRecommendation = d.optJSONObject("liveRecommendation")?.let {
            FeedLiveRecommendation(it.getLong("roomId"), it.getString("title"), it.getString("cover"),
                it.getBoolean("live"), it.getString("areaName"), it.getString("watched"))
        },
        ),
    )

    private fun JSONArray?.objects(): List<JSONObject> = if (this == null) emptyList() else List(length()) { getJSONObject(it) }
}
