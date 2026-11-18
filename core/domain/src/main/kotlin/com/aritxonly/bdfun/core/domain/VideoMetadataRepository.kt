package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.model.video.*

data class DanmakuPost(
    val cid: Long, val bvid: String, val text: String, val progressMs: Long,
    val mode: Int, val fontSize: Int, val color: Int,
)

interface VideoMetadataRepository {
    suspend fun relation(mid: Long): Outcome<Int?>
    suspend fun followers(mid: Long): Outcome<Long>
    suspend fun videoCount(mid: Long): Outcome<Long>
    suspend fun online(aid: Long, cid: Long): Outcome<Pair<Long, String>>
    suspend fun subtitles(aid: Long, cid: Long): Outcome<List<SubtitleTrack>>
    suspend fun subtitleCues(url: String): Outcome<List<SubtitleCue>>
    suspend fun likeDanmaku(cid: Long, id: Long): Outcome<Unit>
    suspend fun sendDanmaku(post: DanmakuPost): Outcome<Unit>
}
