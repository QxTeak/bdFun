package com.aritxonly.bdfun.ui

import com.aritxonly.bdfun.ui.FullscreenTransportActions

import com.aritxonly.bdfun.ui.FullscreenSocialActions

import com.aritxonly.bdfun.ui.FullscreenPreferenceActions

import com.aritxonly.bdfun.ui.FullscreenNavigationActions

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.util.Rational
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.aritxonly.bdfun.cast.CastRequestState
import com.aritxonly.bdfun.core.application.PlaybackController
import com.aritxonly.bdfun.data.Quality
import com.aritxonly.bdfun.data.VideoPage
import com.aritxonly.bdfun.data.SubtitleTrack

/** 每次组合投影当前页面动作；状态委托保持事件触发时读取最新值。 */
internal class VideoFullscreenActionBindings(
    private val context: Context,
    private val playback: PlaybackController,
    private val fullscreenInfo: FullscreenPlayerInfo,
    private val actions: VideoPlayerActions,
    private val ui: VideoPlayerUiStateHolder,
    private val VideoPageBus: VideoPagePresentationState,
    /** 杜比视界走直通表面，缩放/镜像/色觉辅助都作用不到画面，需要挡一下并提示。 */
    private val dolbyVision: () -> Boolean = { false },
) {
    private val settings get() = VideoPageBus.settings
    private val activity = context as? Activity
    private val PlayerUiState get() = VideoPageBus.playerUi
    private val playing get() = playback.state.value.playing
    private var fsPanel by ui.fsPanel
    private var fsInputOpen by ui.fsInputOpen
    private var speed by ui.speed
    private var fsAudioOnly by ui.fsAudioOnly
    private var controlsVisible by ui.controlsVisible
    private var fsAutoPlayNext by ui.fsAutoPlayNext
    private var fsLoopMode by ui.fsLoopMode
    private var fsMirror by ui.fsMirror
    private var fsResizeMode by ui.fsResizeMode
    private var fsColorAssist by ui.fsColorAssist
    private var fsSleepAt by ui.fsSleepAt

    private fun touch() { ui.interactionTick.intValue++ }

    private val onBack: () -> Unit = {
        if (fsPanel != FullscreenPanel.None) {
            fsPanel = FullscreenPanel.None
        } else {
            actions.navigation.onToggleFullscreen()
        }
    }

    private val onPlayPause: () -> Unit = {
        touch()
        togglePlay(playback, playing)
    }

    private val onNext: () -> Unit = {
        touch()
        val pages = fullscreenInfo.pages
        val index = pages.indexOfFirst { it.cid == fullscreenInfo.currentCid }
        val next = pages.getOrNull(index + 1)
        if (next == null) {
            context.showToast(
                if (pages.size <= 1) "下一集不存在" else "已经是最后一集",
            )
        } else {
            actions.transport.onPageSelected(next)
        }
    }

    private val onEpisodes: () -> Unit = {
        touch()
        if (fullscreenInfo.pages.size <= 1) {
            context.showToast("无合集")
        } else {
            fsPanel = FullscreenPanel.Episodes
        }
    }

    private val onLike: () -> Unit = {
        touch()
        actions.interaction.onLikeClick()
    }

    private val onComment: () -> Unit = {
        touch()
        actions.interaction.onCommentClick()
    }

    private val onShare: () -> Unit = {
        touch()
        actions.interaction.onShareClick()
    }

    private val onOpenSpace: () -> Unit = {
        touch()
        actions.interaction.onOpenSpaceClick()
    }

    private val onToggleFolder: (Long) -> Unit = actions.interaction.onToggleFolder

    private val onRequestFavorites: () -> Unit = actions.interaction.onRequestFavorites

    private val onSeek: (Long) -> Unit = { target ->
        touch()
        playback.seekTo(target)

    }

    private val onQualitySelected: (Int) -> Unit = { id ->
        touch()
        actions.transport.onQualitySelected(id)
    }

    private val onOpenCachePanel: () -> Unit = {
        touch()
        fsPanel = FullscreenPanel.Cache
    }

    private val onCacheSelected: (Quality) -> Unit = { quality ->
        touch()
        actions.transport.onCacheVideo(quality)
    }

    private val onSpeedSelected: (Float) -> Unit = { value ->
        touch()
        speed = value
        playback.setSpeed(value)
    }

    private val onPageSelected: (VideoPage) -> Unit = { page ->
        touch()
        actions.transport.onPageSelected(page)
    }

    private val onSubtitleSelected: (SubtitleTrack?) -> Unit = actions.transport.onSubtitleSelected

    private val onSendDanmaku: () -> Unit = {
        touch()
        fsInputOpen = true
    }

    private val onToggleDanmaku: () -> Unit = {
        touch()
        settings.danmakuOn = !settings.danmakuOn
        VideoPageBus.danmakuOn = settings.danmakuOn
    }

    private val onAddWatchLater: () -> Unit = {
        touch()
        actions.interaction.onAddWatchLater()
    }

    private val onPictureInPicture: () -> Unit = {
        runCatching {
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                activity?.enterPictureInPictureMode(
                    PictureInPictureParams.Builder()
                        .setAspectRatio(Rational(16, 9))
                        .build(),
                )
            }
        }
    }

    private val onCast: () -> Unit = {
        val mediaUrl = playback.currentMediaUrl
        if (mediaUrl.isNullOrBlank()) {
            context.showToast("当前没有可投屏的视频")
        } else {
            CastRequestState.request(mediaUrl)
        }
    }

    private val onAudioOnlyChanged: (Boolean) -> Unit = { enabled ->
        fsAudioOnly = enabled
        if (enabled) {
            fsPanel = FullscreenPanel.None
            controlsVisible = true
            PlayerUiState.controlsVisible = true
        }
    }

    private val onAutoPlayNextChanged: (Boolean) -> Unit = { fsAutoPlayNext = it }

    private val onLoopModeChanged: (Int) -> Unit = { fsLoopMode = it }

    private val onMirrorChanged: (Boolean) -> Unit = {
        if (dolbyVision()) context.showToast("杜比视界播放时暂不支持镜像翻转") else fsMirror = it
    }

    private val onResizeModeChanged: (Int) -> Unit = { fsResizeMode = it }

    private val onColorAssistChanged: (Int) -> Unit = {
        if (dolbyVision()) context.showToast("杜比视界播放时暂不支持色觉辅助") else fsColorAssist = it
    }

    private val onSleepAtChanged: (Long) -> Unit = { fsSleepAt = it }

    private val onBackgroundPlayChanged: (Boolean) -> Unit = { enabled ->
        settings.backgroundPlay = enabled
        context.showToast(if (enabled) "已开启后台听视频" else "已关闭后台听视频")
    }

    private val onSeekRelative: (Long) -> Unit = { delta ->
        val target = (playback.refresh().positionMs + delta)
            .coerceIn(0L, playback.refresh().durationMs.coerceAtLeast(0L))
        playback.seekTo(target)

    }

    fun create(): FullscreenActions = FullscreenActions(
        navigation = FullscreenNavigationActions(
            onBack = onBack,
            onNext = onNext,
            onEpisodes = onEpisodes,
            onComment = onComment,
            onOpenSpace = onOpenSpace,
            onOpenCachePanel = onOpenCachePanel,
            onPageSelected = onPageSelected,
            onPictureInPicture = onPictureInPicture,
            onCast = onCast,
        ),
        playback = FullscreenTransportActions(
            onPlayPause = onPlayPause,
            onSeek = onSeek,
            onQualitySelected = onQualitySelected,
            onCacheSelected = onCacheSelected,
            onSpeedSelected = onSpeedSelected,
            onSubtitleSelected = onSubtitleSelected,
            onToggleDanmaku = onToggleDanmaku,
            onSeekRelative = onSeekRelative,
        ),
        social = FullscreenSocialActions(
            onLike = onLike,
            onShare = onShare,
            onToggleFolder = onToggleFolder,
            onRequestFavorites = onRequestFavorites,
            onSendDanmaku = onSendDanmaku,
            onAddWatchLater = onAddWatchLater,
        ),
        preferences = FullscreenPreferenceActions(
            onAudioOnlyChanged = onAudioOnlyChanged,
            onAutoPlayNextChanged = onAutoPlayNextChanged,
            onLoopModeChanged = onLoopModeChanged,
            onMirrorChanged = onMirrorChanged,
            onResizeModeChanged = onResizeModeChanged,
            onColorAssistChanged = onColorAssistChanged,
            onSleepAtChanged = onSleepAtChanged,
            onBackgroundPlayChanged = onBackgroundPlayChanged,
        ),
    )
}
