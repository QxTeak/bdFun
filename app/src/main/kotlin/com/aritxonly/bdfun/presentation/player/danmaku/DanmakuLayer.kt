package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.android.purebilibili.danmaku.engine.DanmakuRenderView
import com.aritxonly.bdfun.core.model.video.DanmakuItem as BiliDanmakuItem

import com.aritxonly.bdfun.core.domain.filterDanmaku

/** 弹幕计数占用的顶部高度：和计数的文字高度一致，只用来拦住新弹幕。 */
private val HotDanmakuGuardHeight = 44.dp

@Composable
fun DanmakuLayer(items: List<BiliDanmakuItem>, positionMs: () -> Long, playing: Boolean,
    playbackSpeed: Float, modifier: Modifier = Modifier, fullscreen: Boolean = false,
    /** 顶部是否要让给「弹幕计数」；只在计数真的显示时为 true。 */
    reserveTopForCounter: Boolean = false) {
    if (items.isEmpty()) return
    val context = LocalContext.current
    val renderView = remember { DanmakuRenderView(context).apply { setRendererTouchable(false) } }
    DisposableEffect(renderView) { onDispose { renderView.releaseRenderer() } }
    val page = LocalVideoPageState.current
    val scope = rememberCoroutineScope()
    DisposableEffect(renderView, playing, page.cid) {
        renderView.setRendererTouchable(!playing)
        renderView.engine.setOnItemClickListener { item, _, _ ->
            val id = item.danmakuId
            val liker = page.danmakuLiker
            val cid = page.cid
            if (id > 0 && liker != null && !playing) {
                android.widget.PopupMenu(context, renderView).apply {
                    menu.add("点赞弹幕：${item.text}").setOnMenuItemClickListener {
                        if (page.cid == cid) scope.launch {
                            val ok = liker(id)
                            android.widget.Toast.makeText(context, if (ok) "已点赞" else "点赞失败，请确认已登录", android.widget.Toast.LENGTH_SHORT).show()
                        }
                        true
                    }
                    show()
                }
            }
        }
        onDispose { renderView.engine.setOnItemClickListener(null) }
    }
    val ui = remember { DanmakuLayerPresentationStateHolder() }
    val options = currentDanmakuLayerOptions(fullscreen)
    val filtered = remember(items, options.filters) { filterDanmaku(items, options.filters) }
    val window = rememberDanmakuEngineWindow(filtered)
    val config = danmakuEngineConfig(options.drawing, ui.viewSize, playbackSpeed)
    val masks = rememberDanmakuMasks(options.drawing.portraitGuard, ui.viewSize)
    val density = LocalDensity.current
    val reservedTopPx = if (reserveTopForCounter) with(density) { HotDanmakuGuardHeight.toPx() } else 0f
    AndroidView(factory = { renderView }, modifier = modifier.onSizeChanged { ui.viewSize = it },
        update = { view -> view.alpha = options.drawing.opacity.coerceIn(0f, 1f) })
    BindDanmakuEngineEffects(renderView, window, masks, config, playing, positionMs, options.drawing.lowRefresh,
        reservedTopPx)
}
