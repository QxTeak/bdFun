package com.aritxonly.bdfun.ui

import android.widget.PopupMenu
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
    var menu: PopupMenu? = null
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
            val captured = view.engine.captureClick(point.x, point.y) ?: return@capture null
            PlayerDanmakuPress({ captured() }, { captured.cancel() })
        }
        view.engine.setOnItemClickListener { item, _, _ ->
            val id = item.danmakuId
            val liker = page.danmakuLiker
            val cid = page.cid
            if (id > 0 && liker != null) {
                layout.menu?.dismiss()
                layout.menu = PopupMenu(view.context, view).apply {
                    menu.add("点赞弹幕：${item.text}").setOnMenuItemClickListener {
                        if (page.cid == cid) scope.launch {
                            val ok = liker(id)
                            if (page.cid == cid) Toast.makeText(view.context,
                                if (ok) "已点赞" else "点赞失败，请确认已登录", Toast.LENGTH_SHORT).show()
                        }
                        true
                    }
                    setOnDismissListener { view.engine.clearClickSelection() }
                    show()
                }
            } else view.engine.clearClickSelection()
        }
        onDispose {
            hit.capture = null
            layout.menu?.dismiss()
            view.engine.clearClickSelection()
            view.engine.setOnItemClickListener(null)
        }
    }
    return Modifier.onGloballyPositioned { layout.coordinates = it }
}
