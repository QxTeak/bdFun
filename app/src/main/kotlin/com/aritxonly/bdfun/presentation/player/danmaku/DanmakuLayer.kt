package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.android.purebilibili.danmaku.engine.DanmakuRenderView
import com.aritxonly.bdfun.core.model.video.DanmakuItem as BiliDanmakuItem

import com.aritxonly.bdfun.core.domain.filterDanmaku

@Composable
fun DanmakuLayer(items: List<BiliDanmakuItem>, positionMs: () -> Long, playing: Boolean,
    playbackSpeed: Float, modifier: Modifier = Modifier, fullscreen: Boolean = false,
    /** 顶部是否要让给「弹幕计数」；只在计数真的显示时为 true。 */
    reserveTopForCounter: Boolean = false) {
    if (items.isEmpty()) return
    val context = LocalContext.current
    val renderView = remember { DanmakuRenderView(context).apply { setRendererTouchable(false) } }
    DisposableEffect(renderView) { onDispose { renderView.releaseRenderer() } }
    val ui = remember { DanmakuLayerPresentationStateHolder() }
    val options = currentDanmakuLayerOptions(fullscreen, reserveTopForCounter)
    val filtered = remember(items, options.filters) { filterDanmaku(items, options.filters) }
    val window = rememberDanmakuEngineWindow(filtered)
    val config = danmakuEngineConfig(options.drawing, ui.viewSize, playbackSpeed)
    val masks = rememberDanmakuMasks(options.drawing.portraitGuard, ui.viewSize)
    AndroidView(factory = { renderView }, modifier = modifier.onSizeChanged { ui.viewSize = it },
        update = { view -> view.alpha = options.drawing.opacity.coerceIn(0f, 1f) })
    BindDanmakuEngineEffects(renderView, window, masks, config, playing, positionMs, options.drawing.lowRefresh)
}
