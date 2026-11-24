package com.aritxonly.bdfun.core.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/** 「循环播放」决定播完往哪走：合集不能越过用户选的循环模式插队。 */
class PlaybackContinuationPolicyTest {

    @Test
    fun singleLoopAlwaysRepeats() {
        // 单集循环不看有没有下一集，也不看队列。
        assertEquals(
            PlaybackContinuation.RepeatSingle,
            playbackContinuation(1, autoPlayNext = true, hasNextPage = true, hasNextPart = true, hasNextInQueue = true, hasCollection = true),
        )
        assertEquals(
            PlaybackContinuation.RepeatSingle,
            playbackContinuation(1, autoPlayNext = false, hasNextPage = false, hasNextPart = false, hasNextInQueue = false, hasCollection = false),
        )
    }

    @Test
    fun listLoopPrefersPagesThenQueue() {
        // 列表循环：先分 P/合集，再回来源列表。
        assertEquals(
            PlaybackContinuation.NextPage,
            playbackContinuation(2, autoPlayNext = true, hasNextPage = true, hasNextPart = false, hasNextInQueue = true, hasCollection = true),
        )
        assertEquals(
            PlaybackContinuation.NextInQueue,
            playbackContinuation(2, autoPlayNext = true, hasNextPage = false, hasNextPart = false, hasNextInQueue = true, hasCollection = false),
        )
        assertEquals(
            PlaybackContinuation.Stop,
            playbackContinuation(2, autoPlayNext = true, hasNextPage = false, hasNextPart = false, hasNextInQueue = false, hasCollection = false),
        )
    }

    @Test
    fun listLoopRestartsCollectionAtItsEnd() {
        // 列表循环放到合集最后一集：回合集第一集，不跳回来源列表。
        assertEquals(
            PlaybackContinuation.RestartCollection,
            playbackContinuation(2, autoPlayNext = true, hasNextPage = false, hasNextPart = false, hasNextInQueue = true, hasCollection = true),
        )
        // 没有合集（单集视频）时列表循环仍然走来源列表。
        assertEquals(
            PlaybackContinuation.NextInQueue,
            playbackContinuation(2, autoPlayNext = true, hasNextPage = false, hasNextPart = false, hasNextInQueue = true, hasCollection = false),
        )
    }

    @Test
    fun noLoopGoesToQueueInsteadOfCollection() {
        // 不循环：视频属于合集（下一项是合集里的别的稿件）时不能插队，走来源列表。
        assertEquals(
            PlaybackContinuation.NextInQueue,
            playbackContinuation(0, autoPlayNext = true, hasNextPage = true, hasNextPart = false, hasNextInQueue = true, hasCollection = true),
        )
        // 合集且来源列表也没了 → 停，不回合集开头（回合集开头只属于列表循环）。
        assertEquals(
            PlaybackContinuation.Stop,
            playbackContinuation(0, autoPlayNext = true, hasNextPage = false, hasNextPart = false, hasNextInQueue = false, hasCollection = true),
        )
    }

    @Test
    fun noLoopStillContinuesOwnParts() {
        // 同一稿件的分 P 不算“下一个视频”，不循环也接着播。
        assertEquals(
            PlaybackContinuation.NextPage,
            playbackContinuation(0, autoPlayNext = true, hasNextPage = true, hasNextPart = true, hasNextInQueue = true, hasCollection = false),
        )
        assertEquals(
            PlaybackContinuation.NextPage,
            playbackContinuation(0, autoPlayNext = true, hasNextPage = true, hasNextPart = true, hasNextInQueue = false, hasCollection = false),
        )
    }

    @Test
    fun noLoopRespectsAutoPlayNextSwitch() {
        // 自动连播关掉：不循环时什么都不自动走。
        assertEquals(
            PlaybackContinuation.Stop,
            playbackContinuation(0, autoPlayNext = false, hasNextPage = true, hasNextPart = true, hasNextInQueue = true, hasCollection = true),
        )
    }
}