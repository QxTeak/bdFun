package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.model.video.SponsorSegment

/** 沿用接口顺序、左闭右开区间；关闭过的片段在本视频内不再提示。 */
fun activeSponsorSegmentAt(segments: List<SponsorSegment>, positionMs: Long, dismissed: Set<String>): SponsorSegment? =
    segments.firstOrNull { segment ->
        segment.uuid !in dismissed && positionMs >= segment.startMs && positionMs < segment.endMs
    }

/** 空降助手模式：0 关闭 / 1 提示 / 2 自动跳过；旧布尔配置迁移后只会落在 0 或 1。 */
enum class SponsorSkipAction { Off, Prompt, Auto }

fun sponsorSkipAction(mode: Int): SponsorSkipAction = when (mode) {
    0 -> SponsorSkipAction.Off
    2 -> SponsorSkipAction.Auto
    else -> SponsorSkipAction.Prompt
}

