package com.aritxonly.bdfun.infra.bili.mapper

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class CommentMapperTest {
    @Test fun anonymousCommentSuppliesImagesWithoutAnyDownloadedPackages() {
        val reply = javaClass.classLoader!!.getResourceAsStream("json/comment-emotes-anonymous.json")!!
            .bufferedReader().use { JSONObject(it.readText()) }
        val item = parseComment(reply, 0, false)
        assertEquals(setOf("吃瓜", "打call", "滑稽"), item.emotes.keys)
        assertTrue(item.emotes.values.all { it.startsWith("https://") && !it.contains("@") })
        assertTrue(item.message.contains("[吃瓜]"))
    }

    private fun realData(): JSONObject = javaClass.classLoader!!
        .getResourceAsStream("json/comments-anonymous.json")!!.bufferedReader()
        .use { JSONObject(it.readText()).getJSONObject("data") }

    @Test fun realPageKeepsLargeIdentityPicturesAndPreviewReplies() {
        val page = mapCommentPage(realData(), 1, 3546607112948052L)
        val first = page.items.first()
        assertEquals(3, page.items.size)
        assertEquals(319894655297L, first.rpid)
        assertEquals(117584544403664L, first.oid)
        assertEquals(3546607112948052L, first.mid)
        assertTrue(first.isUp)
        assertFalse(page.items[1].isUp)
        assertEquals(listOf(323662867632L, 323664379808L), first.previewReplies.map { it.rpid })
        assertEquals(1, page.items[1].pictures.size)
        // 评论配图同样保留原图：曾经拼过 @720w_405h_1c 的居中裁切，
        // 大图查看器只能看到被裁掉的那一块，所以这里断言“没有裁切参数”。
        assertFalse(page.items[1].pictures.single().contains("_1c"))
        assertTrue(page.items[1].pictures.single().startsWith("https://"))
        assertEquals(253, page.total)
        assertFalse(page.isEnd)
    }

    @Test fun topWinsDuplicateIdentityWhileThreadPageIgnoresTop() {
        val data = realData()
        val top = JSONObject(data.getJSONArray("replies").getJSONObject(0).toString())
        top.getJSONObject("content").put("message", "置顶样本")
        data.put("top", JSONArray().put(top))
        val page = mapCommentPage(data, 1, 0)
        assertEquals(3, page.items.size)
        assertTrue(page.items.first().isTop)
        assertEquals("置顶样本", page.items.first().message)
        val thread = mapCommentPage(data, 1, 0, includeTop = false)
        assertFalse(thread.items.first().isTop)
        assertNotEquals("置顶样本", thread.items.first().message)
    }

    @Test fun emptyAndCountBoundariesRetainOriginalPageSemantics() {
        val empty = mapCommentPage(JSONObject(), 1, 0)
        assertTrue(empty.items.isEmpty())
        assertEquals(0, empty.total)
        assertTrue(empty.isEnd)
        val data = realData()
        assertFalse(mapCommentPage(data, 252, 0).isEnd)
        assertTrue(mapCommentPage(data, 253, 0).isEnd)
        assertEquals(253, mapCommentPage(data, 253, 0).page)
    }

    @Test fun previewsAreLimitedToThreeAndOnlyOneNestedLevel() {
        val data = realData()
        val parent = data.getJSONArray("replies").getJSONObject(1)
        val children = parent.getJSONArray("replies")
        children.getJSONObject(0).put("replies", JSONArray().put(JSONObject(children.getJSONObject(1).toString())))
        children.put(JSONObject(children.getJSONObject(2).toString()).put("rpid", 999999999999L))
        val item = parseComment(parent, 0, false)
        assertEquals(3, item.previewReplies.size)
        assertTrue(item.previewReplies.all { it.previewReplies.isEmpty() })
        assertFalse(item.previewReplies.any { it.rpid == 999999999999L })
    }

    @Test fun optionalCommentFieldsKeepLegacyFallbackAndFilteringRules() {
        val reply = JSONObject("""{"rpid":7,"member":{"mid":42,"vip":{"vipStatus":1}},
            "reply_control":{"location":"IP属地测试"},"content":{"message":"正文",
            "emote":{"[doge]":{"url":"emote-url"},"[]":{"url":"invalid"},"[empty]":{}},
            "pictures":[{"img_src":"","url":"https://example.com/p.jpg"},{}],
            "members":[{"mid":9,"uname":"","name":"被提及者"},{"mid":0,"uname":"忽略"}]}}""")
        val item = parseComment(reply, 42, false)
        assertTrue(item.isUp)
        assertTrue(item.isVip)
        assertEquals("IP属地测试", item.ipLocation)
        assertEquals(mapOf("doge" to "emote-url"), item.emotes)
        assertEquals(1, item.pictures.size)
        assertEquals(9L, item.mentions.single().mid)
        assertEquals("被提及者", item.mentions.single().name)
        assertEquals("", parseComment(JSONObject(), 0, false).message)
    }

    @Test fun panelEmotesNormalizeLabelsAndDropMissingTextOrImage() {
        val data = JSONObject("""{"packages":[{"emote":[
            {"text":"[ doge ]","url":"https://example.com/doge.png"},
            {"text":"[]","url":"https://example.com/empty.png"},{"text":"[missing]"}]}]}""")
        val result = mapReplyEmotes(data)
        assertEquals(listOf("doge"), result.map { it.text })
        assertEquals("https://example.com/doge.png", result.single().url)
        assertTrue(mapReplyEmotes(JSONObject()).isEmpty())
    }
}
