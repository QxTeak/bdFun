package com.aritxonly.bdfun.infra.bili

import com.aritxonly.bdfun.core.domain.DanmakuPost
import com.aritxonly.bdfun.core.domain.VideoMetadataRepository

class BiliVideoMetadataRepository(private val backend: BiliBackend) : VideoMetadataRepository {
    override suspend fun likeDanmaku(cid: Long, id: Long) = biliOutcome {
        backend.api.likeDanmaku(cid, id)
    }
    override suspend fun relation(mid: Long) = biliOutcome { backend.relation(mid) }
    override suspend fun followers(mid: Long) = biliOutcome { backend.relationStat(mid).second }
    override suspend fun videoCount(mid: Long) = biliOutcome { backend.upVideoCount(mid) }
    override suspend fun online(aid: Long, cid: Long) = biliOutcome { backend.onlineTotal(aid, cid) }
    override suspend fun subtitles(aid: Long, cid: Long) = biliOutcome { backend.subtitles(aid, cid) }
    override suspend fun subtitleCues(url: String) = biliOutcome { backend.subtitleCues(url) }
    override suspend fun sendDanmaku(post: DanmakuPost) = biliOutcome {
        backend.sendDanmaku(post.cid, post.bvid, post.text, post.progressMs, post.mode, post.fontSize, post.color)
        Unit
    }
}
