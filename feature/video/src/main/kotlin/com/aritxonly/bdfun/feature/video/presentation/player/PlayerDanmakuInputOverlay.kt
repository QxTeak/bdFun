package com.aritxonly.bdfun.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import com.aritxonly.bdfun.core.model.video.DanmakuItem

@Composable
internal fun BoxScope.PlayerDanmakuInputOverlay(
    fullscreen: Boolean, danmaku: List<DanmakuItem>, ui: VideoPlayerUiStateHolder,
    composer: PlayerDanmakuComposePorts,
) {
    var fsInputOpen by ui.fsInputOpen
    // 全屏弹幕输入：点输入框后从底部弹出，和竖屏共用同一套发送器
    if (fullscreen && fsInputOpen) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.78f)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        fsInputOpen = false
                        while (true) {
                            val event = awaitPointerEvent()
                            event.changes.forEach { it.consume() }
                            if (event.changes.none { it.pressed }) break
                        }
                    }
                },
        )
        FullscreenDanmakuInput(
            onDismiss = { fsInputOpen = false },
            items = danmaku,
            composer = composer,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
        )
    }

}
