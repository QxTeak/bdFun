package com.aritxonly.bdfun.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates

/** 以窗口坐标连接手势层和变换后的弹幕层，不向 feature 暴露渲染引擎。 */
class PlayerDanmakuHit {
    var coordinates: LayoutCoordinates? = null
    var capture: ((Offset) -> PlayerDanmakuPress?)? = null
    fun at(position: Offset): PlayerDanmakuPress? {
        val layout = coordinates?.takeIf { it.isAttached } ?: return null
        return capture?.invoke(layout.localToWindow(position))
    }
}

class PlayerDanmakuPress(val confirm: () -> Unit, val cancel: () -> Unit)
