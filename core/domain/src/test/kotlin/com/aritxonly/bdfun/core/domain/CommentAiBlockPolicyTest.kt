package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.model.comment.CommentExtras
import com.aritxonly.bdfun.core.model.comment.CommentItem
import org.junit.Assert.*
import org.junit.Test

class CommentAiBlockPolicyTest {

    private fun comment(
        message: String,
        uname: String = "路人甲",
        isTop: Boolean = false,
        replyCount: Int = 0,
        previews: List<CommentItem> = emptyList(),
    ) = CommentItem(
        rpid = message.hashCode().toLong(), uname = uname, avatar = "", message = message,
        like = 0, ctime = 0, level = 0, replyCount = replyCount, isUp = false, isTop = isTop,
        extras = CommentExtras(previewReplies = previews),
    )

    private fun standard(replyMode: Int = REPLY_MODE_ONLY) =
        CommentAiBlockPolicy(CommentAiBlockConfig(enabled = true, strength = STRENGTH_STANDARD, replyMode = replyMode))

    @Test fun disabledOrOutOfRangeStrengthKeepsEverything() {
        val items = listOf(comment("我是大肥鱼，欢迎主人"), comment("普通评论"))
        assertSame(items, CommentAiBlockPolicy(CommentAiBlockConfig()).filter(items))
        assertSame(items, CommentAiBlockPolicy(CommentAiBlockConfig(enabled = true, strength = 0)).filter(items))
        assertSame(items, CommentAiBlockPolicy(CommentAiBlockConfig(enabled = true, strength = 9)).filter(items))
        assertFalse(CommentAiBlockConfig().isActive())
    }

    @Test fun accountNameWithFatFishIsAlwaysBlocked() {
        assertTrue(standard().isBlocked(comment("随便说说", uname = "大肥鱼本鱼")))
        assertTrue(standard().isBlocked(comment("随便说说", uname = "肥魚二號")))
        assertFalse(standard().isBlocked(comment("随便说说", uname = "小鲸鱼观察员")))
    }

    @Test fun brandWordsBlockAtCupAndAbove() {
        val cup = CommentAiBlockPolicy(CommentAiBlockConfig(enabled = true, strength = STRENGTH_CUP))
        assertTrue(cup.isBlocked(comment("本鲸来啦")))
        assertTrue(cup.isBlocked(comment("大肥魚到此一游")))
        assertTrue(cup.isBlocked(comment("主人今天也在看呢")))
        assertFalse(cup.isBlocked(comment("这条视频不错@小明")))
    }

    @Test fun standardAddsAtButKeepsSingleCharWords() {
        assertTrue(standard().isBlocked(comment("@小明 你看这个")))
        assertFalse(standard().isBlocked(comment("这条鱼烤得不错")))
        assertFalse(standard().isBlocked(comment("12345@qq.com 是我的邮箱")))
        assertFalse(standard().isBlocked(comment("QQ 邮箱见 12345@qq.com")))
    }

    @Test fun autoQuotePrefixAloneIsNotCountedAsMentioning() {
        // B 站给每条回复自动拼「回复 @某人 :」，若把它算成 @，标准档会拦掉所有回复
        assertFalse(standard().isBlocked(comment("回复 @小明 : 同意")))
        assertTrue(standard().isBlocked(comment("回复 @小明 : 我也 @张三 一下")))
    }

    @Test fun killAddsSingleCharAndFullWidthPunctuation() {
        val kill = CommentAiBlockPolicy(CommentAiBlockConfig(enabled = true, strength = STRENGTH_KILL))
        assertTrue(kill.isBlocked(comment("这条鱼烤得不错")))
        assertTrue(kill.isBlocked(comment("今天：来了")))
        assertTrue(kill.isBlocked(comment("「引用」一下")))
        assertTrue(kill.isBlocked(comment("前文——后文")))
        assertFalse(kill.isBlocked(comment("普通 ASCII 逗号, 不算")))
    }

    @Test fun atOnlyTierIgnoresKeywordsUntilRealMention() {
        val atOnly = CommentAiBlockPolicy(CommentAiBlockConfig(enabled = true, strength = STRENGTH_AT_ONLY))
        assertTrue(atOnly.isBlocked(comment("大肥鱼@小明")))
        assertFalse(atOnly.isBlocked(comment("大肥鱼本鱼路过")))
    }

    @Test fun pinnedCommentSurvivesEvenWhenItLooksLikeAi() {
        val kept = standard().filter(listOf(comment("主人好呀", uname = "大肥鱼", isTop = true)))
        assertEquals(1, kept.size)
    }

    @Test fun replyOnlyModeDropsMatchedPreviewsAndDecrementsCount() {
        val root = comment("原标题", replyCount = 5, previews = listOf(comment("普通回复"), comment("本鲸报到"), comment("普通回复二")))
        val kept = standard(REPLY_MODE_ONLY).filter(listOf(root))
        assertEquals(1, kept.size)
        assertEquals(4, kept[0].replyCount)
        assertEquals(listOf("普通回复", "普通回复二"), kept[0].previewReplies.map { it.message })
    }

    @Test fun wholeModeDropsTheFloorWhenOnlyRepliesHit() {
        val root = comment("原标题", replyCount = 3, previews = listOf(comment("本鲸报到")))
        assertTrue(standard(REPLY_MODE_WHOLE).filter(listOf(root)).isEmpty())
    }

    @Test fun noneModeKeepsBlockedPreviews() {
        val root = comment("原标题", replyCount = 3, previews = listOf(comment("本鲸报到")))
        val kept = standard(REPLY_MODE_NONE).filter(listOf(root))
        assertEquals(1, kept.size)
        assertEquals(3, kept[0].replyCount)
        assertEquals(1, kept[0].previewReplies.size)
    }

    @Test fun blockedRootTakesWholeFloorRegardlessOfReplyMode() {
        val root = comment("本鲸报到", replyCount = 9, previews = listOf(comment("普通回复")))
        assertTrue(standard(REPLY_MODE_NONE).filter(listOf(root)).isEmpty())
    }
}
