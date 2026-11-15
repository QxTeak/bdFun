package com.aritxonly.bdfun.infra.bili

import com.aritxonly.bdfun.core.domain.LiveRepository

class BiliLiveRepository(private val backend: BiliBackend) : LiveRepository {
    override suspend fun roomInfo(roomId: Long) = biliOutcome { backend.liveRoomInfo(roomId) }
    override suspend fun hotRoomId() = biliOutcome { backend.liveHotRoomId() }
    override suspend fun portraitBackground(roomId: Long) = biliOutcome { backend.livePortraitBackground(roomId) }
    override suspend fun qualities(roomId: Long) = biliOutcome { backend.liveQualities(roomId) }
    override suspend fun emoticons(roomId: Long) = biliOutcome { backend.liveEmoticons(roomId) }
    override suspend fun danmuInfo(roomId: Long) = biliOutcome { backend.liveDanmuInfo(roomId) }
    override suspend fun playUrl(roomId: Long, qn: Int) = biliOutcome { backend.livePlayUrl(roomId, qn) }
    override suspend fun sendDanmaku(roomId: Long, text: String) =
        biliOutcome { backend.sendLiveDanmaku(roomId, text) }
}
