package com.aritxonly.bdfun.ui

import com.aritxonly.bdfun.core.domain.SentHistoryRepository
import com.aritxonly.bdfun.core.model.video.VideoDetail
import com.aritxonly.bdfun.core.model.video.VideoItem

interface VideoDetailDanmakuHost {
    var aid: Long
    var cid: Long
    val danmakuProgressMs: Long
    var danmakuModeration: com.aritxonly.bdfun.feature.video.application.DanmakuModerationStateHolder?
    var danmakuReporter: (suspend (Long, com.aritxonly.bdfun.core.domain.DanmakuReportReason, String) -> com.aritxonly.bdfun.core.common.Outcome<Unit>)?
    var danmakuVoteLoader: (suspend (List<Long>) -> Unit)?
    var danmakuVotes: kotlinx.coroutines.flow.StateFlow<Map<Pair<Long, Long>, com.aritxonly.bdfun.core.domain.DanmakuVote>>?
    var danmakuLiker: (suspend (Long) -> com.aritxonly.bdfun.core.common.Outcome<Boolean>)?
    var danmakuOwnerMid: Long
    var styledDanmakuSender: StyledDanmakuSender?
    var danmakuSender: (suspend (String, Int, Int, Int) -> Boolean)?
}

class VideoDetailEffectPorts(
    val danmaku: VideoDetailDanmakuHost,
    val sentHistory: () -> SentHistoryRepository,
    val publishFullscreen: (Boolean) -> Unit,
    val updateBackground: (VideoItem, VideoDetail?, Long, Int) -> Unit,
)
