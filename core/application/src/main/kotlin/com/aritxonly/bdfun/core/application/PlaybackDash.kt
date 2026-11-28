package com.aritxonly.bdfun.core.application

import com.aritxonly.bdfun.core.model.video.PlayInfo
import com.aritxonly.bdfun.core.model.video.Quality

/** 一档视频在 MPD 里的身份；切档时用它把 TrackGroup 里的 Format 对上。 */
data class PlaybackDashTrack(val id: Int, val width: Int, val height: Int, val bandwidth: Long)

/**
 * 本地 MPD 播放源。
 *
 * 为什么要拼 MPD：B 站 DASH 的每一档画质是一个独立的 m4s 地址。用
 * `MergingMediaSource(视频, 音频)` 时换档必须重建 MediaSource，ExoPlayer 会释放并重建
 * 音频解码器，听感上就是一次 100~300ms 的断音。把同一视频的所有档位写进一个 MPD 后，
 * 换档只是 `TrackSelectionOverride`，音频轨完全不动，可以做到真正无缝。
 */
data class PlaybackDashSource(
    val manifest: String,
    val videoQualityId: Int,
    val videoTracks: List<PlaybackDashTrack>,
)

/**
 * 是否具备拼本地 MPD 的条件。
 *
 * 不满足时必须回退到原来的渐进流：缺 `segment_base` 的 fMP4 无法声明 sidx 范围，
 * ExoPlayer 会按整文件 progressive 解析，拼出来的 MPD 反而放不了。
 */
fun dashAdaptiveAvailable(playInfo: PlayInfo): Boolean {
    if (playInfo.durationMs <= 0L) return false
    if (playInfo.audioUrl.isNullOrBlank()) return false
    if (playInfo.audioInitRange.isNullOrBlank() || playInfo.audioIndexRange.isNullOrBlank()) return false
    if (playInfo.qualities.size < 2) return false
    return playInfo.qualities.all { quality ->
        quality.videoUrl.isNotBlank() &&
            quality.initRange?.isNotBlank() == true &&
            quality.indexRange?.isNotBlank() == true
    }
}

/**
 * 把 [playInfo] 的全部画质档位拼成一个本地 MPD，并选定 [qualityId]。
 *
 * 返回 null 表示当前数据不适合自适应（调用方继续走渐进流），不抛异常。
 */
fun buildPlaybackDashSource(playInfo: PlayInfo, qualityId: Int): PlaybackDashSource? {
    if (!dashAdaptiveAvailable(playInfo)) return null
    // 没有宽高的档位无法参与自适应选择（MPD 允许省略，但 ExoPlayer 的 TrackGroup 会缺维度，
    // 切档时对不上号），直接整体放弃，避免出现“某些档位切不过去”。
    val videos = playInfo.qualities.filter { it.width > 0 && it.height > 0 }
    if (videos.size < 2) return null
    if (videos.none { it.id == qualityId }) return null
    val audioUrl = playInfo.audioUrl ?: return null

    val manifest = buildString {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        append("<MPD xmlns=\"urn:mpeg:dash:schema:mpd:2011\" type=\"static\"")
        append(" profiles=\"urn:mpeg:dash:profile:isoff-on-demand:2011\"")
        append(" mediaPresentationDuration=\"${playInfo.durationMs.toIsoDuration()}\"")
        append(" minBufferTime=\"PT1.500S\">\n")
        append("  <Period>\n")
        append("    <AdaptationSet contentType=\"video\" mimeType=\"video/mp4\"")
        append(" segmentAlignment=\"true\" startWithSAP=\"1\">\n")
        videos.forEach { quality -> appendVideoRepresentation(quality) }
        append("    </AdaptationSet>\n")
        append("    <AdaptationSet contentType=\"audio\" mimeType=\"audio/mp4\">\n")
        append("      <Representation id=\"audio\"")
        append(" bandwidth=\"${playInfo.audioBandwidth.coerceAtLeast(1)}\"")
        if (playInfo.audioCodec.isNotBlank()) {
            append(" codecs=\"${playInfo.audioCodec.xmlEscape()}\"")
        }
        append(">\n")
        append("        <BaseURL>${audioUrl.xmlEscape()}</BaseURL>\n")
        append("        <SegmentBase indexRange=\"${playInfo.audioIndexRange.orEmpty().xmlEscape()}\">\n")
        append("          <Initialization range=\"${playInfo.audioInitRange.orEmpty().xmlEscape()}\"/>\n")
        append("        </SegmentBase>\n")
        append("      </Representation>\n")
        append("    </AdaptationSet>\n")
        append("  </Period>\n")
        append("</MPD>\n")
    }
    return PlaybackDashSource(
        manifest = manifest,
        videoQualityId = qualityId,
        videoTracks = videos.map {
            PlaybackDashTrack(it.id, it.width, it.height, it.bandwidth)
        },
    )
}

private fun StringBuilder.appendVideoRepresentation(quality: Quality) {
    append("      <Representation id=\"${quality.id}\"")
    append(" bandwidth=\"${quality.bandwidth.coerceAtLeast(1)}\"")
    if (quality.codec.isNotBlank()) append(" codecs=\"${quality.codec.xmlEscape()}\"")
    append(" width=\"${quality.width}\" height=\"${quality.height}\"")
    // frameRate 只接受整数或 num/den；B 站偶尔给 "23.986" 这种小数，写进 MPD 会让解析失败。
    quality.frameRate.toDashFrameRate()?.let { append(" frameRate=\"$it\"") }
    append(">\n")
    append("        <BaseURL>${quality.videoUrl.xmlEscape()}</BaseURL>\n")
    append("        <SegmentBase indexRange=\"${quality.indexRange.orEmpty().xmlEscape()}\">\n")
    append("          <Initialization range=\"${quality.initRange.orEmpty().xmlEscape()}\"/>\n")
    append("        </SegmentBase>\n")
    append("      </Representation>\n")
}

internal fun String.toDashFrameRate(): String? {
    val value = trim()
    if (value.isEmpty()) return null
    val parts = value.split('/')
    if (parts.size == 2 && parts.all { it.toIntOrNull() != null && it.toInt() > 0 }) return value
    val single = value.toIntOrNull() ?: return null
    return single.takeIf { it > 0 }?.toString()
}

internal fun Long.toIsoDuration(): String {
    val safe = coerceAtLeast(0L)
    val seconds = safe / 1000
    val millis = safe % 1000
    return "PT$seconds.${millis.toString().padStart(3, '0')}S"
}

internal fun String.xmlEscape(): String = this
    .replace("&", "&amp;")
    .replace("\"", "&quot;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
