package com.aritxonly.bdfun.core.application

/** 仅同一视频/分 P 换源继承实时位置；新条目仍使用入口提供的恢复位置。 */
fun continuePlaybackSource(
    previousKey: String?, source: PlaybackSource, current: PlaybackSnapshot,
): PlaybackSource = if (source.mediaKey != null && source.mediaKey == previousKey) {
    source.copy(resumePositionMs = current.positionMs.coerceAtLeast(0),
        autoPlay = current.playWhenReady, speed = current.speed)
} else source
