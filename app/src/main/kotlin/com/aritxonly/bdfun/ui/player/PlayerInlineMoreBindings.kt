package com.aritxonly.bdfun.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.aritxonly.bdfun.core.application.PlaybackController
import com.aritxonly.bdfun.data.Quality
import com.aritxonly.teak.common.modal.TeakActionSheet

@Composable
internal fun PlayerInlineMore(
    playback: PlaybackController, media: VideoMediaInput, presentation: VideoPlayerPresentation,
    actions: VideoPlayerActions, fsActions: FullscreenActions, ui: VideoPlayerUiStateHolder,
) {
    val settings = LocalVideoPageState.current.settings
    val context = LocalContext.current
    val fullscreen = presentation.fullscreen
    var halfMoreOpen by ui.halfMoreOpen
    var speed by ui.speed
    var fsAutoPlayNext by ui.fsAutoPlayNext
    var fsLoopMode by ui.fsLoopMode
    var fsMirror by ui.fsMirror
    var fsSubtitlesOn by ui.fsSubtitlesOn
    var fsSleepAt by ui.fsSleepAt
    // 弹幕设置的二级 sheet：从「更多」里打开，跟「更多」是两个独立面板。
    var danmakuSheetOpen by remember { mutableStateOf(false) }
    LaunchedEffect(fullscreen) { if (fullscreen) danmakuSheetOpen = false }
    // 半屏「更多」：与全屏 MorePanel 同一套设置项，复用评论区同款底部弹簧面板。
    if (!fullscreen) {
        HalfScreenMoreSheet(
            visible = halfMoreOpen,
            accentColor = com.aritxonly.bdfun.BrandPink,
            onDismiss = { halfMoreOpen = false },
            state = HalfMoreState(
                speed = speed,
                autoPlayNext = fsAutoPlayNext,
                loopMode = fsLoopMode,
                mirror = fsMirror,
                backgroundPlay = settings.backgroundPlay,
                subtitlesOn = fsSubtitlesOn,
                sleepAtMs = fsSleepAt,
            ),
            quick = inlineMoreQuickActions(context, media.quality, actions, fsActions, ui),
            settings = inlineMoreSettingsActions(playback, ui, settings) {
                danmakuSheetOpen = true
            },
        )
        // 面板用的是主题化表面，弹幕设置这套原本的白色配色要换成主题色，否则浅色下看不见。
        TeakActionSheet(
            visible = danmakuSheetOpen,
            onDismiss = { danmakuSheetOpen = false },
            title = "弹幕设置",
        ) {
            CompositionLocalProvider(LocalDanmakuPanelPalette provides themedDanmakuPanelPalette()) {
                DanmakuSettingsContent(
                    fullscreen = false,
                    onInteract = {},
                    danmakuItems = media.danmaku,
                ) { DanmakuCloudRulesEntry({}) }
            }
        }
    }
}

private fun inlineMoreQuickActions(
    context: Context, quality: Quality?, actions: VideoPlayerActions,
    fsActions: FullscreenActions, ui: VideoPlayerUiStateHolder,
): HalfMoreQuickActions {
    var halfMoreOpen by ui.halfMoreOpen
    val onDislikeClick = actions.interaction.onDislikeClick
    val onAddWatchLater = actions.interaction.onAddWatchLater
    val onCacheVideo = actions.transport.onCacheVideo
    return HalfMoreQuickActions(
        onDislike = {
            onDislikeClick()
            context.showToast("已减少此类推荐")
        },
        onWatchLater = {
            onAddWatchLater()
            halfMoreOpen = false
        },
        onPictureInPicture = {
            fsActions.onPictureInPicture()
            halfMoreOpen = false
        },
        onCast = {
            fsActions.onCast()
            halfMoreOpen = false
        },
        onCache = {
            val item = quality
            if (item == null) {
                context.showToast("暂无可用清晰度")
            } else {
                onCacheVideo(item)
            }
            halfMoreOpen = false
        },
    )
}

private fun inlineMoreSettingsActions(
    playback: PlaybackController, ui: VideoPlayerUiStateHolder,
    settings: com.aritxonly.bdfun.core.domain.SettingsRepository,
    onOpenDanmakuSettings: () -> Unit,
): HalfMoreSettingsActions {
    var speed by ui.speed
    var fsAutoPlayNext by ui.fsAutoPlayNext
    var fsLoopMode by ui.fsLoopMode
    var fsMirror by ui.fsMirror
    var fsSubtitlesOn by ui.fsSubtitlesOn
    var fsSleepAt by ui.fsSleepAt
    return HalfMoreSettingsActions(
        onSpeed = {
            speed = it
            playback.setSpeed(it)
        },
        onAutoPlayNext = { fsAutoPlayNext = !fsAutoPlayNext },
        onLoopMode = { fsLoopMode = it },
        onMirror = { fsMirror = !fsMirror },
        onBackgroundPlay = {
            settings.backgroundPlay = !settings.backgroundPlay
        },
        onSubtitlesToggle = { fsSubtitlesOn = !fsSubtitlesOn },
        onDanmakuSettings = onOpenDanmakuSettings,
        onSleepAt = { target ->
            fsSleepAt = target
        },
    )
}
