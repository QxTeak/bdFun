package com.aritxonly.bdfun.infra.bili.mapper

import com.aritxonly.bdfun.core.domain.choosePlaybackAudio
import com.aritxonly.bdfun.core.domain.choosePlaybackVideo
import com.aritxonly.bdfun.core.model.video.PlayInfo
import com.aritxonly.bdfun.core.model.video.Quality
import com.aritxonly.bdfun.infra.bili.biliQualityLabel as qualityLabel
import com.aritxonly.bdfun.infra.bili.dto.PlayInfoDto
import com.aritxonly.bdfun.infra.bili.dto.PlaybackRepresentationDto
import org.json.JSONObject

internal fun readPlayInfo(data: JSONObject): PlayInfoDto {
    val dash = data.optJSONObject("dash")
    return PlayInfoDto(
        videos = dash?.optJSONArray("video").objects().map(::readPlaybackRepresentation),
        audios = dash?.optJSONArray("audio").objects().map(::readPlaybackRepresentation),
        fallback = readProgressiveFallback(data),
        durationMs = data.l("timelength"),
        acceptQuality = data.optJSONArray("accept_quality")?.let { array ->
            (0 until array.length()).map { array.optInt(it, 0) }
        }.orEmpty(),
    )
}

private fun readPlaybackRepresentation(rep: JSONObject): PlaybackRepresentationDto {
    // B 站 dash 条目用 snake_case；真实样本里还同时带一份 `SegmentBase`（首字母大写）。
    val segment = rep.optJSONObject("segment_base") ?: rep.optJSONObject("segmentBase")
        ?: rep.optJSONObject("SegmentBase")
    return PlaybackRepresentationDto(
        id = rep.optInt("id", 0), bandwidth = rep.l("bandwidth"), codec = rep.s("codecs"),
        url = rep.s("baseUrl").ifBlank { rep.s("base_url") },
        backups = jsonStrings(rep.optJSONArray("backupUrl") ?: rep.optJSONArray("backup_url")),
        width = rep.optInt("width", 0), height = rep.optInt("height", 0),
        frameRate = rep.s("frameRate").ifBlank { rep.s("fps") },
        initRange = segment?.let { it.s("initialization").ifBlank { it.s("Initialization") } }
            ?.takeIf { it.isNotBlank() },
        indexRange = segment?.let { it.s("indexRange").ifBlank { it.s("index_range") } }
            ?.takeIf { it.isNotBlank() },
    )
}

internal fun mapPlayInfo(
    dto: PlayInfoDto, durationSec: Int, codecPreference: String,
    rewrite: (String, List<String>) -> String,
): PlayInfo {
    val selectedAudio = choosePlaybackAudio(dto.audios, { it.id }, { it.bandwidth })
    val audio = selectedAudio?.let { rewrite(it.url, it.backups) to it.url }
    val qualities = mutableListOf<Quality>()
    dto.videos.groupBy { it.id }.forEach { (id, reps) ->
        val rep = choosePlaybackVideo(reps, codecPreference, { it.codec }, { it.bandwidth }) ?: return@forEach
        val baseUrl = rep.url
        if (baseUrl.isBlank()) return@forEach
        qualities += Quality(
            id = id, label = qualityLabel(id), width = rep.width, height = rep.height,
            frameRate = rep.frameRate, codec = rep.codec,
            videoUrl = rewrite(baseUrl, rep.backups), rawVideoUrl = baseUrl,
            bandwidth = rep.bandwidth, initRange = rep.initRange, indexRange = rep.indexRange,
        )
    }
    val fallback = dto.fallback
    return PlayInfo(
        qualities = qualities.sortedByDescending { it.id },
        audioUrl = audio?.first, rawAudioUrl = audio?.second,
        fallbackUrl = fallback.takeIf { it.isNotBlank() }?.let { rewrite(it, emptyList()) },
        rawFallbackUrl = fallback.takeIf { it.isNotBlank() },
        durationMs = dto.durationMs.takeIf { it > 0 } ?: (durationSec * 1000L),
        acceptQuality = dto.acceptQuality,
        audioBandwidth = selectedAudio?.bandwidth ?: 0,
        audioCodec = selectedAudio?.codec.orEmpty(),
        audioInitRange = selectedAudio?.initRange,
        audioIndexRange = selectedAudio?.indexRange,
    )
}

internal fun readProgressiveFallback(data: JSONObject): String =
    data.optJSONArray("durl")?.optJSONObject(0)?.s("url").orEmpty()
