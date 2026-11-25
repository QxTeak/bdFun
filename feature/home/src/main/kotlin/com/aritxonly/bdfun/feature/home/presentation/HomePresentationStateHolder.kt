package com.aritxonly.bdfun.feature.home.presentation

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import com.aritxonly.bdfun.core.model.video.VideoItem

internal class HomePresentationStateHolder {
    var actionVideo by mutableStateOf<VideoItem?>(null)
    /** 长按/省略号所在卡片的窗口坐标；长按菜单据此定位。 */
    var actionAnchor by mutableStateOf(Rect.Zero)
    /** 省略号走玻璃形变菜单（和收藏页「更多」一致）；长按仍用矩形锚点菜单。 */
    val menuState = com.aritxonly.teak.common.navigation.TeakMorphMenuState()
    var moreViaLongPress by mutableStateOf(false)
    var pullOffset by mutableFloatStateOf(0f)
}
