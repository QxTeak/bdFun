package com.aritxonly.bdfun.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aritxonly.bdfun.core.model.video.VideoItem
import com.aritxonly.bdfun.feature.library.application.*

internal class LibraryPresentationStateHolder(chrome: PageChromePresentationState) {
    var consumedScrollTop by mutableIntStateOf(chrome.scrollTopRequest)
    var consumedBatch by mutableIntStateOf(chrome.batchRequest)
    var consumedSearch by mutableIntStateOf(chrome.searchRequest)
    var consumedBatchAction by mutableIntStateOf(chrome.batchActionRequest)
    var scrollTopTick by mutableIntStateOf(0)
    /** 长按视频弹出的操作面板目标；null 表示不显示。 */
    var sheetVideo by mutableStateOf<VideoItem?>(null)
}

/** 单个分类页的「回到顶部」消费进度。 */
internal class LibraryGridPresentationStateHolder(tick: Int) { var seenScrollTop by mutableIntStateOf(tick) }
