package com.aritxonly.bdfun.core.model.video

data class PlayInfo(
    val qualities: List<Quality>,
    val audioUrl: String?,
    val fallbackUrl: String?,
    val durationMs: Long,
    val acceptQuality: List<Int>,
    /** CDN 替换前的原始音轨/兜底地址。 */
    val rawAudioUrl: String? = null,
    val rawFallbackUrl: String? = null,
    /** 当前音轨的 DASH 描述；和画质一起拼本地 MPD 用。 */
    val audioBandwidth: Long = 0,
    val audioCodec: String = "",
    val audioInitRange: String? = null,
    val audioIndexRange: String? = null,
) {
    val isDash: Boolean get() = qualities.isNotEmpty() && audioUrl != null
}
