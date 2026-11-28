package com.aritxonly.bdfun.infra.bili.dto

/** 只记录服务端播放表示字段，编码/音频选择仍交给原领域策略。 */
internal data class PlaybackRepresentationDto(
    val id: Int, val bandwidth: Long, val codec: String,
    val url: String, val backups: List<String>,
    val width: Int, val height: Int, val frameRate: String,
    /** dash.video/audio[].segment_base：fMP4 初始化段与 sidx 索引段。 */
    val initRange: String? = null,
    val indexRange: String? = null,
)

internal data class PlayInfoDto(
    val videos: List<PlaybackRepresentationDto>,
    val audios: List<PlaybackRepresentationDto>,
    val fallback: String, val durationMs: Long, val acceptQuality: List<Int>,
)
