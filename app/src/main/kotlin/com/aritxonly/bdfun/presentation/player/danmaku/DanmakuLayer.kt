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
        if (feed == null) items else (feed.items(cid) + items)
            .distinctBy { if (it.id > 0L) "id:${it.id}" else "${it.timeMs}:${it.mode}:${it.text}" }
            .sortedBy { it.timeMs }
    }
    val filtered = remember(merged, options.filters, matcher, moderation.recalled, page.cid) {
        filterDanmaku(merged.filterNot { matcher.blocks(it) || (page.cid to it.id) in moderation.recalled }, options.filters)
    }
    LaunchedEffect(hit, filtered) { hit.filteredSnapshot.value = filtered }
    DisposableEffect(hit) { onDispose { hit.filteredSnapshot.value = emptyList() } }
    val likeAction = rememberScrollingDanmakuLike(hit, page.cid)
    PrefetchScrollingDanmakuVotes(filtered, page.cid, positionMs, hit)
    val window = rememberDanmakuEngineWindow(filtered, likeAction)
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
        hotPresentation.reservedTopPx.toFloat())
}
