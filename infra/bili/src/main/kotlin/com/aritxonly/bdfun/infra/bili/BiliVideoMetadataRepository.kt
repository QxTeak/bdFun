package com.aritxonly.bdfun.infra.bili

import com.aritxonly.bdfun.core.domain.DanmakuPost
import com.aritxonly.bdfun.core.domain.VideoMetadataRepository

class BiliVideoMetadataRepository(private val backend: BiliBackend) : VideoMetadataRepository {
    override suspend fun reportDanmaku(report: com.aritxonly.bdfun.core.domain.DanmakuReport) = biliOutcome { backend.api.reportDanmaku(report) }
    override suspend fun danmakuVotes(cid: Long, ids: List<Long>) = biliOutcome { backend.api.danmakuVotes(cid, ids) }
    override suspend fun likeDanmaku(cid: Long, id: Long, like: Boolean) = biliOutcome {
        backend.api.likeDanmaku(cid, id, like)
    }
    override suspend fun relation(mid: Long) = biliOutcome { backend.relation(mid) }
    override suspend fun followers(mid: Long) = biliOutcome { backend.relationStat(mid).second }
    override suspend fun videoCount(mid: Long) = biliOutcome { backend.upVideoCount(mid) }
    override suspend fun online(aid: Long, cid: Long) = biliOutcome { backend.onlineTotal(aid, cid) }
    override suspend fun tags(bvid: String) = biliOutcome { backend.videoTags(bvid) }
    override suspend fun subtitles(aid: Long, cid: Long) = biliOutcome { backend.subtitles(aid, cid) }
    override suspend fun subtitleCues(url: String) = biliOutcome { backend.subtitleCues(url) }
    override suspend fun sendDanmaku(post: DanmakuPost) = biliOutcome {
        backend.api.sendDanmaku(post.cid, post.bvid, post.text, post.progressMs, post.mode, post.fontSize, post.color, post.aid, post.appearance)
    }
    override suspend fun recallDanmaku(cid: Long, id: Long) = biliOutcome { backend.api.recallDanmaku(cid, id) }
    override suspend fun specialDanmaku(aid: Long, cid: Long) = biliOutcome { backend.api.specialDanmaku(aid, cid) }
    override suspend fun danmakuCommands(aid: Long, cid: Long) = biliOutcome { backend.api.danmakuCommands(aid, cid) }
    override suspend fun submitDanmakuCommand(selection: com.aritxonly.bdfun.core.domain.DanmakuCommandSelection) =
        biliOutcome { backend.api.submitDanmakuCommand(selection) }
    override suspend fun danmakuVoteCounts(voteId: Long) = biliOutcome { backend.api.danmakuVoteCounts(voteId) }
    override suspend fun danmakuRules() = biliOutcome { backend.api.danmakuRules() }
    override suspend fun addDanmakuRule(kind: com.aritxonly.bdfun.core.domain.DanmakuRuleKind, value: String) =
        biliOutcome { backend.api.addDanmakuRule(kind, value) }
    override suspend fun syncDanmakuConfig(config: com.aritxonly.bdfun.core.domain.DanmakuCloudConfig) =
        biliOutcome { backend.api.syncDanmakuConfig(config) }
    override suspend fun deleteDanmakuRule(id: Long) = biliOutcome { backend.api.deleteDanmakuRule(id) }
}
