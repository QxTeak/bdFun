package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.model.video.*

data class DanmakuSendAppearance(val vipColor: Boolean = false, val upIdentity: Boolean = false)

data class DanmakuPost(
    val cid: Long, val bvid: String, val text: String, val progressMs: Long,
    val mode: Int, val fontSize: Int, val color: Int, val aid: Long = 0, val appearance: DanmakuSendAppearance = DanmakuSendAppearance(),
)

data class DanmakuVote(val likes: Long?, val liked: Boolean)

interface VideoMetadataRepository {
    suspend fun relation(mid: Long): Outcome<Int?>
    suspend fun followers(mid: Long): Outcome<Long>
    suspend fun videoCount(mid: Long): Outcome<Long>
    suspend fun online(aid: Long, cid: Long): Outcome<Pair<Long, String>>
    suspend fun subtitles(aid: Long, cid: Long): Outcome<List<SubtitleTrack>>
    suspend fun subtitleCues(url: String): Outcome<List<SubtitleCue>>
    suspend fun reportDanmaku(report: DanmakuReport): Outcome<Unit>
    suspend fun danmakuVotes(cid: Long, ids: List<Long>): Outcome<Map<Long, DanmakuVote>>
    suspend fun likeDanmaku(cid: Long, id: Long, like: Boolean): Outcome<Boolean>
    suspend fun sendDanmaku(post: DanmakuPost): Outcome<Long>
    suspend fun recallDanmaku(cid: Long, id: Long): Outcome<Unit>
    suspend fun specialDanmaku(aid: Long, cid: Long): Outcome<SpecialDanmakuPlayback>
    suspend fun danmakuCommands(aid: Long, cid: Long): Outcome<List<DanmakuCommand>>
    suspend fun submitDanmakuCommand(selection: DanmakuCommandSelection): Outcome<Unit>
    /** 互动投票实时候选票数（选项值 -> 票数）；普通评分命令返回空。 */
    suspend fun danmakuVoteCounts(voteId: Long): Outcome<Map<Int, Long>>
    suspend fun danmakuRules(): Outcome<List<DanmakuCloudRule>>
    suspend fun addDanmakuRule(kind: DanmakuRuleKind, value: String): Outcome<DanmakuCloudRule>
    suspend fun syncDanmakuConfig(config: DanmakuCloudConfig): Outcome<Unit>
    suspend fun deleteDanmakuRule(id: Long): Outcome<Unit>
}
