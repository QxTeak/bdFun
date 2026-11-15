package com.aritxonly.bdfun.feature.live.application

import com.aritxonly.bdfun.core.common.*
import com.aritxonly.bdfun.core.domain.*

/** 通过Outcome读取直播数据，API与JSON实现均由端口隔离。 */
internal class LiveReadAccess(
    private val repository: LiveRepository, private val login: LoginStatus, private val decoder: LiveMessageDecoder,
) {
    val isLoggedIn get() = login.isLoggedIn()
    suspend fun liveRoomInfo(id: Long) = repository.roomInfo(id)
    suspend fun liveHotRoomId() = repository.hotRoomId()
    suspend fun livePortraitBackground(id: Long) = repository.portraitBackground(id)
    suspend fun liveQualities(id: Long) = repository.qualities(id)
    suspend fun liveEmoticons(id: Long) = repository.emoticons(id)
    suspend fun liveDanmuInfo(id: Long) = repository.danmuInfo(id)
    suspend fun livePlayUrl(id: Long, qn: Int) = repository.playUrl(id, qn)
    suspend fun sendDanmaku(id: Long, text: String) = repository.sendDanmaku(id, text)
    fun decode(raw: String, id: Long) = decoder.decode(raw, id)
}
