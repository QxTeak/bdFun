package com.aritxonly.bdfun.ui

import com.aritxonly.bdfun.core.domain.SentHistoryRepository
import com.aritxonly.bdfun.core.model.video.VideoDetail
import com.aritxonly.bdfun.core.model.video.VideoItem

interface VideoDetailDanmakuHost {
    var aid: Long
    var cid: Long
    val danmakuProgressMs: Long
    var danmakuReporter: (suspend (Long, com.aritxonly.bdfun.core.domain.DanmakuReportReason, String) -> Boolean)?
    var danmakuLiker: (suspend (Long) -> Boolean)?
    var danmakuSender: (suspend (String, Int, Int, Int) -> Boolean)?
}

class VideoDetailEffectPorts(
    val danmaku: VideoDetailDanmakuHost,
    val sentHistory: () -> SentHistoryRepository,
    val publishFullscreen: (Boolean) -> Unit,
    val updateBackground: (VideoItem, VideoDetail?, Long, Int) -> Unit,
)
