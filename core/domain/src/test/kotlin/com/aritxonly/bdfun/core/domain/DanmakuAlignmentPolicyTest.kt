package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.model.video.*
import org.junit.Assert.*
import org.junit.Test

class DanmakuAlignmentPolicyTest {
    @Test fun scanTimestampOnlyInsideVideoDuration() {
        assertEquals(83_000L, danmakuTimestampMs("空降 01:23 有彩蛋", 200_000L))
        assertEquals(3_600_000L, danmakuTimestampMs("1:00:00", 4_000_000L))
        assertNull(danmakuTimestampMs("1:67:03", 9_000_000L))
        assertNull(danmakuTimestampMs("2:15", 120_000L))
    }

    @Test fun cloudRulesKeepSenderKeywordAndRegexIndependent() {
        val matcher = DanmakuRuleMatcher(listOf(
            DanmakuCloudRule(1, DanmakuRuleKind.Sender, "a0bc"),
            DanmakuCloudRule(2, DanmakuRuleKind.Keyword, "测试"),
            DanmakuCloudRule(3, DanmakuRuleKind.Pattern, "foo[0-9]+"),
            DanmakuCloudRule(4, DanmakuRuleKind.Pattern, "["),
        ))
        fun item(text: String, hash: String = "") = DanmakuItem(0, 1, 0, 25f, text, userHash = hash)
        assertTrue(matcher.blocks(item("abc", "A0BC")))
        assertTrue(matcher.blocks(item("一条测试弹幕")))
        assertTrue(matcher.blocks(item("foo99")))
        assertFalse(matcher.blocks(item("foo")))
    }

    @Test fun commandWindowAndGradeSubmissionMatchServerSemantics() {
        val grade = DanmakuCommand(1, DanmakuCommandKind.Grade, "打分", 1000, 3000,
            interactionId = 10, options = listOf(DanmakuCommandOption(2, "1"), DanmakuCommandOption(4, "2")))
        assertEquals(grade, activeDanmakuCommand(listOf(grade), emptySet(), 1000))
        assertNull(activeDanmakuCommand(listOf(grade), emptySet(), 4000))
        assertNull(activeDanmakuCommand(listOf(grade), setOf(grade.id), 1000))
        assertTrue(DanmakuCommandSelection(1, 2, grade, 4).valid)
        assertFalse(DanmakuCommandSelection(1, 2, grade, 3).valid)
        assertFalse(DanmakuCommandSelection(1, 2, grade, 6).valid)
    }
}
