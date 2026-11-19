package com.aritxonly.bdfun.infra.bili

import com.aritxonly.bdfun.infra.bili.mapper.readPopularPage
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 样本来源：2026-11-19 匿名请求 `https://api.bilibili.com/x/web-interface/popular?pn=1&ps=5`，
 * 只保留列表第一项的必要字段，并把 title 截短，避免把整段真实标题当测试数据。
 */
class PopularPageMapperTest {
    private val sample = """
        {"code":0,"message":"OK","data":{"list":[
          {"aid":117637157751172,"cid":43721230307,"bvid":"BV1NDsS6UE8D","pic":"http://i1.hdslb.com/bfs/archive/cover.jpg",
           "title":"【侠盗猎车手6】4K 最高画质","pubdate":1795000925,"duration":29840,"tname":"单机游戏",
           "owner":{"mid":360046931,"name":"11的游戏世界","face":"https://i2.hdslb.com/bfs/face/avatar.jpg"},
           "stat":{"view":2397881,"danmaku":13884,"like":43676}},
          {"aid":1,"bvid":"","title":"没有 bvid 的脏数据"}
        ],"no_more":false}}
    """.trimIndent()

    @Test fun mapsRealSampleFields() {
        val page = readPopularPage(JSONObject(sample).getJSONObject("data"))
        assertEquals(1, page.videos.size)
        val video = page.videos.single()
        assertEquals("BV1NDsS6UE8D", video.bvid)
        assertEquals(117637157751172L, video.aid)
        assertEquals("【侠盗猎车手6】4K 最高画质", video.title)
        assertEquals(2397881L, video.play)
        assertEquals(13884L, video.danmaku)
        assertEquals(29840, video.duration)
        assertEquals("11的游戏世界", video.ownerName)
        assertEquals(360046931L, video.ownerMid)
        assertEquals(43721230307L, video.cid)
    }

    @Test fun noMoreFlagDrivesPagination() {
        assertTrue(readPopularPage(JSONObject(sample).getJSONObject("data")).hasMore)
        val finished = JSONObject("""{"list":[{"bvid":"BV1xx411c7mD","title":"t"}],"no_more":true}""")
        assertFalse(readPopularPage(finished).hasMore)
    }

    @Test fun emptyPageStopsPagingEvenWithoutNoMore() {
        val empty = JSONObject("""{"list":[],"no_more":false}""")
        assertFalse(readPopularPage(empty).hasMore)
    }
}
