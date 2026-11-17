package com.aritxonly.bdfun.infra.bili.mapper

import com.aritxonly.bdfun.infra.bili.mapper.readVideoDetail
import com.aritxonly.bdfun.infra.bili.mapper.readFeedVideo
import com.aritxonly.bdfun.infra.bili.mapper.mapVideoDetail
import com.aritxonly.bdfun.infra.bili.mapper.mapFeedVideo
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class VideoDetailMapperTest {
    private fun sample(name: String): JSONObject = javaClass.classLoader!!
        .getResourceAsStream("json/$name")!!.bufferedReader().use { JSONObject(it.readText()) }
    private fun detail(name: String) = mapVideoDetail(
        readVideoDetail(sample(name).getJSONObject("data")), { it },
    )

    @Test fun collectionVideoRetainsIdentityAndEpisodes() {
        val video = detail("video-detail-1.json")
        assertEquals("BV1orW46aEba", video.bvid)
        assertEquals(117584544403664L, video.aid)
        assertEquals(43443290870L, video.cid)
        assertEquals(2283296L, video.ownerMid)
        assertEquals("小猪P_", video.ownerName)
        assertEquals("灭建国的爸爸叫什么", video.title)
        assertEquals(99, video.duration)
        assertEquals(1, video.pages.size)
        assertTrue(video.episodes.isNotEmpty())
        assertTrue(video.episodes.all { it.bvid.isNotBlank() && it.cid > 0 })
        assertTrue(video.cover.endsWith("@1200w_675h_1c.webp"))
        // 投币页用它判断转载视频最多只能投 1 币，必须保留接口原值。
        assertEquals(3, video.copyright)
    }

    @Test fun secondRealVideoDoesNotInventCollection() {
        val video = detail("video-detail-2.json")
        assertEquals("BV18cxv6SEe7", video.bvid)
        assertEquals(117529095769332L, video.aid)
        assertEquals(548824L, video.ownerMid)
        assertEquals("“灭建国是谁呀😄？？”", video.title)
        assertTrue(video.episodes.isEmpty())
        assertEquals("", video.seasonTitle)
    }

    @Test fun thirdRealVideoRetainsLargeOwnerId() {
        val video = detail("video-detail-3.json")
        assertEquals("BV1ouxp6REjf", video.bvid)
        assertEquals(117539027817782L, video.aid)
        assertEquals(3690985378810373L, video.ownerMid)
        assertEquals("灭霸全家福", video.title)
        assertEquals(1, video.pages.size)
        assertFalse(video.liked)
        assertFalse(video.coined)
        assertFalse(video.favorited)
    }

    @Test fun missingOptionalFieldsKeepLegacyDefaults() {
        val video = mapVideoDetail(readVideoDetail(JSONObject()), { it })
        assertEquals("", video.bvid)
        assertEquals(0L, video.ownerMid)
        assertEquals(0L, video.reply)
        assertEquals("", video.ownerSign)
        assertTrue(video.pages.isEmpty())
        assertTrue(video.episodes.isEmpty())
        assertFalse(video.isFollowed)
        assertEquals(0, video.copyright)
    }

    @Test fun titleDecoderRemainsAnExplicitBoundary() {
        val dto = readVideoDetail(sample("video-detail-2.json").getJSONObject("data"))
        val video = mapVideoDetail(dto) { "decoded:$it" }
        assertEquals("decoded:${dto.title}", video.title)
    }

    @Test fun feedVideoUsesRealRelatedItemAndRejectsMissingBvid() {
        val first = sample("related.json").getJSONArray("data").getJSONObject(0)
        val dto = readFeedVideo(first)!!
        val video = mapFeedVideo(dto) { it }
        assertEquals("BV18cxv6SEe7", video.bvid)
        assertEquals(first.getLong("aid"), video.aid)
        assertEquals(first.getJSONObject("owner").getLong("mid"), video.ownerMid)
        assertEquals(first.getJSONObject("stat").getLong("view"), video.play)
        assertTrue(video.cover.endsWith("@480w_270h_1c.webp"))
        assertNull(readFeedVideo(JSONObject()))
    }
}
