package com.aritxonly.bdfun.ui

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import com.aritxonly.bdfun.core.model.video.VideoDetail
import com.aritxonly.bdfun.core.model.video.VideoItem

class VideoActivityPageStateHolder(initialAutoNext: Int) {
    val consumedAutoNext = mutableIntStateOf(initialAutoNext)
    val comments = mutableStateOf<VideoDetail?>(null)
    val danmakuInputOpen = mutableStateOf(false)
    /** 详情接口的 stat.reply；Tab 条在覆盖层，读不到详情状态，只能由页面单向发布。 */
    val commentCount = mutableStateOf(0L)
}
