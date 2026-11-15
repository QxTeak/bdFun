package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/** app仅装配播放器与评论等接缝，不新增测量节点。 */
class VideoDetailLayoutSlots(
    val pager: @Composable BoxScope.() -> Unit,
    val player: @Composable BoxScope.(VideoPlayerViewport) -> Unit,
    val loading: @Composable (Modifier, Color) -> Unit,
    val togglePlayback: () -> Unit, val accentColor: Color, val playIcon: Int,
    val onBack: () -> Unit,
)
class VideoCollapseHost(
    val commentThreadVisible: () -> Boolean,
    val pauseAll: () -> Unit,
    val togglePlayback: () -> Unit,
    val registerThreadScroll: (((Float) -> Float)?) -> Unit,
)
