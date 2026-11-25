package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.aritxonly.teak.common.navigation.TeakMorphMenuState

@Composable
internal fun CommentRowMore(onMore: () -> Unit, morphState: TeakMorphMenuState? = null) {
    // 和视频卡片右下角完全同一颗 Miuix `More`：24dp 点击区、无涟漪；
    // 传入锚点时走 Activity 根部的形变菜单，从图标位置展开。
    VideoCardMoreButton(
        onClick = onMore,
        onCover = false,
        touchSize = 24.dp,
        iconSize = 17.5.dp,
        morphState = morphState,
        onMorphTrigger = onMore,
        morphAutoOpen = false,
    )
}
