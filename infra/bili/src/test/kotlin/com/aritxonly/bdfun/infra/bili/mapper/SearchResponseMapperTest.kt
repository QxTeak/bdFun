package com.aritxonly.bdfun.infra.bili.mapper

import com.aritxonly.bdfun.core.domain.SearchHotWord
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class SearchResponseMapperTest {
    @Test fun realSuggestionsRetainTermPriorityDistinctOrderAndEightItemLimit() {
        val data = javaClass.classLoader!!.getResourceAsStream("json/search-suggest-anonymous.json")!!
            .bufferedReader().use { JSONObject(it.readText()) }
        val result = mapSearchSuggestions(data) { error("real sample has terms") }
        assertEquals(8, result.size)
        assertEquals("灭霸的爸爸叫灭建国", result.first())
        val variant = JSONObject("""{"result":{"tag":[{"term":"term","name":"ignored"},
            {"term":"term"},{"name":"name"}]}}""")
        assertEquals(listOf("term", "decoded:name"), mapSearchSuggestions(variant) { "decoded:$it" })
    }
    @Test fun videosAndUsersKeepExplicitHtmlBoundaryAndLegacyNumberParsing() {
        val data = JSONObject("""{"result":[{}, {"bvid":"BV1orW46aEba","aid":117584544403664,
            "title":"title","author":"author","mid":3690989657000489,"play":"1.2万",
            "danmaku":"7","duration":"01:02","pubdate":123,
            "uname":"user","usign":"sign","fans":"9","videos":"bad","level":6}]}""")
        val video = mapSearchVideos(data) { "decoded:$it" }.single()
        assertEquals("decoded:title", video.title)
        assertEquals("decoded:author", video.ownerName)
        assertEquals(3690989657000489L, video.ownerMid)
        assertEquals(0L, video.play)
        assertEquals(7L, video.danmaku)
        assertEquals(62, video.duration)
        val user = mapSearchUsers(data) { "decoded:$it" }.single()
        assertEquals("decoded:user", user.name)
        assertEquals("decoded:sign", user.sign)
        assertEquals(9L, user.fans)
        assertEquals(0L, user.videos)
        assertEquals(6, user.level)
    }
    @Test fun searchRecommendReadsListKeywordWithShowNameFallback() {
        // 结构取自 2026-11-23 匿名请求 app.bilibili.com/x/v2/search/recommend 的真实响应（list 截取）。
        val data = JSONObject("""{"code":0,"data":{"list":[
            {"keyword":"最中立的电竞","show_name":"最中立的电竞"},
            {"keyword":"异环","show_name":"异环"},
            {"keyword":"异环","show_name":"异环"},
            {"show_name":"只有展示名"},
            {"keyword":""}]}}""").getJSONObject("data")
        assertEquals(listOf("最中立的电竞", "异环", "只有展示名"), mapSearchRecommend(data))
        assertTrue(mapSearchRecommend(JSONObject()).isEmpty())
    }
    @Test fun hotwordSourcesRetainDuplicatesAndOriginalFallbackFields() {
        val data = JSONObject("""{"trending":{"list":[{"keyword":" word ","show_name":"展示一",
            "icon":"http://i0.hdslb.com/a.png"},{"keyword":"word"},{"keyword":"word"},{"keyword":""}]}}""")
        assertEquals(
            listOf(
                SearchHotWord(" word ", "展示一", "http://i0.hdslb.com/a.png"),
                SearchHotWord("word"),
                SearchHotWord("word"),
            ),
            mapSearchDiscovery(data),
        )
        // 词条小标只在 square 接口里有，热搜词兜底接口没有。
        assertEquals(
            listOf(SearchHotWord("keyword"), SearchHotWord("name")),
            mapHotwordSearch(JSONObject("""{"data":[
                {"keyword":"keyword","name":"ignored"},{"name":"name"},{}]}""")),
        )
        assertTrue(mapHotwordSearch(JSONObject()).isEmpty())
    }
}
