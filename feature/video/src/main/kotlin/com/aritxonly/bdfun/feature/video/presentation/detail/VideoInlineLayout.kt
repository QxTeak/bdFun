package com.aritxonly.bdfun.ui

import com.aritxonly.bdfun.core.domain.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

data class VideoInlineLayout(
    val collapseState: InlinePlayerCollapseState,
    val expandedTopInset: androidx.compose.ui.unit.Dp,
    val expandedPlayerHeight: androidx.compose.ui.unit.Dp,
    val collapsedPlayerHeight: androidx.compose.ui.unit.Dp,
    val collapseConnection: NestedScrollConnection,
    val animatedPlayerHeight: Float,
    val playerBlockHeight: androidx.compose.ui.unit.Dp,
)

/**
 * 半屏播放器窗口的高度（dp）。
 *
 * 横屏视频/读不到宽高比时沿用 16:9；竖屏视频在 16:9 窗口里只能缩成中间一条，
 * 改成 3:4 让画面明显变大，再用窗口高度 60% 兜底，避免把小屏视频顶成整页。
 * 横屏分栏不经过这里（分栏直接铺满整列），所以横屏尺寸不受影响。
 */
fun resolveInlineVideoHeightDp(
    widthDp: Float,
    windowHeightDp: Float,
    videoAspectRatio: Float?,
): Float {
    val portrait = videoAspectRatio != null && videoAspectRatio in 0.05f..0.999f
    if (!portrait) return widthDp * 9f / 16f
    val tallBlock = widthDp * 4f / 3f
    val heightCap = windowHeightDp * 0.60f
    return if (heightCap > 0f) minOf(tallBlock, heightCap) else tallBlock
}

@Composable
fun rememberVideoInlineLayout(
    activeBvid: String, playing: Boolean, selectedTab: Int, paneLayout: VideoPaneLayout,
    immersiveStatusBar: Boolean, statusBarTop: androidx.compose.ui.unit.Dp, host: VideoCollapseHost,
    /** 当前清晰度画面的宽高比（宽/高）；竖屏视频 < 1，用于把半屏播放器按更满的比例铺开。 */
    videoAspectRatio: Float? = null,
): VideoInlineLayout {
    // ---- 播放器随浏览收起：完全照搬 BiliPai 的共享位移模型 ----
    // offsetPx 是唯一的状态，播放器高度、缩放、页面内边距都由它推导。
    val collapseDensity = LocalDensity.current
    // 竖屏视频改用更高的半屏窗口，横屏/读不到比例时保持原 16:9。
    val videoAspectHeight = resolveInlineVideoHeightDp(
        widthDp = paneLayout.widthDp,
        windowHeightDp = paneLayout.heightDp,
        videoAspectRatio = videoAspectRatio,
    ).dp
    val collapseState = rememberInlinePlayerCollapseState(activeBvid)
    // 「上滑收起播放器」设置已删除，固定按原来的「关闭」模式运行。
    val collapseMode = com.aritxonly.bdfun.core.model.video.PlayerCollapseMode.OFF
    // bdFun 的视频详情页原本固定 16:9 横屏画面；现在只在读出竖屏宽高比时换比例，
    // 播放器收起策略仍按横屏详情页处理，避免顺手改动收起行为。
    val isVerticalVideo = false
    val pausedForInlineLayout = !playing
    val commentThreadVisible = host.commentThreadVisible()
    val hidePausedPlayerForComments = shouldHidePausedInlinePlayerForComments(
        selectedTabIndex = selectedTab,
        isPlaybackPaused = pausedForInlineLayout,
        isCommentThreadVisible = commentThreadVisible,
    )
    val collapseEnabled = !paneLayout.isSplit && shouldEnableInlinePortraitScrollTransform(
        collapseMode = collapseMode,
        selectedTabIndex = selectedTab,
        isVerticalVideo = isVerticalVideo,
        isPlaybackPaused = pausedForInlineLayout,
        isCommentThreadVisible = commentThreadVisible,
    )
    // 整块播放器区域（含状态栏）在展开时的下边界。
    val expandedTopInset = if (immersiveStatusBar) 0.dp else statusBarTop
    val expandedPlayerHeight = videoAspectHeight + expandedTopInset
    // 最小时保留「状态栏 + 56dp 立即播放条」。
    val collapsedViewportHeightDp = resolveInlinePlayerCollapsedViewportHeightDp(
        standardCollapsedHeightDp = 56f,
        hidePausedPlayerForComments = hidePausedPlayerForComments,
        retainPlayToolbar = true,
    ).dp
    val collapsedPlayerHeight = statusBarTop + collapsedViewportHeightDp
    val collapseRangePx = with(collapseDensity) {
        (expandedPlayerHeight - collapsedPlayerHeight).coerceAtLeast(0.dp).toPx()
    }
    ReconcileVideoCollapse(activeBvid, playing, collapseEnabled, collapseRangePx, collapseState, host)
    val collapseConnection = rememberVideoCollapseConnection(collapseEnabled, collapseRangePx, collapseState, host)
    // 高度只由共享位移推导：跟手拖动 snap，恢复展开/范围校正用 220ms 补间。
    val targetPlayerHeight = (
        expandedPlayerHeight + with(collapseDensity) { collapseState.offsetPx.toDp() }
        ).coerceIn(collapsedPlayerHeight, expandedPlayerHeight)
    val animatedPlayerHeight by androidx.compose.animation.core.animateFloatAsState(
        targetValue = targetPlayerHeight.value,
        animationSpec = if (collapseState.restoreRequested || collapseState.layoutReconciled) {
            androidx.compose.animation.core.tween(
                durationMillis = 220,
                easing = androidx.compose.animation.core.FastOutSlowInEasing,
            )
        } else {
            androidx.compose.animation.core.snap()
        },
        label = "inline_player_collapse_height",
    )
    val playerBlockHeight = animatedPlayerHeight.dp
    return VideoInlineLayout(collapseState, expandedTopInset, expandedPlayerHeight, collapsedPlayerHeight,
        collapseConnection, animatedPlayerHeight, playerBlockHeight)
}

