package com.aritxonly.bdfun.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.viewinterop.AndroidView
import com.aritxonly.bdfun.core.application.PlaybackVideoOutputMode
import com.aritxonly.bdfun.core.model.video.SponsorSegment
import com.aritxonly.bdfun.feature.video.presentation.VideoPlaybackPlatform

@Composable
fun PlayerSurface(
    playback: VideoPlaybackPlatform,
    media: VideoMediaInput,
    presentation: VideoPlayerPresentation,
    ui: VideoPlayerUiStateHolder,
    danmakuVisible: Boolean,
    btLatencyMs: Int,
    sponsor: SponsorSegment?,
    picture: VideoPictureSettings, slots: PlayerSurfaceSlots,
    onSkip: (SponsorSegment) -> Unit,
) {
    val state = playback.state.collectAsState()
    val playing by remember(playback) { derivedStateOf { state.value.playing } }
    var playerViewRef by ui.playerViewRef
    val fullscreen = presentation.fullscreen
    val danmaku = media.danmaku
    val boosting by ui.boosting
    val boostSpeed by ui.boostSpeed
    val speed by ui.speed
    // 杜比视界只能走直通表面；此时缩放/镜像/色觉辅助都做不到，下面统一关掉。
    val outputTarget by playback.videoOutputTarget.collectAsState()
    val directSurface = outputTarget?.mode == PlaybackVideoOutputMode.DolbyVision
    Box(
        modifier = Modifier.fillMaxSize()
            .then(if (directSurface) Modifier else Modifier.playerPictureGestures(ui))
            .drawColorAssist(ui, enabled = !directSurface),
    ) {
        // 只缩放视频画面本身：弹幕、进度条、赞助提示都不跟着变大变小。
        Box(modifier = Modifier.fillMaxSize()
            .then(if (directSurface) Modifier else Modifier.playerPictureTransform(ui))) {
            AndroidView(
                factory = { ctx -> PlayerOutputHost(ctx) },
                modifier = Modifier.fillMaxSize(),
                update = { host ->
                    host.bind {
                        playback.createOutput(host.context).also { playerViewRef = it }
                    }
                },
                onRelease = { host -> host.dispose() },
            )
        }

        if (danmakuVisible) {
            val hot = if (picture.hotDanmakuEnabled && !ui.fsAudioOnly.value) ui.danmakuHit.hotPresentation.value
                else HotDanmakuPresentation()
            slots.danmaku(danmaku, { ui.frameMs.longValue - btLatencyMs }, playing,
                if (boosting) boostSpeed else speed, Modifier.fillMaxSize(), fullscreen, hot, ui.danmakuHit)
        }

        PlayerAlwaysProgress(ui, fullscreen, picture.progressBarMode, picture.accentColor)
        PlayerHotDanmakuOverlay(ui, fullscreen, picture, danmakuVisible)
        if (sponsor != null) {
            PlayerSponsorOverlay(sponsor, fullscreen, picture.accentColor) { segment ->
                onSkip(segment)
            }
        }

    }

}

private fun Modifier.playerPictureTransform(ui: VideoPlayerUiStateHolder): Modifier {
    val fsMirror by ui.fsMirror
    var videoScale by ui.videoScale
    var videoRotation by ui.videoRotation
    var videoOffset by ui.videoOffset
    return this
        .fillMaxSize()
        .graphicsLayer {
            scaleX = (if (fsMirror) -1f else 1f) * videoScale
            scaleY = videoScale
            rotationZ = videoRotation
            translationX = videoOffset.x
            translationY = videoOffset.y
        }
}

/** 双指缩放/旋转/平移只改视频画面；手势层仍铺满整个播放器，弹幕大小不受影响。 */
private fun Modifier.playerPictureGestures(ui: VideoPlayerUiStateHolder): Modifier {
    var videoScale by ui.videoScale
    var videoRotation by ui.videoRotation
    var videoOffset by ui.videoOffset
    return this
        .fillMaxSize()
        .pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                while (true) {
                    val event = awaitPointerEvent()
                    if (event.changes.count { it.pressed } >= 2) {
                        val zoom = event.calculateZoom()
                        val pan = event.calculatePan()
                        val rotation = event.calculateRotation()
                        videoScale = (videoScale * zoom).coerceIn(0.4f, 3f)
                        videoRotation += rotation
                        videoOffset += pan
                        event.changes.forEach { it.consume() }
                    }
                    if (event.changes.none { it.pressed }) break
                }
            }
        }
}

private fun Modifier.drawColorAssist(ui: VideoPlayerUiStateHolder, enabled: Boolean): Modifier {
    val fsColorAssist by ui.fsColorAssist
    return this.drawWithContent {
        drawContent()
        if (!enabled) return@drawWithContent
        when (fsColorAssist) {
            1 -> drawRect(Color.Gray, blendMode = BlendMode.Saturation)
            2 -> drawRect(Color(0x3300A8FF), blendMode = BlendMode.Color)
        }
    }
}
