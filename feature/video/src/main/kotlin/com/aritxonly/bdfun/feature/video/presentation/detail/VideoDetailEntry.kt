package com.aritxonly.bdfun.ui

import androidx.compose.foundation.pager.PagerState
import com.aritxonly.bdfun.feature.video.presentation.observeVideoFeatureState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue

/**
 * 进全屏后延迟多久再升级清晰度。
 *
 * 转屏 + 全屏布局重排大约 300~500ms；留出余量让播放器和弹幕层先稳定下来，
 * 避免「布局重建 + 播放源重建」挤在同一帧里。暂停时不等这个延时。
 */
private const val FullscreenQualityUpgradeDelayMs = 900L

@Composable
fun VideoDetailEntry(
    isLoggedIn: () -> Boolean, request: VideoScreenRequest, presentation: VideoScreenPresentation, events: VideoScreenEvents,
    pager: PagerState, ports: VideoDetailScreenPorts,
) {
    val holders = rememberVideoScreenStateHolders(request, ports.state)
    // 稍后再看入口：按当前 bvid 取本地合集，塞进详情后选集/连播都走合集系统。
    val localCollection = ports.localCollectionFor(request.video.bvid)
    androidx.compose.runtime.LaunchedEffect(request.video.bvid, localCollection.first) {
        holders.controllers.detail.setLocalCollection(localCollection.first, localCollection.second)
    }
    val collected = observeVideoFeatureState(holders.controllers.detail, holders.controllers.selection,
        holders.controllers.interaction, holders.metadata)
    val (detail, selection, interaction, metadata) = collected
    // 只更新当前标识；真正退出时仍由 Activity 清理读取缓存。
    LaunchedEffect(selection.bvid) { ports.publishBvid(selection.bvid) }
    // Tab 在 Activity 覆盖层里，拿不到 read.detail，评论数只能单向发布出去。
    // 换集/PGC 换集/缓存恢复都会替换 detail，这里跟着刷。
    val commentCount = detail.detail?.reply ?: 0L
    LaunchedEffect(commentCount) { events.onCommentCount(commentCount) }
    val playing by holders.ui.playing
    val qualities = detail.playInfo?.qualities.orEmpty()
    val selectedQuality = if (selection.autoQuality) {
        ports.automaticQuality.select(qualities)
    } else {
        qualities.firstOrNull { it.id == selection.quality } ?: qualities.firstOrNull()
    }
    // 半屏只起 720P：小窗里 4K/HDR 没有意义，还会白拉高码率。
    // 全屏不再「一进去就换高清」：转屏/全屏布局本身就要重建一遍，同一个窗口里再
    // 重建播放源（新 MediaSource 会重开解码器）会叠成一次明显卡顿，音频也会断一下。
    // 现在先让 720P 继续播，等全屏布局稳定（暂停时直接换，没有音频断点）再升级；
    // 升级后回半屏保持这个清晰度，不再降回 720P。
    var enteredFullscreen by remember(selection.bvid) { mutableStateOf(false) }
    LaunchedEffect(presentation.fullscreen, selection.bvid, playing) {
        if (!presentation.fullscreen || enteredFullscreen) return@LaunchedEffect
        if (!playing) {
            // 暂停时没有声音，换源听不出来，可以立刻升级。
            enteredFullscreen = true
            return@LaunchedEffect
        }
        kotlinx.coroutines.delay(FullscreenQualityUpgradeDelayMs)
        enteredFullscreen = true
    }
    val quality = if (enteredFullscreen) selectedQuality
        else com.aritxonly.bdfun.core.domain.capInlineQuality(selectedQuality, qualities)
    // 用当前清晰度的真实宽高比判断竖屏视频；读不到就按原来的 16:9 处理。
    val videoAspectRatio = quality?.let { q ->
        if (q.width > 0 && q.height > 0) q.width.toFloat() / q.height.toFloat() else null
    }
    val inlineLayout = rememberVideoInlineLayout(selection.bvid, playing, pager.currentPage,
        presentation.paneLayout, presentation.immersiveStatusBar, presentation.statusBarTop, ports.collapse,
        videoAspectRatio)
    val scope = rememberCoroutineScope()
    val actions = rememberVideoDetailActions(isLoggedIn, request, holders, scope, events.onBack, ports.actionHost)
    val content = VideoScreenContent(request.video, detail, selection, interaction, metadata, quality, isLoggedIn())
    BindVideoDetailEffects(request, content, holders, actions, pager, ports.load, ports.effects, presentation.autoRotate)
    ports.slots.layout(VideoDetailLayoutContent(request, presentation, events, pager, inlineLayout,
        holders, content, actions, scope))
    ports.slots.sheets(holders.ui, interaction, actions)
}
