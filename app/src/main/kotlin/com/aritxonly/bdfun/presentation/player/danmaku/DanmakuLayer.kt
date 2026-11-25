package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import com.aritxonly.bdfun.core.domain.hotDanmakuReservationLeadMs
import com.aritxonly.bdfun.core.domain.DanmakuRuleMatcher
import com.aritxonly.bdfun.feature.video.application.DanmakuModerationState
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.android.purebilibili.danmaku.engine.DanmakuRenderView
import com.aritxonly.bdfun.core.model.video.DanmakuItem as BiliDanmakuItem

import com.aritxonly.bdfun.core.domain.filterDanmaku

@Composable
fun DanmakuLayer(items: List<BiliDanmakuItem>, positionMs: () -> Long, playing: Boolean,
    playbackSpeed: Float, modifier: Modifier = Modifier, fullscreen: Boolean = false,
    hotPresentation: HotDanmakuPresentation, hit: PlayerDanmakuHit) {
    if (items.isEmpty()) return
    val context = LocalContext.current
    val renderView = remember { DanmakuRenderView(context).apply { setRendererTouchable(false) } }
    DisposableEffect(renderView) { onDispose { renderView.releaseRenderer() } }
    val clickCoordinates = BindDanmakuClick(renderView, hit)
    val ui = remember { DanmakuLayerPresentationStateHolder() }
    val options = currentDanmakuLayerOptions(fullscreen)
    val page = LocalVideoPageState.current
    val moderation = page.danmakuModeration?.state?.collectAsState()?.value ?: DanmakuModerationState()
    val ownerHash = remember(page.danmakuOwnerMid) {
        com.aritxonly.bdfun.core.domain.danmakuUserHash(page.danmakuOwnerMid).ifEmpty { null }
    }
    LaunchedEffect(renderView, ownerHash) { renderView.engine.upOwnerUserHash = ownerHash }
    val matcher = remember(moderation.rules) { DanmakuRuleMatcher(moderation.rules) }
    // 分段弹幕：详情只带回前两段，播放推进到新的一段时由这里补齐当前段前后。
    val feed = page.danmakuFeed
    val feedRevision = if (feed != null) feed.revision.collectAsState().value else 0L
    val cid = page.cid
    LaunchedEffect(cid, items, feed) { feed?.seed(cid, items) }
    LaunchedEffect(cid, feed) {
        if (feed != null) {
            snapshotFlow { positionMs().coerceAtLeast(0L) }
                .distinctUntilChanged { old, new -> old / 10_000L == new / 10_000L }
                .collect { feed.ensure(cid, it) }
        }
    }
    val merged = remember(items, feedRevision, cid, feed) {
        if (feed == null) items else mergeDanmaku(items, feed.items(cid))
    }
    val filtered = remember(merged, options.filters, matcher, moderation.recalled, page.cid) {
        val recalled = moderation.recalled
        val kept = ArrayList<BiliDanmakuItem>(merged.size)
        for (item in merged) {
            if (matcher.blocks(item)) continue
            if (recalled.isNotEmpty() && (page.cid to item.id) in recalled) continue
            kept += item
        }
        filterDanmaku(kept, options.filters)
    }
    LaunchedEffect(hit, filtered) { hit.filteredSnapshot.value = filtered }
    DisposableEffect(hit) { onDispose { hit.filteredSnapshot.value = emptyList() } }
    val likeAction = rememberScrollingDanmakuLike(hit, page.cid)
    PrefetchScrollingDanmakuVotes(filtered, page.cid, positionMs, hit)
    val window = rememberDanmakuEngineWindow(filtered, likeAction)
    // 只把本机发送的条目补进渲染器；服务端弹幕仍走正常时间线。
    val selfItems = remember(merged) { merged.filter { it.self } }
    val config = danmakuEngineConfig(options.drawing, ui.viewSize, playbackSpeed)
    val masks = rememberDanmakuMasks(options.drawing.portraitGuard, ui.viewSize)
    val leadMs = hotDanmakuReservationLeadMs(
        if (options.filters.blockScroll) 0 else config.scrollDurationMs,
        if (options.filters.blockFixed) 0 else config.pinnedDurationMs,
        playbackSpeed,
    )
    SideEffect { hit.hotReservationLeadMs.longValue = leadMs }
    AndroidView(factory = { renderView }, modifier = modifier.then(clickCoordinates)
        .onSizeChanged { ui.viewSize = it },
        update = { view -> view.alpha = options.drawing.opacity.coerceIn(0f, 1f) })
    BindDanmakuEngineEffects(renderView, window, masks, config, playing, positionMs, options.drawing.lowRefresh,
        hotPresentation.reservedTopPx.toFloat(), selfItems)
}

/** 合并分段弹幕与详情首屏弹幕：按 id 去重，无 id 的本地条目按内容去重后统一按时间排序。 */
private fun mergeDanmaku(
    items: List<BiliDanmakuItem>,
    feedItems: List<BiliDanmakuItem>,
): List<BiliDanmakuItem> {
    val capacity = feedItems.size + items.size
    val out = ArrayList<BiliDanmakuItem>(capacity)
    val ids = HashSet<Long>(capacity)
    val texts = HashSet<String>(capacity)
    fun add(item: BiliDanmakuItem) {
        if (item.id > 0L) {
            if (ids.add(item.id)) out += item
        } else if (texts.add("${item.timeMs}:${item.mode}:${item.text}")) {
            out += item
        }
    }
    for (item in feedItems) add(item)
    for (item in items) add(item)
    out.sortBy { it.timeMs }
    return out
}
