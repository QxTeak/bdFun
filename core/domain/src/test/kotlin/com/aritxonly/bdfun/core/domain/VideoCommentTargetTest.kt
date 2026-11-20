package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.model.video.*
import org.junit.Assert.assertEquals
import org.junit.Test

class VideoCommentTargetTest {
    private fun detail(epId: Long) = VideoDetail(
        bvid = "BV1xx411c7mD",
        aid = 170001,
        title = "标题",
        cover = "",
        desc = "",
        owner = VideoDetailOwner("UP", "", 7, ""),
        counts = VideoDetailCounts(0, 0, 0, 0, 0, 0, 0),
        info = VideoDetailInfo(0, 10, 1, "", emptyList(), epId = epId, seasonId = if (epId > 0) 43161 else 0),
    )

    @Test fun ordinaryVideoUsesAid() {
        val target = videoCommentTarget(detail(0L))
        assertEquals("170001", target.oid)
        assertEquals(1, target.type)
    }

    /**
     * 番剧/影视评论区也挂在**单集 aid** 上；用 ep_id 请求会返回 0 条。
     * 换集时 aid 变化，评论区因此自动重建。
     */
    @Test fun pgcEpisodeUsesEpisodeAidInsteadOfEpId() {
        val target = videoCommentTarget(detail(689147L))
        assertEquals("170001", target.oid)
        assertEquals(1, target.type)
    }
}
