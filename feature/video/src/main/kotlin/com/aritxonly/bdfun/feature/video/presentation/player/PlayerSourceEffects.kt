package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import com.aritxonly.bdfun.core.application.PlaybackController
import com.aritxonly.bdfun.core.application.PlaybackSource
import com.aritxonly.bdfun.core.domain.resolvePlaybackSourceUrls
import com.aritxonly.bdfun.feature.video.presentation.VideoPlaybackPlatform
import androidx.compose.ui.platform.LocalContext

@Composable
fun BindPlayerSource(
    playback: VideoPlaybackPlatform,
    media: VideoMediaInput,
    presentation: VideoPlayerPresentation,
    ui: VideoPlayerUiStateHolder, diagnostics: PlayerDiagnostics, autoPlay: () -> Boolean,
    progress: com.aritxonly.bdfun.core.domain.PlaybackProgressRepository, resumePlayback: () -> Boolean,
) {
    val log = diagnostics.log
    val bvid = media.bvid
    val info = media.info
    val quality = media.quality
    val autoQuality = media.autoQuality
    val resumePositionMs = presentation.resumePositionMs
    val usingFallback by ui.usingFallback
    val usingOriginal by ui.usingOriginal
    val speed by ui.speed
    val mediaKey = "$bvid:${presentation.fullscreenInfo.currentCid}"
    LaunchedEffect(mediaKey, info, quality, usingFallback, usingOriginal) {
        val source = resolvePlaybackSourceUrls(quality, info, usingOriginal, usingFallback) ?: run {
            log(
                "PLAYER",
                "no-source bvid=$bvid q=${quality?.id} hasInfo=${info != null} " +
                    "qualities=${info?.qualities?.size ?: 0} " +
                    "usingOriginal=$usingOriginal usingFallback=$usingFallback",
            )
            if (info != null && info.qualities.isEmpty() && info.fallbackUrl.isNullOrBlank()) {
                diagnostics.toast("播放地址为空，已记录日志")
            }
            return@LaunchedEffect
        }
        val url = source.videoUrl
        val audioUrl = source.audioUrl
        log(
            "PLAYER",
            "source bvid=$bvid q=${quality?.id} auto=$autoQuality " +
                "usingOriginal=$usingOriginal usingFallback=$usingFallback " +
                "video=${describeMediaUrl(url)} audio=${describeMediaUrl(audioUrl)}",
        )
        val previousKey = ui.preparedMediaKey.value
        if (resumePlayback() && previousKey != null && previousKey != mediaKey) {
            val previous = previousKey.split(":")
            val tick = playback.refresh()
            progress.save(previous[0], previous.getOrNull(1)?.toLongOrNull() ?: 0L, tick.positionMs, tick.durationMs)
        }
        val localResume = if (resumePlayback() && previousKey != mediaKey && resumePositionMs <= 0L) {
            progress.resumeFor(bvid, presentation.fullscreenInfo.currentCid)
        } else 0L
        ui.preparedMediaKey.value = mediaKey
        playback.prepare(PlaybackSource(
            videoUrl = url, audioUrl = audioUrl, mediaKey = mediaKey,
            headers = mapOf("Referer" to "https://www.bilibili.com/video/$bvid/"), userAgent = playback.userAgent,
            resumePositionMs = if (resumePositionMs > 0L) resumePositionMs else localResume, autoPlay = autoPlay(), speed = speed,
        ))
    }

}
