package com.aritxonly.bdfun.ui


// 顺序按 B 站 qn 从高到低：127 8K / 126 杜比 / 125 HDR / 120 4K / 116 1080P60 / 112 1080P+ / 80 1080P / 64 720P
internal val QUALITY_LABELS = listOf("自动", "HDR", "4K", "1080P 60FPS", "1080P+", "1080P", "720P")
internal val QUALITY_VALUES = listOf(0, 125, 120, 116, 112, 80, 64)
internal val LATENCY_VALUES = listOf(50, 100, 150, 200, 250, 300)
internal val PLAYBACK_SPEED_VALUES = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f, 2.5f, 3f)
internal val PLAYBACK_SPEED_LABELS = PLAYBACK_SPEED_VALUES.map {
    if (it % 1f == 0f) "${it.toInt()}×" else "${it}×"
}
internal val BOOST_SPEED_VALUES = listOf(1.5f, 2f, 2.5f, 3f, 4f, 5f)
internal val BOOST_SPEED_LABELS = BOOST_SPEED_VALUES.map {
    if (it % 1f == 0f) "${it.toInt()}×" else "${it}×"
}
internal val DECODER_LABELS = listOf(
    "自动",
    "硬件解码",
    "软件解码",
    "硬件解码（禁回退）",
)

internal val CODEC_LABELS = listOf(
    "自动",
    "AVC (H.264)",
    "HEVC (H.265)",
    "AV1",
    "DVH1",
)

internal val CODEC_VALUES = listOf("auto", "avc", "hevc", "av1", "dvh1")
