package com.aritxonly.bdfun.ui

import android.content.Context
import com.aritxonly.bdfun.core.application.DownloadQueueStateHolder
import com.aritxonly.bdfun.core.application.PlaybackQueueStateHolder
import com.aritxonly.bdfun.core.domain.PlaybackProgressRepository
import com.aritxonly.bdfun.core.domain.SentHistoryRepository
import com.aritxonly.bdfun.feature.video.application.VideoStateFactory
import com.aritxonly.bdfun.feature.video.presentation.VideoPlaybackPlatform

/** 原生接缝由app组装，页面只使用当前显式依赖；延迟读取保持原初始化时机。 */
internal class VideoPageDependencies(
    val controllers: VideoStateFactory,
    val progress: PlaybackProgressRepository,
    private val downloadsProvider: () -> DownloadQueueStateHolder,
    private val sentHistoryProvider: () -> SentHistoryRepository,
    val background: BackgroundPlaybackStateHolder,
    val queue: PlaybackQueueStateHolder,
    val createPlayback: (Context, Int) -> VideoPlaybackPlatform,
    val log: (String, String) -> Unit,
    val comments: CommentDependencies,
    /** 分段弹幕窗口：播放推进时按需补齐当前段前后。 */
    val danmakuFeed: com.aritxonly.bdfun.core.application.DanmakuSegmentFeed,
) {
    val downloads get() = downloadsProvider()
    val sentHistory get() = sentHistoryProvider()
}
