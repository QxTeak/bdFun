package com.aritxonly.bdfun.feature.home.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.aritxonly.bdfun.core.model.video.VideoItem

internal class HomePresentationStateHolder {
    var actionVideo by mutableStateOf<VideoItem?>(null)
    /** 长按/省略号所在卡片的窗口坐标；长按菜单据此定位。 */
    var actionAnchor by mutableStateOf(Rect.Zero)
    /** 长按手指位置 + 专用形变锚点。 */
    var pressPoint by mutableStateOf(Offset.Zero)
    val pointMenuState = com.aritxonly.teak.common.navigation.TeakMorphMenuState()
    var openPointMenu by mutableStateOf(false)
    var pullOffset by mutableFloatStateOf(0f)
}
