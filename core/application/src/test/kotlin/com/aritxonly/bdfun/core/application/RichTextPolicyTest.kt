package com.aritxonly.bdfun.core.application

import com.aritxonly.bdfun.core.application.videoTextClickBvid
import com.aritxonly.bdfun.core.application.displayVideoTextToken
import com.aritxonly.bdfun.core.model.video.ResolvedVideoLink
import org.junit.Assert.*
import org.junit.Test

class RichTextPolicyTest {
    @Test fun commentDictionaryAloneResolvesUninstalledPackagesAndNestedNames() {
        val emotes = mapOf("[未下载包_笑[开心]]" to "remote", "未下载包_笑" to "short")
        val text = "[未下载包_笑[开心]] [未下载包_笑] [未下载包_笑[开心]]"
        val plan = analyzeRichText(text, emotes, emptyMap(), false)
        assertEquals(listOf("remote", "short", "remote"), plan.hits.map { it.emoteUrl })
        assertEquals(listOf("未下载包_笑[开心]", "未下载包_笑", "未下载包_笑[开心]"),
            plan.hits.map { it.emoteToken })
        assertFalse(plan.plainText(false))
    }
    @Test fun literalNamesEscapeRegexAndDoNotReplaceUnknownOrBareText() {
        val plan = analyzeRichText("[包_a+b.*] [包_axxbZZ] 包_a+b.*", mapOf("包_a+b.*" to "remote"), emptyMap(), false)
        assertEquals(1, plan.hits.size)
        assertEquals(0, plan.hits.single().start)
        assertEquals("[包_a+b.*]".length, plan.hits.single().end)
    }

    @Test fun longPackageEmoteNamesAreRecognizedWithoutTruncation() {
        val token = "这是一个超过十二个字符的表情包名称_开心"
        val hit = analyzeRichText("[$token]", mapOf(token to "url"), emptyMap(), false).hits.single()
        assertEquals(token, hit.emoteToken)
        assertEquals(token.length + 2, hit.end)
    }

