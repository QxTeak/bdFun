package com.aritxonly.bdfun.core.domain

/** 一个视频播完之后往哪走。 */
enum class PlaybackContinuation {
    /** 单集循环：重播当前这一集。 */
    RepeatSingle,

    /** 切到同一播放列表里的下一项（分 P 或合集里的下一个稿件）。 */
    NextPage,

    /** 切到来源列表（稍后再看 / 历史 / 收藏夹…）里的下一个视频。 */
    NextInQueue,

    /** 合集已经放到最后一集，回到合集的第一集重新循环。 */
    RestartCollection,

    /** 什么都不做，停在播完的状态。 */
    Stop,
}

/**
 * 播完的走向完全由「循环播放」设置决定，自动连播只负责“要不要自动走”：
 *
 * - 单集循环：一直重播当前集；
 * - 列表循环：先播完当前稿件的分 P，再进合集的下一集；合集放到最后一集后
 *   回到合集第一集重新循环（不会跳回来源列表，也不重播分 P）；
 * - 不循环：只接着播同一稿件的分 P（同一稿件不算“下一个视频”），
 *   合集属于别的稿件，不能插队，直接回到来源列表。
 *
 * @param hasNextPage 分 P 或合集里还有下一项
 * @param hasNextPart 下一项仍是同一稿件的分 P（合集条目不算）
 * @param hasNextInQueue 来源列表里还有下一个视频
 * @param hasCollection 当前视频带合集，且 [hasNextPage] 为 false 时说明合集已经放到末尾
 */
fun playbackContinuation(
    loopMode: Int,
    autoPlayNext: Boolean,
    hasNextPage: Boolean,
    hasNextPart: Boolean,
    hasNextInQueue: Boolean,
    hasCollection: Boolean,
): PlaybackContinuation = when (loopMode) {
    1 -> PlaybackContinuation.RepeatSingle
    2 -> when {
        hasNextPage -> PlaybackContinuation.NextPage
        hasCollection -> PlaybackContinuation.RestartCollection
        hasNextInQueue -> PlaybackContinuation.NextInQueue
        else -> PlaybackContinuation.Stop
    }
    else -> when {
        autoPlayNext && hasNextPart -> PlaybackContinuation.NextPage
        autoPlayNext && hasNextInQueue -> PlaybackContinuation.NextInQueue
        else -> PlaybackContinuation.Stop
    }
}