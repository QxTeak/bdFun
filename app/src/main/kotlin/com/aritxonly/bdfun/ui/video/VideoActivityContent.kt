package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun VideoActivityContent(
    isLoggedIn: () -> Boolean, settings: com.aritxonly.bdfun.feature.settings.presentation.SettingsBindings,
    route: com.aritxonly.bdfun.core.navigation.Route.Video?, requestedBvid: String?, requestedAid: Long,
    pipMode: Boolean, host: VideoActivityHostActions,
) {
    val page = LocalVideoPageState.current
    val video = rememberVideoActivityEntry(route?.snapshot, requestedBvid, requestedAid, host, page.dependencies.controllers) ?: return
    val ui = remember { VideoActivityPageStateHolder(page.autoNextRequest) }
    BindVideoActivityQueue(video, ui, host, page.dependencies.queue) { page.autoNextRequest }
    val pipWindow = pipMode
    val fullscreen = page.playerUi.fullscreen || pipWindow
    val alpha = rememberManualProgress(if (fullscreen) 0f else 1f, 200f)
    val commentRequest = remember { Triple(route?.commentRoot ?: 0L, route?.commentTarget ?: 0L, route?.seekMs ?: 0L) }
    val pager = androidx.compose.foundation.pager.rememberPagerState(
        initialPage = if (commentRequest.first > 0L) 1 else 0, pageCount = { 2 },
    )
    val scope = rememberCoroutineScope()
    val screenWidthDp = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp
    val statusBarDp = rememberStatusBarTop()
    val playerHeightDp = (screenWidthDp * 9f / 16f).dp
    val immersiveStatus = settings.videoImmersiveStatusBar
    val playerTopInset = if (immersiveStatus) 0.dp else statusBarDp
    BindVideoActivityWindow(immersiveStatus, ui, host)
    // Tab 玻璃只盖在播放器下方，采不到视频像素；单独给它一份“播放器以下内容”的底片，
    // 避免整个视频层被卷进离屏合成。渐变遮罩仍在录制层里，模糊结果和原来一致。
    val contentBackground = com.aritxonly.teak.common.theme.LocalTeakContentColors.current.background
    val contentBackdrop = top.yukonga.miuix.kmp.blur.rememberLayerBackdrop {
        drawRect(contentBackground)
        drawContent()
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val paneLayout = resolveVideoPaneLayout(maxWidth.value, maxHeight.value)
        val layout = VideoActivityLayout(paneLayout, statusBarDp, fullscreen, pipMode, alpha, playerTopInset)
        androidx.compose.runtime.SideEffect { host.syncSystemBars(layout.splitPanes) }
        TeakActivityScaffold(
            settings = settings,
            onBack = { host.onBack() },
            options = ActivityPageOptions(
                backRevealed = false,
                showBackButton = false,
                showTopBar = false,
                // 全屏时评论页不可见，回到顶部控件也必须一起消失。
                showScrollTop = !pipMode && !fullscreen,
            ),
            disc = ActivityDiscOptions(
                showBackgroundDisc = false,
            ),
            slots = ActivityPageSlots(
                overlay = { VideoActivityOverlay(layout, ui, pager, scope, contentBackdrop) },
            ),
        ) {
            VideoActivityBody(isLoggedIn, settings,
                VideoActivityScene(video, commentRequest, route?.epId ?: 0L, route?.seasonId ?: 0L),
                layout, ui, pager, scope, host,
                contentBackdrop = contentBackdrop)
        }
    }
}
