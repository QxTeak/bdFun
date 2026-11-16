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
    val positionMs by remember(playback) { derivedStateOf { state.value.positionMs } }
    val durationMs by remember(playback) { derivedStateOf { state.value.durationMs } }
    var playerViewRef by ui.playerViewRef
    val fullscreen = presentation.fullscreen
    val danmaku = media.danmaku
    val frameMs by ui.frameMs
    val boosting by ui.boosting
    val boostSpeed by ui.boostSpeed
    val speed by ui.speed
    Box(
        modifier = playerPictureTransform(ui),
    ) {
        AndroidView(
            factory = { ctx ->
                playback.createOutput(ctx).also { playerViewRef = it }.view
            },
            modifier = Modifier.fillMaxSize(),
        )

        if (danmakuVisible) {
            // 只跟"有没有计数内容"有关，不看播放控件：控件层压在弹幕之上，
            // 开关控件不应该让弹幕整体位移。
            val counterVisible = picture.hotDanmakuEnabled && !ui.fsAudioOnly.value &&
                ui.hotDanmaku.value.isNotEmpty()
            slots.danmaku(danmaku, { frameMs - btLatencyMs }, playing,
                if (boosting) boostSpeed else speed, Modifier.fillMaxSize(), fullscreen, counterVisible)
        }

        PlayerAlwaysProgress(ui, fullscreen, positionMs, durationMs, picture.progressBarMode, picture.accentColor)
        PlayerHotDanmakuOverlay(ui, fullscreen, picture.hotDanmakuEnabled,
            picture.hotDanmakuStrokeDp, picture.hotDanmakuTextScale)
        if (sponsor != null) {
            PlayerSponsorOverlay(sponsor, fullscreen, picture.accentColor) { segment ->
                onSkip(segment)
            }
        }

    }

}

private fun playerPictureTransform(ui: VideoPlayerUiStateHolder): Modifier {
    val fsMirror by ui.fsMirror
    var videoScale by ui.videoScale
    var videoRotation by ui.videoRotation
    var videoOffset by ui.videoOffset
    val fsColorAssist by ui.fsColorAssist
    return Modifier
        .fillMaxSize()
        .graphicsLayer {
            scaleX = (if (fsMirror) -1f else 1f) * videoScale
            scaleY = videoScale
            rotationZ = videoRotation
            translationX = videoOffset.x
            translationY = videoOffset.y
        }
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
        .drawWithContent {
            drawContent()
            when (fsColorAssist) {
                1 -> drawRect(Color.Gray, blendMode = BlendMode.Saturation)
                2 -> drawRect(Color(0x3300A8FF), blendMode = BlendMode.Color)
            }
        }
}
