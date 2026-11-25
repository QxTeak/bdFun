package com.aritxonly.bdfun.feature.home.presentation

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.aritxonly.bdfun.core.model.video.VideoItem

/** 共享展示/平台能力由壳装配；模块不感知卡片导航实现或进程诊断单例。 */
@Immutable
class HomePresentationHost(
    val topBarHeight: Dp,
    val gridCells: (Dp, Dp) -> GridCells,
    val card: @Composable (
        VideoItem, Boolean, () -> Unit, (Rect) -> Unit, (Rect) -> Unit,
        com.aritxonly.teak.common.navigation.TeakMorphMenuState?, (() -> Unit)?,
    ) -> Unit,
    val loading: @Composable (Color, Dp) -> Unit,
    val onMessage: (String) -> Unit,
    val onEnter: () -> Unit,
    val onExit: () -> Unit,
)
