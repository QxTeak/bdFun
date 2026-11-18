package com.aritxonly.bdfun.ui

import android.widget.Toast
import androidx.compose.runtime.LaunchedEffect
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
    var voteJob: kotlinx.coroutines.Job? = null
}

@Composable
internal fun BindDanmakuClick(view: DanmakuRenderView, hit: PlayerDanmakuHit): Modifier {
    val page = LocalVideoPageState.current
    val scope = rememberCoroutineScope()
    val layout = remember(view) { DanmakuClickLayout() }
    LaunchedEffect(page.cid, page.danmakuVotes) {
        hit.votes.value = emptyMap()
        page.danmakuVotes?.collect { votes -> hit.votes.value = votes.filterKeys { it.first == page.cid }.mapKeys { it.key.second } }
    }
    DisposableEffect(view, hit, page.cid) {
        val activeCid = page.cid
        hit.loadVotes = { ids -> if (page.cid == activeCid) page.danmakuVoteLoader?.invoke(ids) }
        hit.sendSame = { text -> page.cid == activeCid && page.danmakuSender?.invoke(text, 1, 25, 0xFFFFFF) == true }
        hit.like = { id ->
            if (page.cid == activeCid) scope.launch {
                val ok = page.danmakuLiker?.invoke(id) == true
                if (page.cid == activeCid && !ok) Toast.makeText(view.context, "操作失败，请确认已登录", Toast.LENGTH_SHORT).show()
            }
        }
        // 单击确认由播放器负责，原生 View 不再抢走拖动、双击和长按。
        view.setRendererTouchable(false)
        hit.capture = capture@{ window ->
            val coordinates = layout.coordinates?.takeIf { it.isAttached } ?: return@capture null
            val point = coordinates.windowToLocal(window)
            if (point.x < 0 || point.y < hit.hotPresentation.value.heightPx || point.x >= coordinates.size.width || point.y >= coordinates.size.height) return@capture null
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
                layout.menu = DanmakuActionPopup(view) {
                    layout.voteJob?.cancel()
                    view.engine.clearClickSelection()
                }.apply {
                    show(item.text.orEmpty(), point?.x?.toInt() ?: 0, point?.y?.toInt() ?: 0,
                        liked = hit.votes.value[id]?.liked == true,
                        count = hit.votes.value[id]?.likes ?: item.likeCount,
                        like = {
                            if (page.cid == cid) scope.launch {
                                val ok = liker(id)
                                if (page.cid == cid) Toast.makeText(view.context,
                                    if (ok) "操作成功" else "点赞失败，请确认已登录", Toast.LENGTH_SHORT).show()
                            }
                        }, report = { reason, content ->
                            if (page.cid == cid && reporter != null) scope.launch {
                                val ok = reporter(id, reason, content)
                                if (page.cid == cid) Toast.makeText(view.context,
                                    if (ok) "举报已提交" else "举报失败，请稍后重试", Toast.LENGTH_SHORT).show()
                            }
                        })
                    layout.voteJob = scope.launch {
                        launch { hit.loadVotes?.invoke(listOf(id)) }
                        hit.votes.collect { votes ->
                            val vote = votes[id]
                            showVote(vote?.liked == true, vote?.likes ?: item.likeCount)
                        }
                    }
                }
            } else view.engine.clearClickSelection()
        }
        onDispose {
            hit.capture = null
            hit.like = null
            hit.loadVotes = null
            hit.sendSame = null
            hit.votes.value = emptyMap()
            layout.menu?.close()
            view.engine.clearClickSelection()
            view.engine.setOnItemClickListener(null)
        }
    }
    return Modifier.onGloballyPositioned { layout.coordinates = it }
}
