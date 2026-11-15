package com.aritxonly.bdfun.infra.bili.mapper

import org.junit.Assert.assertEquals
import org.junit.Test

class CommentTextDecoderTest {
    @Test fun decodesNamedEntitiesWithoutRemovingLiteralTags() {
        assertEquals("<测试> & \"正文\" '回复'\u00a0©…", decodeCommentText(
            "&lt;测试&gt; &amp; &quot;正文&quot; &apos;回复&apos;&nbsp;&copy;&hellip;"))
        assertEquals("<b>正文</b>\n下一行", decodeCommentText("<b>正文</b>\n下一行"))
    }
    @Test fun numericEntitiesSupportChineseAndSupplementaryUnicode() {
        assertEquals("中文😀'", decodeCommentText("&#20013;&#x6587;&#X1F600;&#39;"))
    }
    @Test fun preservesInvalidAndUnknownEntities() {
        val raw = "&unknown; &;xx &amp &#0; &#xD800; &#1114112; &#9999999999;"
        assertEquals(raw, decodeCommentText(raw))
    }
    @Test fun decodesOnceAndPreservesLinksEmotesMentionsAndWhitespace() {
        assertEquals("&lt; [doge] @某人 https://example.com/?a=1&b=2\n  ", decodeCommentText(
            "&amp;lt; [doge] @某人 https://example.com/?a=1&amp;b=2\n  "))
    }
}
