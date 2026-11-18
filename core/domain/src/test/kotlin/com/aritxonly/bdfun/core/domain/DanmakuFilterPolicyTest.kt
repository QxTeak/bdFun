package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.model.video.DanmakuItem
import org.junit.Assert.*
import org.junit.Test

class DanmakuFilterPolicyTest {
    private val options = DanmakuFilterOptions(false, false, false, false, false, false, false, emptySet())
    private fun item(text: String, mode: Int = 1, time: Long = 0, color: Int = 0xFFFFFF) = DanmakuItem(time, mode, color, 1f, text)
    @Test fun retainsNonBlankStandardModesIncludingReverse() {
        val input = (0..9).map { item("mode$it", it) } + item(" \n ")
        assertEquals((1..6).map { "mode$it" }, filterDanmaku(input, options).map { it.text })
    }
    @Test fun scrollBlockRemovesAllScrollingDirections() {
        assertEquals(listOf(4, 5), filterDanmaku((1..6).map { item("$it", it) }, options.copy(blockScroll = true)).map { it.mode })
    }
    @Test fun fixedBlockRemovesBothPinnedDirections() {
        assertEquals(listOf(1, 2, 3), filterDanmaku((1..5).map { item("$it", it) }, options.copy(blockFixed = true)).map { it.mode })
    }
    @Test fun colorBlockUsesExactWhiteRatherThanMaskedRgb() {
        val input = listOf(item("white"), item("alpha-white", color = -1), item("pink", color = 0xFB7299))
        assertEquals(listOf(input.first()), filterDanmaku(input, options.copy(blockColor = true)))
    }
    @Test fun keywordFilterIgnoresBlankKeysAndIsCaseSensitiveSubstring() {
        val input = listOf(item("Alpha"), item("alpha"), item("xAlphaY"), item("other"))
        assertEquals(listOf(input[1], input[3]), filterDanmaku(input, options.copy(keywordFilter = true, keywords = setOf(" ", "", "Alpha"))))
        assertEquals(input, filterDanmaku(input, options.copy(keywords = setOf("Alpha"))))
    }
    @Test fun dedupeKeepsFirstInputOccurrenceBeforeTimeSorting() {
        val first = item("same", time = 100); val earlier = item("same", time = 1); val other = item("other", time = 50)
        assertEquals(listOf(other, first), filterDanmaku(listOf(first, earlier, other), options.copy(dedupe = true)))
        assertEquals(listOf(earlier, other, first), filterDanmaku(listOf(first, earlier, other), options))
    }
    @Test fun removedItemDoesNotClaimDuplicateSlotAndEqualTimesKeepInputOrder() {
        val input = listOf(item("same", mode = 4), item("same", mode = 1), item("other", mode = 1))
        assertEquals(input.drop(1), filterDanmaku(input, options.copy(blockFixed = true, dedupe = true)))
    }
    @Test fun blockCountUsesMergedCountInsteadOfLikesOrMode() {
        val popular = item("热门").copy(likeCount = 99)
        val merged = item("合并").copy(count = 4)
        assertEquals(listOf(popular), filterDanmaku(listOf(popular, merged), options.copy(blockCount = true)))
    }
    @Test fun advancedAndCountFlagsDoNotReintroduceOriginallyRejectedModes() {
        val input = (1..9).map { item("$it", it) }
        assertEquals(filterDanmaku(input, options), filterDanmaku(input, options.copy(blockAdvanced = true, blockCount = true)))
    }
}
