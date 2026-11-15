package com.aritxonly.bdfun.feature.live.application

import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.common.messageOr
import com.aritxonly.bdfun.core.domain.LiveDanmakuConnection
import com.aritxonly.bdfun.core.domain.LivePlayback
import com.aritxonly.bdfun.core.model.live.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal data class LiveRoomContentState(
    val room: LiveRoomInfo? = null, val roomId: Long = 0,
    val qualities: List<Pair<Int, String>> = emptyList(), val emotes: Map<String, String> = emptyMap(),
)
internal data class LiveTransportState(
    val url: String? = null, val error: String? = null, val quality: Int = 10000,
    val playing: Boolean = true, val danmakuEnabled: Boolean = true, val reloadTick: Int = 0,
)
internal data class LiveRoomState(
    val content: LiveRoomContentState = LiveRoomContentState(), val playback: LiveTransportState = LiveTransportState(),
    val messages: List<LiveChatMessage> = emptyList(), val danmakuConnected: Boolean = false,
)

/** 保留main的请求顺序、同地址重载和300条窗口；离开页面后拒绝迟到结果。 */
internal class LiveRoomStateHolder(
    private val requestedRoomId: Long, private val fallback: LiveRoomInfo, private val access: LiveReadAccess,
    private val connection: LiveDanmakuConnection, private val playback: LivePlayback, scope: CoroutineScope,
    private val currentUid: () -> Long, private val clockMillis: () -> Long = System::currentTimeMillis,
    private val clockNanos: () -> Long = System::nanoTime, private val log: (String) -> Unit = {},
) {
    private val mutable = MutableStateFlow(LiveRoomState())
    val state = mutable.asStateFlow()
    private val job = SupervisorJob(scope.coroutineContext[Job])
    private val ownScope = CoroutineScope(scope.coroutineContext + job)
    private var closed = false
    private var lastLocalEcho: Pair<String, Long>? = null
    val displayRoom get() = mutable.value.content.room ?: fallback.copy(roomId = requestedRoomId)

    suspend fun loadRoom() {
        val roomId = if (requestedRoomId > 0) requestedRoomId else read { access.liveHotRoomId() } ?: fallback.roomId
        val quality = mutable.value.playback.quality
        // 房间信息/背景/清晰度/播放地址互不依赖，四条并行；标题和封面先上屏，流地址随后接。
        kotlinx.coroutines.coroutineScope {
            val infoRequest = async { read { access.liveRoomInfo(roomId) } }
            val portraitRequest = async { read { access.livePortraitBackground(roomId) } }
            val qualitiesRequest = async { read { access.liveQualities(roomId) } }
            val urlRequest = async { read { access.livePlayUrl(roomId, quality) } }

            val info = infoRequest.await()
            val portrait = portraitRequest.await()
            val room = (info ?: fallback.copy(roomId = roomId)).let {
                if (portrait.isNullOrBlank()) it else it.copy(cover = portrait)
            }
            mutable.update { it.copy(content = it.content.copy(room = room, roomId = roomId)) }

            val qualities = qualitiesRequest.await().orEmpty()
            mutable.update { it.copy(content = it.content.copy(qualities = qualities)) }

            val url = urlRequest.await()
            mutable.update { it.copy(playback = it.playback.copy(url = url,
                error = if (url == null) "直播流拿不到，可能未开播或接口变动" else it.playback.error)) }
        }
    }

    suspend fun loadEmotes() {
        val roomId = mutable.value.content.roomId
        if (roomId <= 0) return
        val emotes = read { access.liveEmoticons(roomId) }.orEmpty()
        mutable.update { it.copy(content = it.content.copy(emotes = emotes)) }
    }

    suspend fun receiveDanmaku() {
        val roomId = mutable.value.content.roomId
        if (roomId <= 0 || closed) return
        connection.disconnect()
        val info = read { access.liveDanmuInfo(roomId) }
        if (info == null) { log("danmuInfo=null room=$roomId"); return }
        log("danmuInfo ok room=$roomId hosts=${info.second.size}")
        val uid = currentUid()
        connection.connect(info.second, info.first, roomId, uid)
        log("ws connect urls=${info.second.size} first=${info.second.firstOrNull()} uid=$uid")
        var logged = 0
        connection.collectMessages { packet ->
            if (closed) return@collectMessages
            if (logged < 6) { log("packet op=${packet.operation} len=${packet.body.size}"); logged++ }
            if (packet.operation != LIVE_DANMAKU_MESSAGE_OPERATION) return@collectMessages
            if (!mutable.value.danmakuConnected) mutable.update { it.copy(messages = emptyList(), danmakuConnected = true) }
            access.decode(String(packet.body, Charsets.UTF_8), clockNanos())?.let { message ->
                // 自己发的弹幕会先从本地回显；服务端随后再推一遍同一条时不要出现两行。
                val echo = lastLocalEcho
                if (echo != null && message.kind == LiveChatKind.DANMAKU &&
                    message.text == echo.first && clockMillis() - echo.second < 5_000L
                ) {
                    lastLocalEcho = null
                    return@collectMessages
                }
                mutable.update { it.copy(messages = (it.messages + message).takeLast(300)) }
            }
        }
    }

    fun startPlayback(url: String) {
        if (closed) return
        try { playback.prepare(url); mutable.update { it.copy(playback = it.playback.copy(playing = true)) } }
        catch (cancel: CancellationException) { throw cancel }
        catch (failure: Exception) { playbackFailure(failure) }
    }

    fun reload(quality: Int) {
        if (closed) return
        mutable.update { it.copy(playback = it.playback.copy(error = null)) }
        ownScope.launch {
            val url = read { access.livePlayUrl(mutable.value.content.roomId, quality) }
            if (url == null) {
                mutable.update { it.copy(playback = it.playback.copy(error = "直播重载失败，请稍后再试")) }
                return@launch
            }
            mutable.update { it.copy(playback = it.playback.copy(quality = quality, url = url)) }
            startPlayback(url)
            mutable.update { it.copy(messages = emptyList(), danmakuConnected = false,
                playback = it.playback.copy(playing = true, reloadTick = it.playback.reloadTick + 1)) }
        }
    }

    fun appendLocal(text: String) {
        if (closed) return
        val message = LiveChatMessage(clockMillis(), LiveChatKind.DANMAKU, "我", text, nameColor = 0xFF6699L)
        mutable.update { it.copy(messages = (it.messages + message).takeLast(300)) }
    }

    /**
     * 真正发到直播间；成功才回显，失败把服务端原因交给页面 toast。
     * 返回 null 表示成功（或空消息），非空字符串是给用户看的错误。
     */
    suspend fun sendMessage(text: String): String? {
        if (closed) return null
        val value = text.trim()
        if (value.isEmpty()) return null
        if (!access.isLoggedIn) return "请先登录后再发弹幕"
        val roomId = mutable.value.content.roomId
        if (roomId <= 0L) return "直播间还没加载好"
        val result = access.sendDanmaku(roomId, value)
        currentCoroutineContext().ensureActive()
        if (closed) return null
        return when (result) {
            is Outcome.Success -> {
                lastLocalEcho = value to clockMillis()
                appendLocal(value)
                null
            }
            is Outcome.Failure -> result.error.messageOr("发送失败")
        }
    }
    fun togglePlaying() {
        if (closed) return
        val next = !mutable.value.playback.playing
        mutable.update { it.copy(playback = it.playback.copy(playing = next)) }
        if (next) playback.play() else playback.pause()
    }
    fun toggleDanmaku() { if (!closed) mutable.update { it.copy(playback = it.playback.copy(danmakuEnabled = !it.playback.danmakuEnabled)) } }
    fun close() {
        if (closed) return
        closed = true
        job.cancel()
        connection.disconnect()
        playback.release()
    }
    private fun playbackFailure(failure: Exception) {
        mutable.update { it.copy(playback = it.playback.copy(error = failure.message ?: "播放失败")) }
    }
    private suspend fun <T> read(action: suspend () -> Outcome<T>): T? {
        if (closed) throw CancellationException("直播页已关闭")
        val result = action()
        currentCoroutineContext().ensureActive()
        if (closed) throw CancellationException("直播页已关闭")
        return when (result) { is Outcome.Success -> result.value; is Outcome.Failure -> null }
    }
}
