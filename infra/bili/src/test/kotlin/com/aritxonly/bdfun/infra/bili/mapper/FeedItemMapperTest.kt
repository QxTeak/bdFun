package com.aritxonly.bdfun.infra.bili.mapper

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

/** 结构探针只使用迁移前解析器已有字段，不冒充真实接口返回或作为新增字段依据。 */
class FeedItemMapperTest {
    private fun item(id: String = "12345678901234567890", major: JSONObject = JSONObject(), desc: String = "") =
        JSONObject().put("id_str", id).put("modules", JSONObject()
            .put("module_author", JSONObject().put("mid", 5).put("name", "UP").put("face", "//img/face.jpg"))
            .put("module_dynamic", JSONObject().put("major", major).put("desc", JSONObject().put("text", desc))))
    private fun video(bvid: String) = JSONObject().put("bvid", bvid).put("aid", 7).put("title", "video")
        .put("cover", "//img/video.jpg").put("duration_text", "01:23")
        .put("stat", JSONObject().put("play", "1.2万").put("danmaku", "9000"))
    @Test fun missingModulesOrAuthorIsRejectedButEmptyAuthorAndLongIdAreRetained() {
        assertNull(parseFeedItem(JSONObject()))
        assertNull(parseFeedItem(JSONObject().put("modules", JSONObject())))
        val raw = item(); raw.getJSONObject("modules").put("module_author", JSONObject())
        val result = parseFeedItem(raw)!!
        assertEquals("12345678901234567890", result.id); assertEquals(result.id, result.commentOid)
        assertEquals(17, result.commentType); assertEquals(0L, result.mid); assertNull(result.video)
    }
    @Test fun archiveWinsOverSeasonWhileCollectionFlagStillReflectsSeason() {
        val major = JSONObject().put("archive", video("BV1234567890")).put("ugc_season", video("BVabcdefghij"))
            .put("opus", JSONObject().put("title", "opus title").put("summary", JSONObject().put("text", "opus summary")))
        val result = parseFeedItem(item(major = major, desc = "description"))!!
        assertEquals("BV1234567890", result.video!!.bvid); assertTrue(result.isCollection)
        assertEquals("opus title", result.title); assertEquals("description", result.text)
        assertEquals(12000L, result.video!!.play); assertEquals(9000L, result.video!!.danmaku)
        assertEquals(83, result.video!!.duration); assertEquals("UP", result.video!!.ownerName)
        assertEquals(0L, result.video!!.cid); assertEquals(0L, result.video!!.pubdate)
    }
    @Test fun emptyArchiveObjectDoesNotFallBackToValidSeasonAndMissingArchiveDoes() {
        val major = JSONObject().put("archive", JSONObject()).put("ugc_season", video("BVabcdefghij"))
        assertEquals("", parseFeedItem(item(major = major))!!.video!!.bvid)
        major.remove("archive")
        assertEquals("BVabcdefghij", parseFeedItem(item(major = major))!!.video!!.bvid)
    }
    @Test fun textAndTitleKeepOriginalFallbackOrderAndBlankSemantics() {
        val major = JSONObject().put("opus", JSONObject().put("title", " ").put("summary", JSONObject().put("text", "opus text")))
            .put("article", JSONObject().put("title", "article title").put("desc", "article text"))
            .put("common", JSONObject().put("title", "common title").put("desc", "common text").put("summary", "summary"))
            .put("pgc", JSONObject().put("desc", "pgc text"))
        assertEquals("article title", parseFeedItem(item(major = major, desc = " \n"))!!.title)
        assertEquals("opus text", parseFeedItem(item(major = major))!!.text)
        major.getJSONObject("opus").remove("summary")
        assertEquals("article text", parseFeedItem(item(major = major))!!.text)
        major.getJSONObject("article").put("desc", " ")
        assertEquals("common text", parseFeedItem(item(major = major))!!.text)
        major.getJSONObject("common").put("desc", ""); assertEquals("summary", parseFeedItem(item(major = major))!!.text)
        major.getJSONObject("common").put("summary", ""); assertEquals("pgc text", parseFeedItem(item(major = major))!!.text)
    }
    @Test fun imagesPreserveSourceOrderMixedCoverFormatsAndDeduplicateAfterNormalization() {
        val major = JSONObject().put("draw", JSONObject().put("items", JSONArray().put(JSONObject().put("src", "//img/a.jpg"))))
            .put("opus", JSONObject().put("pics", JSONArray().put(JSONObject().put("url", "http://img/a.jpg")).put(JSONObject().put("url", "//img/b.jpg"))))
            .put("article", JSONObject().put("covers", JSONArray().put("//img/c.jpg").put(JSONObject().put("url", "//img/d.jpg")).put(123)))
            .put("common", JSONObject().put("cover", "//img/e.jpg")).put("live", JSONObject().put("cover", ""))
            .put("medialist", JSONObject().put("cover", "//img/f.jpg")).put("courses", JSONObject().put("cover", "//img/e.jpg"))
            .put("music", JSONObject().put("cover", "//img/g.jpg")).put("pgc", JSONObject().put("cover", "//img/pgc.jpg"))
        val result = parseFeedItem(item(major = major))!!
        // 动态图片保留原图 URL：缩略图裁切由界面层按显示宽度加，查看器必须拿到完整图。
        assertEquals(('a'..'g').map { "https://img/$it.jpg" }, result.images)
        assertEquals("https://img/pgc.jpg@720w_405h_1c.webp", result.pgcCover)
    }
    @Test fun originalForwardingStillStopsAfterOneLevel() {
        val raw = item("one").put("orig", item("two").put("orig", item("three")))
        val result = parseFeedItem(raw)!!
        assertEquals("two", result.orig!!.id); assertNull(result.orig!!.orig)
        assertNull(parseFeedItem(raw, allowOrig = false)!!.orig)
    }
    @Test fun subscriptionLiveFallbackMalformedContentAndEmptyDirectObjectKeepOriginalRules() {
        val info = JSONObject().put("live_play_info", JSONObject().put("room_id", 99).put("title", "live")
            .put("cover", "//img/live.jpg").put("live_status", 1).put("area_name", "area")
            .put("watched_show", JSONObject().put("text_large", "1万人")))
        val major = JSONObject().put("subscription_new", JSONObject().put("live_rcmd", JSONObject().put("content", info.toString())))
        val result = parseFeedItem(item(major = major))!!.liveRecommendation!!
        assertEquals(99L, result.roomId); assertTrue(result.live); assertEquals("area", result.areaName); assertEquals("1万人", result.watched)
        major.put("live_rcmd", JSONObject()); assertNull(parseFeedItem(item(major = major))!!.liveRecommendation)
        major.put("live_rcmd", JSONObject().put("content", "bad json")); assertNull(parseFeedItem(item(major = major))!!.liveRecommendation)
    }
    @Test fun statisticsCommentRoutingVipIpAndPgcLabelsKeepOriginalFieldsAndDefaults() {
        val labels = mapOf(1 to "番剧", 2 to "电影", 3 to "纪录片", 4 to "国创", 5 to "电视剧", 0 to "剧集")
        for ((type, label) in labels) {
            val raw = item(major = JSONObject().put("pgc", JSONObject().put("sub_type", type).put("title", "pgc title")))
            raw.put("basic", JSONObject().put("comment_id_str", "oid").put("comment_type", 1))
            val modules = raw.getJSONObject("modules")
            modules.put("module_stat", JSONObject().put("like", JSONObject().put("count", 12).put("status", true))
                .put("comment", JSONObject().put("count", 3)).put("forward", JSONObject().put("count", 4)))
            modules.put("module_tag", JSONObject().put("text", "tag"))
            modules.getJSONObject("module_author").put("vip", JSONObject().put("vipType", 2))
                .put("pub_location_text", " ").put("pub_location", "IP first").put("ip_location", "IP second")
            val result = parseFeedItem(raw)!!
            assertEquals(label, result.pgcLabel); assertEquals("pgc title", result.pgcTitle)
            assertEquals("oid", result.commentOid); assertEquals(1, result.commentType); assertTrue(result.authorVip)
            assertEquals("IP first", result.ipLocation); assertEquals(12, result.like); assertTrue(result.liked)
            assertEquals(3, result.comment); assertEquals(4, result.forward); assertEquals("tag", result.moduleTag)
            raw.getJSONObject("basic").put("comment_type", -1).put("comment_id_str", "")
            assertEquals(17, parseFeedItem(raw)!!.commentType); assertEquals(result.id, parseFeedItem(raw)!!.commentOid)
        }
    }
}
