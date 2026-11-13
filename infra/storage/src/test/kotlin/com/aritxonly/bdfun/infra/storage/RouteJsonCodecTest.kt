package com.aritxonly.bdfun.infra.storage

import com.aritxonly.bdfun.core.model.feed.FeedItemAuthor

import com.aritxonly.bdfun.core.model.feed.FeedCounts

import com.aritxonly.bdfun.core.model.feed.FeedAuthor

import com.aritxonly.bdfun.core.model.feed.FeedAttachments

import com.aritxonly.bdfun.core.model.video.VideoItemExtras

import com.aritxonly.bdfun.core.navigation.Route
import com.aritxonly.bdfun.core.model.video.*
import com.aritxonly.bdfun.core.model.feed.*
import org.junit.Assert.*
import org.junit.Test

class RouteJsonCodecTest {
    private val video = VideoItem(
        bvid = "BVone",
        aid = Long.MAX_VALUE,
        title = "标题\"😀",
        cover = "cover",
        ownerName = "UP",
        ownerFace = "face",
        ownerMid = 88,
        play = 11,
        danmaku = 12,
        duration = 13,
        pubdate = 14,
        extras = VideoItemExtras(
            cid = 15,
            reason = "推荐",
            isFollowed = true,
            startPositionMs = 123456,
            categoryName = "游戏",
            recommendationTrackId = "track",
            appRecommendation = true,
            dislikeReasons = listOf(RecommendationReason(1, "原因")),
        ),
    )
    private val dynamic = FeedItem(
        id = "id",
        author = FeedItemAuthor(
            name = "UP",
            face = "face",
            mid = 88,
            authorVip = true,
            ipLocation = "上海",
        ),
        time = "时间",
        text = "内容\n😀",
        images = listOf("a", "b"),
        video = video,
        counts = FeedCounts(
            like = 11,
            comment = 12,
            forward = 13,
            liked = true,
        ),
        commentOid = "oid",
        commentType = 11,
        attachments = FeedAttachments(
            title = "文章",
            isCollection = true,
            pgcCover = "pc",
            pgcTitle = "pt",
            pgcLabel = "pl",
            moduleTag = "tag",
            liveRecommendation = FeedLiveRecommendation(1, "直播", "封面", true, "分区", "观看"),
        ),
    )
    private val codec = RouteJsonCodec()

    @Test fun videoSnapshotAndCommentTargetSurviveIndependentDecoder() {
        val route = Route.Video(video.bvid, video.aid, video, true, 21, 22, 23)
        assertEquals(route, RouteJsonCodec().decode(codec.encode(route)))
    }
    @Test fun nestedForwardAndAllFeedMetadataSurviveRoundTrip() {
        val forwarded = dynamic.copy(id = "forward", attachments = dynamic.attachments.copy(orig = dynamic))
        val route = Route.Feed(forwarded.id, forwarded)
        assertEquals(route, RouteJsonCodec().decode(codec.encode(route)))
    }
    @Test fun independentlyOpenedPagesDoNotShareSnapshot() {
        val first = codec.encode(Route.Video(video.bvid, snapshot = video))
        val second = codec.encode(Route.Video("two", snapshot = video.copy(bvid = "two")))
        assertEquals("two", (codec.decode(second) as Route.Video).snapshot?.bvid)
        assertEquals(video, (codec.decode(first) as Route.Video).snapshot)
    }
    @Test fun idOnlyRoutesKeep64BitAidAndSpaceId() {
        val routes = listOf(Route.Video("", Long.MAX_VALUE), Route.Video("BVone"), Route.Space(Long.MAX_VALUE))
        routes.forEach { assertEquals(it, codec.decode(codec.encode(it))) }
    }
    @Test fun malformedAndUnknownPayloadsAreRejected() {
        listOf("broken", "{}", "{\"version\":2,\"type\":\"space\",\"mid\":1}",
            "{\"version\":1,\"type\":\"other\"}").forEach { assertNull(codec.decode(it)) }
    }
    @Test fun privateChatKeepsIdentityPresentationAndReadReceiptParameters() {
        val route = Route.PrivateChat(Long.MAX_VALUE, 2, "昵称\"😀", "avatar", 7, Long.MAX_VALUE - 1)
        assertEquals(route, RouteJsonCodec().decode(codec.encode(route)))
    }
    @Test fun privateChatDefaultsMatchTheOriginalIntentDefaults() {
        assertEquals(Route.PrivateChat(42), codec.decode("""{"version":1,"type":"private-chat","talkerId":42}"""))
    }
}
