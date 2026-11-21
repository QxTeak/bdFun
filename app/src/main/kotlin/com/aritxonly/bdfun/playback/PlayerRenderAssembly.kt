package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.BrandPink
import com.aritxonly.bdfun.cast.CastDialog
import com.aritxonly.bdfun.cast.CastRequestState
import com.aritxonly.bdfun.feature.video.presentation.VideoPlaybackPlatform

/** 平台动作/资源按原页面装配；各getter与slot保留原条件分支中的读取时机。 */
@Composable
internal fun rememberPlayerRenderPorts(
    playback: VideoPlaybackPlatform, media: VideoMediaInput, presentation: VideoPlayerPresentation,
    actions: VideoPlayerActions, ui: VideoPlayerUiStateHolder,
): PlayerRenderPorts {
    val context = LocalContext.current
    val page = LocalVideoPageState.current
    val settings = page.settings
    val viewer = com.aritxonly.bdfun.AppGraph.session.value
    val fsActions = VideoFullscreenActionBindings(context, playback, presentation.fullscreenInfo, actions, ui, page).create()
    // 全屏/半屏共用 onCast 写请求状态，这里统一挂一个投屏面板，避免复制第二份入口。
    val castUrl = CastRequestState.url
    CastDialog(visible = castUrl != null, url = castUrl, onDismiss = CastRequestState::clear)
    return PlayerRenderPorts(
        page = page,
        gestures = PlayerGestureOptions({ settings.gestureBrightnessVolume }, { settings.boostDefaultSpeed },
            { settings.gestureCenterFullscreen }, { settings.gestureBoostSwipe },
            { settings.vibrationEnabled }),
        fullscreenActions = fsActions,
        picture = VideoPictureSettings(
            settings.progressBarMode,
            settings.hotDanmakuEnabled,
            BrandPink,
            settings.danmakuStroke,
            hotDanmakuTextScale = if (presentation.fullscreen) settings.danmakuScaleFullscreen else settings.danmakuScaleInline,
            danmakuOpacity = settings.danmakuOpacity,
        ),
        surface = rememberPlayerSurfaceSlots(),
        buffering = { modifier -> TvBuffering(modifier = modifier, tvSize = 26.4.dp) },
        cover = { cover, modifier -> InlinePlayerCover(cover, modifier) },
        inlineAppearance = ::inlinePlayerAppearance,
        fullscreenVisuals = {
            val chrome = rememberFullscreenChromeAssembly()
            PlayerFullscreenVisuals(chrome.appearance, chrome.slots, settings.danmakuOn, settings.backgroundPlay)
        },
        danmakuComposer = PlayerDanmakuComposePorts(
            { page.styledDanmakuSender },
            allowVipColor = viewer?.vip == true,
            allowUpIdentity = viewer != null && viewer.mid > 0 && viewer.mid == page.danmakuOwnerMid,
        ),
        danmakuOverlays = { visible ->
            if (visible && !settings.danmakuBlockAdvanced && !ui.fsAudioOnly.value) {
                SpecialVideoDanmakuOverlay(media.danmaku, playback, ui, page.aid, presentation.fullscreenInfo.currentCid)
                if (settings.videoInteraction) {
                    DanmakuCommandOverlay(playback, page.aid, presentation.fullscreenInfo.currentCid,
                        actions.interaction, presentation.fullscreenInfo.liked)
                }
            }
            DanmakuSelectionActions(playback, presentation.fullscreenInfo.currentCid)
        },
        inlineMore = { PlayerInlineMore(playback, media, presentation, actions, fsActions, ui) },
    )
}
