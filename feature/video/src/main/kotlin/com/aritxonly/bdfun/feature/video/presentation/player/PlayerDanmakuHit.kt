package com.aritxonly.bdfun.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates

/** 以窗口坐标连接手势层和变换后的弹幕层，不向 feature 暴露渲染引擎。 */
class PlayerDanmakuHit {
    val votes = kotlinx.coroutines.flow.MutableStateFlow<Map<Long, com.aritxonly.bdfun.core.domain.DanmakuVote>>(emptyMap())
    val filteredSnapshot = androidx.compose.runtime.mutableStateOf<List<com.aritxonly.bdfun.core.model.video.DanmakuItem>>(emptyList())
    val hotPresentation = androidx.compose.runtime.mutableStateOf(HotDanmakuPresentation())
    var loadVotes: (suspend (List<Long>) -> Unit)? = null
    var sendSame: (suspend (String) -> Boolean)? = null
    var like: ((Long) -> Unit)? = null
    var coordinates: LayoutCoordinates? = null
    var capture: ((Offset) -> PlayerDanmakuPress?)? = null
    fun at(position: Offset): PlayerDanmakuPress? {
        val layout = coordinates?.takeIf { it.isAttached } ?: return null
        val window = layout.localToWindow(position)
        return capture?.invoke(window)
    }
}

class PlayerDanmakuPress(val confirm: () -> Unit, val cancel: () -> Unit)
