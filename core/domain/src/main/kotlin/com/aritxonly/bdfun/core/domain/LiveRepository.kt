package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.model.live.LiveRoomInfo

/** 保留main的空房间/未开播回退；传输错误由Outcome承载。 */
interface LiveRepository {
    suspend fun roomInfo(roomId: Long): Outcome<LiveRoomInfo?>
    suspend fun hotRoomId(): Outcome<Long?>
    suspend fun portraitBackground(roomId: Long): Outcome<String?>
    suspend fun qualities(roomId: Long): Outcome<List<Pair<Int, String>>>
    suspend fun emoticons(roomId: Long): Outcome<Map<String, String>>
    suspend fun danmuInfo(roomId: Long): Outcome<Pair<String, List<String>>?>
    suspend fun playUrl(roomId: Long, qn: Int): Outcome<String?>
    suspend fun sendDanmaku(roomId: Long, text: String): Outcome<Unit>
}
