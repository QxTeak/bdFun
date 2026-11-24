package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.domain.videoPlaybackPages
import com.aritxonly.bdfun.core.model.video.VideoDetailOwner
import com.aritxonly.bdfun.core.model.video.VideoDetailInfo
import com.aritxonly.bdfun.core.model.video.VideoDetailCounts
import com.aritxonly.bdfun.core.model.video.*
import org.junit.Assert.*
import org.junit.Test

class VideoPagePolicyTest {
    private val first = VideoPage(1, 1, "第一P", 10)
    private val second = VideoPage(2, 2, "第二P", 20)
    private val detail = VideoDetail(
        bvid = "one",
        aid = 42,
        title = "标题",
        cover = "",
        desc = "简介",
        owner = VideoDetailOwner(
            ownerName = "UP",
            ownerFace = "",
            ownerMid = 7,
            ownerSign = "",
        ),
        counts = VideoDetailCounts(
            play = 0,
            danmaku = 0,
            like = 0,
            coin = 0,
            favorite = 0,
            share = 0,
            reply = 0,
        ),
        info = VideoDetailInfo(
            pubdate = 0,
            duration = 10,
            cid = 1,
            tname = "分区",
            pages = listOf(first),
        ),
    )
    private fun episode(cid: Long) = VideoEpisode("BV$cid", cid, cid, "集$cid", "", 30)
    @Test fun multiPartKeepsOriginalPartsAndDeduplicatesCollectionByCid() {
        val pages = videoPlaybackPages(detail.copy(
            info = detail.info.copy(
                pages = listOf(first, second),
                episodes = listOf(episode(2), episode(3)),
            ),
        ))
        assertEquals(listOf(1L, 2L, 3L), pages.map { it.cid })
        assertEquals(second, pages[1])
    }
    @Test fun collectionTakesPriorityForSinglePartVideo() {
        val pages = videoPlaybackPages(detail.copy(
            info = detail.info.copy(
                episodes = listOf(episode(1), episode(3)),
            ),
        ))
        assertEquals(listOf("集1", "集3"), pages.map { it.part })
        assertEquals(listOf(1, 2), pages.map { it.page })
    }
    @Test fun singleCollectionEntryDoesNotReplaceOriginalPart() {
        assertEquals(listOf(first), videoPlaybackPages(detail.copy(
            info = detail.info.copy(
                episodes = listOf(episode(9)),
            ),
        )))
    }
    @Test fun collectionStartIsZeroForSinglePartVideo() {
        // 单 P 视频的播放页就是纯合集列表，合集从第 0 项开始。
        val single = detail.copy(info = detail.info.copy(episodes = listOf(episode(1), episode(3))))
        assertEquals(0, collectionStartIndex(single))
    }
    @Test fun collectionStartSkipsOwnParts() {
        // 多 P 视频前面是自己分 P，合集第一集要跳过它们。
        val multi = detail.copy(info = detail.info.copy(
            pages = listOf(first, second),
            episodes = listOf(episode(3), episode(4)),
        ))
        assertEquals(listOf(1L, 2L, 3L, 4L), videoPlaybackPages(multi).map { it.cid })
        assertEquals(2, collectionStartIndex(multi))
    }
    @Test fun collectionStartIsMinusOneWithoutCollection() {
        assertEquals(-1, collectionStartIndex(detail))
        assertEquals(-1, collectionStartIndex(detail.copy(info = detail.info.copy(episodes = listOf(episode(9))))))
    }
}