private class CollapsePauseStateHolder { var paused by mutableStateOf(false) }

@Composable
private fun ReconcileVideoCollapse(
    activeBvid: String, playing: Boolean, collapseEnabled: Boolean,
    collapseRangePx: Float, collapseState: InlinePlayerCollapseState, host: VideoCollapseHost,
) {
    // 切页/暂停让可收起范围变化时，把超界位移平滑收回。
    LaunchedEffect(collapseEnabled, collapseRangePx) {
        collapseState.reconcileLayout(
            collapseEnabled = collapseEnabled,
            collapseRangePx = collapseRangePx,
        )
    }
    // 收起浏览时自动暂停，展开后只恢复本功能触发的暂停。
    val pauseState = remember(activeBvid) { CollapsePauseStateHolder() }
    LaunchedEffect(playing) {
        if (playing) pauseState.paused = false
    }
    val collapsedForAutoPause = resolvePlayerCollapsedForAutoPause(
        swipeHidePlayerEnabled = collapseEnabled,
        playerHeightOffsetPx = collapseState.offsetPx,
        videoHeightPx = collapseRangePx,
        compactForCommentTab = false,
        restoreRequested = collapseState.restoreRequested,
    )
    LaunchedEffect(collapsedForAutoPause, collapseEnabled) {
        when {
            shouldAutoPauseOnPlayerCollapse(
                autoPauseEnabled = true,
                isPlayerCollapsed = collapsedForAutoPause,
                isPlaying = playing,
            ) -> {
                pauseState.paused = true
                host.pauseAll()
            }
            shouldAutoResumeOnPlayerExpand(
                autoPauseEnabled = true,
                isPlayerCollapsed = collapsedForAutoPause,
                wasAutoPausedByCollapse = pauseState.paused,
            ) -> {
                pauseState.paused = false
                host.togglePlayback()
            }
            !collapsedForAutoPause -> pauseState.paused = false
        }
    }
}

@Composable
private fun rememberVideoCollapseConnection(
    collapseEnabled: Boolean, collapseRangePx: Float, collapseState: InlinePlayerCollapseState, host: VideoCollapseHost,
): NestedScrollConnection {
    // 简介和评论共用同一个折叠手势；谁在滚动，都先收播放器，收满再把剩余距离交给列表。
    val collapseConnection = remember(collapseEnabled, collapseRangePx, collapseState) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val update = reduceVideoDetailPreScroll(
                    currentOffsetPx = collapseState.offsetPx,
                    deltaPx = available.y,
                    // 切页后位移可能暂时超出新范围；先允许它从当前位置回到合法范围。
                    minOffsetPx = minOf(-collapseRangePx, collapseState.offsetPx),
                    inlinePortraitScrollEnabled = collapseEnabled,
                    isPortraitFullscreen = false,
                )
                return Offset(0f, collapseState.consumeScroll(update))
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                val update = reduceVideoDetailPostScroll(
                    currentOffsetPx = collapseState.offsetPx,
                    deltaPx = available.y,
                    minOffsetPx = minOf(-collapseRangePx, collapseState.offsetPx),
                    inlinePortraitScrollEnabled = collapseEnabled,
                    isPortraitFullscreen = false,
                )
                return Offset(0f, collapseState.consumeScroll(update))
            }
        }
    }
    RegisterThreadCollapseScroll(collapseConnection, collapseEnabled, collapseRangePx, collapseState, host)
    return collapseConnection
}

@Composable
private fun RegisterThreadCollapseScroll(
    collapseConnection: NestedScrollConnection, collapseEnabled: Boolean,
    collapseRangePx: Float, collapseState: InlinePlayerCollapseState, host: VideoCollapseHost,
) {
    // 「展开回复」是独立 Dialog，拿不到这里的 nestedScroll，所以注册一个入口：
    // 它的列表往上划时，把同样的位移交给当前播放器，效果和主评论区一致。
    DisposableEffect(collapseConnection, collapseEnabled, collapseRangePx) {
        host.registerThreadScroll { deltaPx ->
            val update = if (deltaPx < 0f) {
                reduceVideoDetailPreScroll(
                    currentOffsetPx = collapseState.offsetPx,
                    deltaPx = deltaPx,
                    minOffsetPx = minOf(-collapseRangePx, collapseState.offsetPx),
                    inlinePortraitScrollEnabled = collapseEnabled,
                    isPortraitFullscreen = false,
                )
            } else {
                reduceVideoDetailPostScroll(
                    currentOffsetPx = collapseState.offsetPx,
                    deltaPx = deltaPx,
                    minOffsetPx = minOf(-collapseRangePx, collapseState.offsetPx),
                    inlinePortraitScrollEnabled = collapseEnabled,
                    isPortraitFullscreen = false,
                )
            }
            if (update == null) 0f else collapseState.consumeScroll(update)
        }
        onDispose { host.registerThreadScroll(null) }
    }
}