    private val bv = "BV1orW46aEba"
    @Test fun plainPathFlagsMatchTheOriginalEvenForAnUnknownEmote() {
        assertTrue(analyzeRichText("正文", emptyMap(), emptyMap(), false).plainText(false))
        assertFalse(analyzeRichText("正文", emptyMap(), emptyMap(), false).plainText(true))
        val unknown = analyzeRichText("[unknown]", mapOf("other" to "image"), emptyMap(), false)
        assertTrue(unknown.hasEmote); assertTrue(unknown.hits.isEmpty()); assertFalse(unknown.plainText(false))
    }
    @Test fun urlWinsOverEmotesAndMentionsAndOnlyItsTrailingPunctuationIsExcluded() {
        val text = "https://example.test/@小明[doge]，"
        val plan = analyzeRichText(text, mapOf("doge" to "image"), mapOf("小明" to 9), true)
        assertEquals(1, plan.hits.size)
        assertEquals("https://example.test/@小明[doge", plan.hits.single().linkUrl)
        assertEquals(plan.hits.single().linkUrl!!.length, plan.hits.single().end)
        assertTrue(plan.hasEmote); assertTrue(plan.hasMention)
    }
    @Test fun emotesWinOverContainedMentionsAndTokenKeysWinOverBracketedKeys() {
        val plan = analyzeRichText("[@甲]", mapOf("@甲" to "first", "[@甲]" to "second"), mapOf("甲" to 1), true)
        assertEquals(1, plan.hits.size); assertEquals("first", plan.hits.single().emoteUrl)
        assertNull(plan.hits.single().mentionName)
    }
    @Test fun mentionsPreferLongNamesEscapeRegexCharactersAndRetainZeroMid() {
        val names = linkedMapOf("小" to 1L, "小明" to 2L, "A+B" to 0L)
        val plan = analyzeRichText("@小明 @A+B", emptyMap(), names, true)
        assertEquals(listOf("小明", "A+B"), plan.hits.map { it.mentionName })
        assertEquals(listOf(2L, 0L), plan.hits.map { it.mentionMid })
        assertTrue(analyzeRichText("@小明", emptyMap(), names, false).hits.isEmpty())
        assertFalse(analyzeRichText("@小明", emptyMap(), names, false).hasMention)
    }
    @Test fun repeatedBareIdUsesOnlyFirstLiteralOccurrenceWhileUrlsRetainEveryMatch() {
        val plan = analyzeRichText("x$bv $bv $bv", emptyMap(), emptyMap(), false)
        assertEquals(1, plan.hits.size); assertEquals(1, plan.hits.single().start)
        assertEquals(2, collectRichLinks("https://x.test/one https://x.test/one").size)
    }
    @Test fun offsetsRemainUtf16AndHitsStayInSourceOrder() {
        val plan = analyzeRichText("🙂 $bv [doge] @甲", mapOf("doge" to "image"), mapOf("甲" to 1), true)
        assertEquals(3, plan.hits.first().start)
        assertEquals(listOf(bv, null, null), plan.hits.map { it.linkUrl })
        assertEquals(listOf(null, "doge", null), plan.hits.map { it.emoteToken })
        assertEquals("甲", plan.hits.last().mentionName)
    }
    @Test fun longEmotesAreSupportedAndGenericUrlCaseStaysUnchanged() {
        assertEquals("image", analyzeRichText("[abcdefghijklm]", mapOf("abcdefghijklm" to "image"), emptyMap(), false).hits.single().emoteUrl)
        assertEquals("image", analyzeRichText("[abcdefghijkl]", mapOf("[abcdefghijkl]" to "image"), emptyMap(), false).hits.single().emoteUrl)
        assertTrue(analyzeRichText("HTTP://example.test", emptyMap(), emptyMap(), false).plainText(false))
        val short = analyzeRichText("http://a", emptyMap(), emptyMap(), false)
        assertTrue(short.hasUrl); assertTrue(short.hits.isEmpty()); assertFalse(short.plainText(false))
    }
    @Test fun titleAndClickFallbacksKeepRawTextUntilMetadataArrives() {
        assertEquals(bv, displayVideoTextToken(bv, emptyMap()))
        assertEquals(bv, videoTextClickBvid("bv" + bv.substring(2), emptyMap()))
        assertNull(videoTextClickBvid("av123", emptyMap()))
        assertNull(videoTextClickBvid("https://b23.tv/a", emptyMap()))
        val resolved = mapOf("av123" to ResolvedVideoLink(bv, "标题"))
        assertEquals("标题", displayVideoTextToken("av123", resolved)); assertEquals(bv, videoTextClickBvid("av123", resolved))
    }

    @Test fun commentTimestampsAcceptEnglishAndChineseColons() {
        val plan = analyzeRichText("前面 13:10 中间 1：02：33 后面", emptyMap(), emptyMap(), false, timestampClick = true)
        assertEquals(listOf(13 * 60 + 10, 3600 + 2 * 60 + 33), plan.hits.map { it.timestampSeconds })
        assertTrue(plan.hasTimestamp)
        assertFalse(plan.plainText(false))
        // 没有播放器时保持纯文本，不标色也不可点。
        assertEquals(0, analyzeRichText("13:10", emptyMap(), emptyMap(), false).hits.size)
    }

    @Test fun commentTimestampsRejectDatesUrlsAndOutOfRangeSeconds() {
        assertEquals(0, analyzeRichText("2026:11:28", emptyMap(), emptyMap(), false, timestampClick = true).hits.size)
        assertEquals(0, analyzeRichText("https://a.test/13:10", emptyMap(), emptyMap(), false, timestampClick = true)
            .hits.count { it.timestampSeconds != null })
        assertEquals(0, analyzeRichText("13:99 100:200", emptyMap(), emptyMap(), false, timestampClick = true)
            .hits.count { it.timestampSeconds != null })
    }
}
