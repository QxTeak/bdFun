package com.aritxonly.bdfun.feature.video.presentation

import com.aritxonly.bdfun.ui.videoCommentTabLabel
import org.junit.Assert.assertEquals
import org.junit.Test

class VideoCommentTabLabelTest {
    @Test fun zeroOrNegativeCountFallsBackToPlainLabel() {
        assertEquals("评论", videoCommentTabLabel(0L))
        assertEquals("评论", videoCommentTabLabel(-1L))
    }

    @Test fun smallCountsStayVerbatim() {
        assertEquals("评论 1", videoCommentTabLabel(1L))
        assertEquals("评论 406", videoCommentTabLabel(406L))
        assertEquals("评论 9999", videoCommentTabLabel(9999L))
    }

    @Test fun largeCountsUseCompactUnitsWithoutInnerSpace() {
        assertEquals("评论 1.2万", videoCommentTabLabel(12_345L))
        assertEquals("评论 10万", videoCommentTabLabel(100_000L))
        assertEquals("评论 1.3亿", videoCommentTabLabel(130_000_000L))
    }

    @Test fun compactUnitBoundaryMatchesFormatCount() {
        // formatCount 在 10000 处进位，「评论 1万」不能出现空格或 .0。
        assertEquals("评论 1万", videoCommentTabLabel(10_000L))
        assertEquals("评论 9999", videoCommentTabLabel(9_999L))
    }
}