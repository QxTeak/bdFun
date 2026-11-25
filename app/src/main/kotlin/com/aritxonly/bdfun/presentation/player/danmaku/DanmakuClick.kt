package com.aritxonly.bdfun.ui

import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import com.android.purebilibili.danmaku.engine.DanmakuRenderView
import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.common.messageOr
import com.aritxonly.bdfun.core.model.video.DanmakuItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private data class DanmakuSelection(val item: DanmakuItem, val position: Offset)

private class DanmakuClickLayout {
    var coordinates: LayoutCoordinates? = null
    var selection by mutableStateOf<DanmakuSelection?>(null)
    var reporting by mutableStateOf(false)

    fun clear(view: DanmakuRenderView) {
        selection = null
        reporting = false
        view.engine.clearClickSelection()
    }
}

@Composable
internal fun BindDanmakuClick(view: DanmakuRenderView, hit: PlayerDanmakuHit): Modifier {
    val page = LocalVideoPageState.current
    val scope = rememberCoroutineScope()
    val cid = page.cid
    val layout = remember(view, cid) { DanmakuClickLayout() }
    LaunchedEffect(cid, page.danmakuVotes) {
        hit.votes.value = emptyMap()
        page.danmakuVotes?.collect { votes ->
            hit.votes.value = votes.filterKeys { it.first == cid }.mapKeys { it.key.second }
            view.engine.invalidate()
        }
    }
    DisposableEffect(view, hit, cid) {
        bindDanmakuVoteActions(page, hit, scope, view.context)
        view.setRendererTouchable(false)
        hit.capture = capture@{ window ->
            val coordinates = layout.coordinates?.takeIf { it.isAttached } ?: return@capture null
            val point = coordinates.windowToLocal(window)
            if (point.x < 0 || point.y < hit.hotPresentation.value.heightPx ||
                point.x >= coordinates.size.width || point.y >= coordinates.size.height) return@capture null
            layout.clear(view)
            val captured = view.engine.captureClick(point.x, point.y) ?: return@capture null
            PlayerDanmakuPress({ captured() }, { captured.cancel() })
        }
        view.engine.setOnItemClickListener { item, rect, _ ->
            val position = layout.coordinates?.localToWindow(Offset(rect.centerX(), rect.bottom))
            if (position != null && item.danmakuId > 0 && page.danmakuLiker != null) {
                layout.selection = DanmakuSelection(DanmakuItem(
                    item.showAtTime, 1, 0xFFFFFF, 25f,
                    item.mergedOriginalText ?: item.text.orEmpty(), item.danmakuId, item.likeCount,
                    self = item.isSelf, userHash = item.userHash,
                ), position)
            } else view.engine.clearClickSelection()
        }
        onDispose {
            hit.capture = null
            hit.like = null
            hit.loadVotes = null
            hit.sendSame = null
            hit.votes.value = emptyMap()
            layout.clear(view)
            view.engine.setOnItemClickListener(null)
        }
    }
    BindDanmakuSelection(layout, view, page, hit, cid)
    return Modifier.onGloballyPositioned { layout.coordinates = it }
}

@Composable
private fun BindDanmakuSelection(layout: DanmakuClickLayout, view: DanmakuRenderView,
    page: VideoPagePresentationState, hit: PlayerDanmakuHit, cid: Long) {
    val selection = layout.selection ?: return
    val item = selection.item
    val votes by hit.votes.collectAsState()
    val vote = votes[item.id]
    val context = view.context
    val dismiss = { layout.clear(view) }
    LaunchedEffect(item.id, cid) { hit.loadVotes?.invoke(listOf(item.id)) }
    if (layout.reporting) {
        DanmakuReportDialog(item, onDismiss = dismiss) { reason, text ->
            if (page.cid != cid) return@DanmakuReportDialog false
            val result = page.danmakuReporter?.invoke(item.id, reason, text)
                ?: Outcome.Failure(com.aritxonly.bdfun.core.common.AppError.Unknown("弹幕举报入口已关闭"))
            context.showToast(result.toReportMessage())
            result is Outcome.Success
        }
    } else DanmakuActionPopup(selection.position, vote?.liked == true, vote?.likes ?: item.likeCount, item, dismiss,
        onLike = { hit.like?.invoke(item.id) },
        onCopy = {
            val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("弹幕", item.text))
            dismiss()
        },
        onReport = if (item.self) null else { { layout.reporting = true } },
        onMore = { action ->
            dismiss()
            page.pendingDanmakuAction = DanmakuActionRequest(cid, item, action)
        })
}

private fun bindDanmakuVoteActions(page: VideoPagePresentationState, hit: PlayerDanmakuHit,
    scope: CoroutineScope, context: android.content.Context) {
    val activeCid = page.cid
    hit.loadVotes = { ids -> if (page.cid == activeCid) page.danmakuVoteLoader?.invoke(ids) }
    hit.sendSame = { text -> page.cid == activeCid && page.danmakuSender?.invoke(text, 1, 25, 0xFFFFFF) == true }
    hit.like = { id ->
        if (page.cid == activeCid) scope.launch {
            val result = page.danmakuLiker?.invoke(id)
                ?: Outcome.Failure(com.aritxonly.bdfun.core.common.AppError.Unknown("弹幕点赞入口已关闭"))
            if (page.cid == activeCid) Toast.makeText(context, result.toLikeMessage(), Toast.LENGTH_SHORT).show()
        }
    }
}

private fun Outcome<Boolean>.toLikeMessage(): String = when (this) {
    is Outcome.Success -> if (value) "已点赞" else "已取消点赞"
    is Outcome.Failure -> error.messageOr("点赞失败")
}

private fun Outcome<Unit>.toReportMessage(): String = when (this) {
    is Outcome.Success -> "举报已提交"
    is Outcome.Failure -> error.messageOr("举报失败")
}
