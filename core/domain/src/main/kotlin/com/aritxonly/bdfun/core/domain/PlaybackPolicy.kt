package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.model.video.Quality

/** 编码与音轨选择规则不依赖 JSON、设备或网络，保留旧播放器的兼容性顺序。 */
fun <T> choosePlaybackAudio(items: List<T>, id: (T) -> Int, bandwidth: (T) -> Long): T? {
    val standard = items.filter { id(it) in setOf(30216, 30232, 30280) }
    return standard.maxByOrNull(id) ?: items.minByOrNull(bandwidth)
}

fun <T> choosePlaybackVideo(items: List<T>, preference: String, codec: (T) -> String, bandwidth: (T) -> Long): T? {
    val wanted = if (preference == "auto") emptyList() else items.filter { playbackCodecMatches(codec(it), preference) }
    val avc = items.filter { codec(it).contains("avc", true) }
    val pool = wanted.ifEmpty { avc.ifEmpty { items } }
    return if (wanted.isNotEmpty()) wanted.maxByOrNull(bandwidth)
    else pool.sortedWith(compareBy<T> { playbackCodecPriority(codec(it)) }.thenByDescending(bandwidth)).firstOrNull()
}

fun playbackCodecPriority(codec: String): Int {
    val value = codec.lowercase()
    return when {
        value.contains("avc") || value.contains("h264") -> 0
        value.contains("hev") || value.contains("hvc") || value.contains("h265") -> 1
        value.contains("av01") || value.contains("av1") -> 2
        else -> 3
    }
}

fun playbackCodecMatches(codec: String, preference: String): Boolean {
    val value = codec.lowercase()
    return when (preference.lowercase()) {
        "avc" -> value.contains("avc")
        "hevc" -> value.contains("hev") || value.contains("hvc")
        "av1" -> value.contains("av01") || value.contains("av1")
        "dvh1" -> value.contains("dvh1") || value.contains("dvhe") || value.contains("dolby")
        else -> true
    }
}

/**
 * 杜比视界必须直通显示表面，不能走 GPU 合成的纹理表面：
 * DV 的颜色（Profile 5 的 IPT/PQ）要靠解码器的 RPU 元数据交给屏幕解，
 * 一旦经过 GPU 合成，元数据和 10bit 一起丢失，整帧会变成绿色/紫色。
 * B 站会在清晰度 id 126，或 codecs 的 dvh1/dvhe/dolby 上标出来。
 */
fun isDolbyVisionQuality(quality: Quality?): Boolean {
    if (quality == null) return false
    val codec = quality.codec.lowercase()
    return quality.id == 126 || codec.contains("dvh") || codec.contains("dolby")
}
