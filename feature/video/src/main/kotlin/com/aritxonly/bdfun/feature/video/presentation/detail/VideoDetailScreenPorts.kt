package com.aritxonly.bdfun.ui

import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import com.aritxonly.bdfun.core.domain.AutomaticVideoQualitySelector
import com.aritxonly.bdfun.feature.video.application.VideoInteractionState
import kotlinx.coroutines.CoroutineScope

class VideoDetailScreenPorts(
    val state: VideoDetailStatePorts,
    val collapse: VideoCollapseHost,
    val automaticQuality: AutomaticVideoQualitySelector,
    val publishBvid: (String) -> Unit,
    val load: VideoDetailLoadPorts,
    val effects: VideoDetailEffectPorts,
    val actionHost: VideoDetailActionHost,
    val slots: VideoDetailScreenSlots,
)

class VideoDetailScreenSlots(
    val layout: @Composable (VideoDetailLayoutContent) -> Unit,
    val sheets: @Composable (VideoDetailUiStateHolder, VideoInteractionState, VideoDetailViewActions) -> Unit,
)

/** 同一帧的布局输入，避免跨模块Composable回调展开成过长的参数列表。 */
class VideoDetailLayoutContent(
    val request: VideoScreenRequest,
    val presentation: VideoScreenPresentation,
    val events: VideoScreenEvents,
    val pager: PagerState,
    val inlineLayout: VideoInlineLayout,
    val holders: VideoScreenStateHolders,
    val content: VideoScreenContent,
    val actions: VideoDetailViewActions,
    val scope: CoroutineScope,
)
