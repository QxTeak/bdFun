package com.aritxonly.bdfun.ui

import androidx.compose.foundation.pager.PagerState
import com.aritxonly.bdfun.feature.video.presentation.observeVideoFeatureState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun VideoDetailEntry(
    isLoggedIn: () -> Boolean, request: VideoScreenRequest, presentation: VideoScreenPresentation, events: VideoScreenEvents,
    pager: PagerState, ports: VideoDetailScreenPorts,
) {
    val holders = rememberVideoScreenStateHolders(request, ports.state)
    val collected = observeVideoFeatureState(holders.controllers.detail, holders.controllers.selection,
        holders.controllers.interaction, holders.metadata)
    val (detail, selection, interaction, metadata) = collected
    // 只更新当前标识；真正退出时仍由 Activity 清理读取缓存。
    LaunchedEffect(selection.bvid) { ports.publishBvid(selection.bvid) }
    val playing by holders.ui.playing
    val qualities = detail.playInfo?.qualities.orEmpty()
    val selectedQuality = if (selection.autoQuality) {
        ports.automaticQuality.select(qualities)
    } else {
        qualities.firstOrNull { it.id == selection.quality } ?: qualities.firstOrNull()
    }
    // 半屏只起 720P：小窗里 4K/HDR 没有意义，还会白拉高码率；全屏再恢复用户档位。
    val quality = if (presentation.fullscreen) selectedQuality
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
