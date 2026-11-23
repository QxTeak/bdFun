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
    val audioOnly by ui.fsAudioOnly
    val speed by ui.speed
    val mediaKey = "$bvid:${presentation.fullscreenInfo.currentCid}"
    // audioOnly 进 key：退出听视频时要按当时的档位重新起播（听视频期间被跳过的档位切换会在这里补上）。
    LaunchedEffect(mediaKey, info, quality, usingFallback, usingOriginal, audioOnly) {
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
        // 听视频没有画面，只有声音：DASH 的音轨与档位无关，而半屏压 720P、全屏回用户档位会让档位
        // 每一转都变一次。音轨没变就不该重新起播，否则每切一次横竖屏都要黑一下、缓冲一下。
        // 退出听视频要看画面了，这时才按当前档位重新起播。
        val unchanged = previousKey == mediaKey && if (audioOnly) {
            ui.preparedAudioUrl.value == audioUrl &&
                (audioUrl != null || ui.preparedVideoUrl.value == url)
        } else {
            ui.preparedVideoUrl.value == url && ui.preparedAudioUrl.value == audioUrl
        }
        if (unchanged) {
            log("PLAYER", "source-keep bvid=$bvid q=${quality?.id} audioOnly=$audioOnly")
            return@LaunchedEffect
        }
        if (resumePlayback() && previousKey != null && previousKey != mediaKey) {
            val previous = previousKey.split(":")
            val tick = playback.refresh()
            progress.save(previous[0], previous.getOrNull(1)?.toLongOrNull() ?: 0L, tick.positionMs, tick.durationMs)
        }
        val localResume = if (resumePlayback() && previousKey != mediaKey && resumePositionMs <= 0L) {
            progress.resumeFor(bvid, presentation.fullscreenInfo.currentCid)
        } else 0L
        ui.preparedMediaKey.value = mediaKey
        ui.preparedVideoUrl.value = url
        ui.preparedAudioUrl.value = audioUrl
        playback.prepare(PlaybackSource(
            videoUrl = url, audioUrl = audioUrl, mediaKey = mediaKey,
            headers = mapOf("Referer" to "https://www.bilibili.com/video/$bvid/"), userAgent = playback.userAgent,
            resumePositionMs = if (resumePositionMs > 0L) resumePositionMs else localResume, autoPlay = autoPlay(), speed = speed,
            // DASH 的音视频是分开的；投屏给电视一个文件地址，优先用能带声音的 durl。
            castUrl = info?.fallbackUrl?.takeIf { it.isNotBlank() },
        ))
    }

}
