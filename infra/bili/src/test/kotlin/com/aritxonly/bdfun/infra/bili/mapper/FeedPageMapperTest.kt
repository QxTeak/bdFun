package com.aritxonly.bdfun.infra.bili.mapper

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

/** 原字段结构探针，不冒充已登录动态入口的真实响应。 */
class FeedPageMapperTest {
    @Test fun portalDeduplicatesAuthorsFirstWinsButKeepsRepeatedLiveRoomsAndServerCount() {
        val portal = mapFeedPortal(JSONObject("""{"up_list":{"items":[{"mid":0},
            {"mid":3690989657000489,"uname":"first","has_update":false},
            {"mid":3690989657000489,"uname":"second","has_update":true}],"offset":"next","has_more":true},
            "live_users":{"count":5,"items":[{"room_id":0},{"mid":7,"room_id":99},{"mid":8,"room_id":99}]}}"""))
        assertEquals(3690989657000489L, portal.upList.items.single().mid)
        assertEquals("first", portal.upList.items.single().name)
        assertFalse(portal.upList.items.single().hasUpdate)
        assertEquals("next", portal.upList.offset)
        assertTrue(portal.upList.hasMore)
        assertEquals(listOf(99L, 99L), portal.liveUsers.map { it.roomId })
        assertEquals(5, portal.liveCount)
        assertTrue(parseFeedUpPage(null).items.isEmpty())
    }

    @Test fun pageAndDetailKeepExistingItemReaderAndBooleanCursorSemantics() {
        val data = JSONObject("""{"items":[{}, {"id_str":"12345678901234567890",
            "modules":{"module_author":{}}}],"offset":"cursor","has_more":true}""")
        val page = mapFeedPage(data)
        assertEquals("12345678901234567890", page.items.single().id)
        assertEquals("cursor", page.offset)
        assertTrue(page.hasMore)
        assertFalse(mapFeedPage(data.put("has_more", 1)).hasMore)
        val detail = mapFeedDetail(JSONObject().put("item", data.getJSONArray("items").getJSONObject(1)))!!
        assertEquals(page.items.single().id, detail.id)
        assertNull(mapFeedDetail(JSONObject()))
    }
}
