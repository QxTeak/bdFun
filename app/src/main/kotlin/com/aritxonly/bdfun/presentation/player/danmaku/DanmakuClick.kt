package com.aritxonly.bdfun.ui

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import com.android.purebilibili.danmaku.engine.DanmakuRenderView
import kotlinx.coroutines.launch

private class DanmakuClickLayout {
    var coordinates: LayoutCoordinates? = null
    var menu: DanmakuActionPopup? = null
}

@Composable
internal fun BindDanmakuClick(view: DanmakuRenderView, hit: PlayerDanmakuHit): Modifier {
    val page = LocalVideoPageState.current
    val scope = rememberCoroutineScope()
    val layout = remember(view) { DanmakuClickLayout() }
    DisposableEffect(view, hit, page.cid) {
        // 单击确认由播放器负责，原生 View 不再抢走拖动、双击和长按。
        view.setRendererTouchable(false)
        hit.capture = capture@{ window ->
            val coordinates = layout.coordinates?.takeIf { it.isAttached } ?: return@capture null
            val point = coordinates.windowToLocal(window)
            if (point.x < 0 || point.y < 0 || point.x >= coordinates.size.width || point.y >= coordinates.size.height) return@capture null
            layout.menu?.close()
            layout.menu = null
            val captured = view.engine.captureClick(point.x, point.y) ?: return@capture null
            PlayerDanmakuPress({ captured() }, { captured.cancel() })
        }
        view.engine.setOnItemClickListener { item, rect, _ ->
            val id = item.danmakuId
            val liker = page.danmakuLiker
            val cid = page.cid
            if (id > 0 && liker != null) {
                val reporter = page.danmakuReporter
                val point = layout.coordinates?.localToWindow(androidx.compose.ui.geometry.Offset(rect.centerX(), rect.bottom))
                // 旧浮层必须在捕获新弹幕之前关闭，否则它的 dismiss 会释放新选择。
                layout.menu = DanmakuActionPopup(view) { view.engine.clearClickSelection() }.apply {
                    show(item.text.orEmpty(), point?.x?.toInt() ?: 0, point?.y?.toInt() ?: 0,
                        like = {
                            if (page.cid == cid) scope.launch {
                                val ok = liker(id)
                                if (page.cid == cid) Toast.makeText(view.context,
                                    if (ok) "已点赞" else "点赞失败，请确认已登录", Toast.LENGTH_SHORT).show()
                            }
                        }, report = { reason, content ->
                            if (page.cid == cid && reporter != null) scope.launch {
                                val ok = reporter(id, reason, content)
                                if (page.cid == cid) Toast.makeText(view.context,
                                    if (ok) "举报已提交" else "举报失败，请稍后重试", Toast.LENGTH_SHORT).show()
                            }
                        })
                }
            } else view.engine.clearClickSelection()
        }
        onDispose {
            hit.capture = null
            layout.menu?.close()
            view.engine.clearClickSelection()
            view.engine.setOnItemClickListener(null)
        }
    }
    return Modifier.onGloballyPositioned { layout.coordinates = it }
}
