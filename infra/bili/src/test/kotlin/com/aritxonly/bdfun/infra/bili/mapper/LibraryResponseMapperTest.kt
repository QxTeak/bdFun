package com.aritxonly.bdfun.infra.bili.mapper

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

/** 收藏/历史形状为原字段构造回归，未使用或伪造登录账号响应。 */
class LibraryResponseMapperTest {
    private fun history() = JSONObject("""{"history":{"bvid":"BV1orW46aEba","oid":117584544403664},
        "title":"title","author_name":"author","view_at":88,"progress":-1,"duration":99,
        "videos":{"view":9,"danmaku":7}}""")

    @Test fun historyKeepsStatFallbackCursorVariantsAndThirtyMappedItemThreshold() {
        val data = JSONObject().put("list", JSONArray().put(JSONObject()).put(history()))
        val page = mapHistoryPage(data)
        assertEquals(1, page.items.size)
        assertEquals(117584544403664L, page.items.single().aid)
        assertEquals(9, page.items.single().play)
        assertEquals(-1, page.items.single().progress)
        assertEquals(88L, page.viewAt)
        assertFalse(page.hasMore)
        assertEquals("cursorMax=null cursorViewAt=null cursorHasMore=null", describeHistoryCursor(data))
        for (flag in listOf<Any>(true, 1)) {
            assertTrue(mapHistoryPage(JSONObject(data.toString()).put("cursor", JSONObject().put("has_more", flag))).hasMore)
        }
        assertTrue(mapHistoryPage(JSONObject(data.toString()).put("has_more", true)).hasMore)
        val many = JSONArray().apply { repeat(30) { put(history()) } }
        assertTrue(mapHistoryPage(JSONObject().put("list", many)).hasMore)
        assertNull(mapLibraryVideoStats(JSONObject()))
        assertEquals(9L to 7L, mapLibraryVideoStats(JSONObject("""{"data":{"stat":{"view":9,"danmaku":7}}}""")))
    }

    @Test fun historyKeepsWatchedCidFromTheNestedArchiveRecord() {
        // 取自历史游标接口的 archive 响应示例，只保留续播相关字段。
        val data = JSONObject("""{"list":[{"history":{"oid":883617049,"bvid":"BV1sK4y147ob",
            "page":1,"cid":205017957,"business":"archive"},"progress":13,"duration":91}]}""")
        val entry = mapHistoryPage(data).items.single()
        assertEquals(205017957L, entry.cid)
        assertEquals(13, entry.progress)
        assertEquals(0L, mapHistoryPage(JSONObject().put("list", JSONArray().put(history()))).items.single().cid)
    }

    @Test fun collectionIdsKeepNumericValuesAndObjectFallbackWithoutAddingStringSupport() {
        val ids = JSONArray("""[-1,0,7,7,"8",{"id":9},{"id":0,"media_id":10},{"id":-2}]""")
        assertEquals(setOf(-1L, 0L, 7L, 9L, 10L), mapFavoriteFolderIds(JSONObject().put("data", ids)))
        assertEquals(setOf(5L), mapFavoriteFolderIds(JSONObject("""{"data":{"list":[{"media_id":5}]}}""")))
        val folder = mapFavoriteFolders(JSONObject("""{"list":[{"id":9,"title":"收藏夹","media_count":3}]}""")).single()
        assertEquals(9L, folder.id)
        assertEquals("收藏夹", folder.title)
        assertEquals(3, folder.count)
    }

    @Test fun watchLaterAndFavoriteVideosRetainDistinctFieldsAndRawFortyItemFallback() {
        val later = mapWatchLater(JSONObject("""{"list":[{}, {"bvid":"BV1orW46aEba","aid":4,
            "owner":{"name":"owner"},"add_at":8,"progress":5,"stat":{"view":7}}]}""")).single()
        assertEquals("owner", later.author)
        assertEquals(8L, later.viewedAt)
        assertEquals(7, later.play)
        val valid = JSONObject("""{"bvid":"BV1orW46aEba","id":117584544403664,"upper":{"mid":9,"name":"UP"},
            "cnt_info":{"play":11,"danmaku":12},"pubtime":99}""")
        val mapped = mapFavoriteVideos(JSONObject().put("medias", JSONArray().put(valid)))
        assertEquals(117584544403664L, mapped.first.single().aid)
        assertEquals(11L, mapped.first.single().play)
        assertEquals(99L, mapped.first.single().pubdate)
        assertFalse(mapped.second)
        val raw = JSONObject().put("medias", JSONArray().apply { repeat(40) { put(JSONObject()) } })
        assertTrue(mapFavoriteVideos(raw).first.isEmpty())
        assertTrue(mapFavoriteVideos(raw).second)
        assertFalse(mapFavoriteVideos(raw.put("has_more", false)).second)
    }

    @Test fun relationMappingKeepsRawPaginationAndEventTimeLoginReads() {
        val raw = JSONArray().put(JSONObject().put("mid", 0))
        repeat(19) { raw.put(JSONObject().put("mid", 9)) }
        val data = JSONObject().put("list", raw).put("total", 0)
        assertEquals(listOf(9L), mapRelationTargetIds(data))
        var reads = 0
        val result = mapRelationPage(data, JSONObject("""{"9":{"attribute":6}}"""), { ++reads == 1 }, 1)
        assertEquals(19, result.items.size)
        assertEquals(19, reads)
        assertEquals(6, result.items.first().attribute)
        assertEquals(0, result.items.last().attribute)
        assertEquals(2, result.nextPage)
        assertNull(mapRelationPage(data, null, { true }, 1).items.first().attribute)
        assertEquals(20, mapFollowings(data).size)
        assertEquals(0, mapRelationAttribute(JSONObject()))
        assertEquals(4L to 5L, mapRelationStat(JSONObject("""{"following":4,"follower":5}""")))
    }
}
