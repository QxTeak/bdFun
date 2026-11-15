package com.aritxonly.bdfun.feature.live.application

import com.aritxonly.bdfun.core.common.*
import com.aritxonly.bdfun.core.domain.*
import com.aritxonly.bdfun.core.model.live.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class LiveRoomStateHolderTest {
    private class Source : LiveRepository {
        val calls = mutableListOf<String>()
        var url: Outcome<String?> = Outcome.Success("same-stream")
        var gate: CompletableDeferred<Unit>? = null
        override suspend fun hotRoomId(): Outcome<Long?> { calls += "hot"; return Outcome.Success(null) }
        override suspend fun roomInfo(roomId: Long): Outcome<LiveRoomInfo?> {
            calls += "info:$roomId"
            gate?.let { withContext(NonCancellable) { it.await() } }
            return Outcome.Success(null)
        }
        override suspend fun portraitBackground(roomId: Long): Outcome<String?> { calls += "portrait"; return Outcome.Success("portrait") }
        override suspend fun qualities(roomId: Long): Outcome<List<Pair<Int, String>>> { calls += "qualities"; return Outcome.Success(listOf(80 to "高清")) }
        override suspend fun emoticons(roomId: Long) = Outcome.Success(emptyMap<String, String>())
        override suspend fun danmuInfo(roomId: Long) = Outcome.Success("token" to listOf("wss://fixture"))
        override suspend fun playUrl(roomId: Long, qn: Int): Outcome<String?> { calls += "play:$qn"; return url }
        override suspend fun sendDanmaku(roomId: Long, text: String): Outcome<Unit> {
            calls += "send:$roomId:$text"
            return Outcome.Success(Unit)
        }
    }
    private class Connection : LiveDanmakuConnection {
        var packets = emptyList<LiveDanmakuPacket>()
        var disconnects = 0
        var uid: Long? = null
        override fun connect(urls: List<String>, token: String, roomId: Long, uid: Long) { this.uid = uid }
        override fun disconnect() { disconnects++ }
        override suspend fun collectMessages(consumer: suspend (LiveDanmakuPacket) -> Unit) { packets.forEach { consumer(it) } }
    }
    private class Playback : LivePlayback {
        val prepared = mutableListOf<String>()
        var releases = 0
        var fail = false
        override fun prepare(url: String) { prepared += url; if (fail) error("decoder") }
        override fun play() {}
        override fun pause() {}
        override fun release() { releases++ }
    }
    private class Fixture(id: Long = 0) : AutoCloseable {
        val source = Source()
        val connection = Connection()
        val playback = Playback()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        private var sequence = 0L
        val holder = LiveRoomStateHolder(id, LiveRoomInfo(42, "标题", "主播", 7),
            LiveReadAccess(source, LoginStatus { true }, LiveMessageDecoder { raw, n ->
                if (raw == "unsupported") null else LiveChatMessage(n, LiveChatKind.DANMAKU, "观众", raw)
            }), connection, playback, scope, { 7 }, { ++sequence }, { ++sequence })
        override fun close() { holder.close(); scope.cancel() }
    }

    @Test fun missingHotAndInfoKeepMainFallbackAndRequestOrder() = runBlocking {
        Fixture().use { f ->
            f.holder.loadRoom()
            assertEquals(listOf("hot", "info:42", "portrait", "qualities", "play:10000"), f.source.calls)
            assertEquals(42L, f.holder.displayRoom.roomId)
            assertEquals("portrait", f.holder.displayRoom.cover)
            assertEquals("same-stream", f.holder.state.value.playback.url)
        }
    }
    @Test fun sameUrlReloadStillPreparesAndClearsMessagesEvenWhenPreparationFails() = runBlocking {
        Fixture(9).use { f ->
            f.holder.loadRoom(); f.holder.appendLocal("本地")
            f.playback.fail = true
            f.holder.reload(80)
            assertEquals(listOf("same-stream"), f.playback.prepared)
            assertEquals(80, f.holder.state.value.playback.quality)
            assertEquals(1, f.holder.state.value.playback.reloadTick)
            assertTrue(f.holder.state.value.messages.isEmpty())
            assertTrue(f.holder.state.value.playback.playing)
            assertEquals("decoder", f.holder.state.value.playback.error)
        }
    }
    @Test fun failedReloadKeepsExistingStreamQualityAndMessages() = runBlocking {
        Fixture(9).use { f ->
            f.holder.loadRoom(); f.holder.appendLocal("保留")
            f.source.url = Outcome.Failure(AppError.Network("offline"))
            f.holder.reload(80)
            val state = f.holder.state.value
            assertEquals("same-stream", state.playback.url)
            assertEquals(10000, state.playback.quality)
            assertEquals(listOf("保留"), state.messages.map { it.text })
            assertEquals(0, state.playback.reloadTick)
            assertEquals("直播重载失败，请稍后再试", state.playback.error)
        }
    }
    @Test fun firstBusinessPacketClearsLocalEchoEvenWhenDecoderDoesNotSupportIt() = runBlocking {
        Fixture(9).use { f ->
            f.holder.loadRoom(); f.holder.appendLocal("本地")
            f.connection.packets = listOf(LiveDanmakuPacket(1, 3, 1, byteArrayOf()),
                LiveDanmakuPacket(1, 5, 2, "unsupported".toByteArray()))
            f.holder.receiveDanmaku()
            assertTrue(f.holder.state.value.danmakuConnected)
            assertTrue(f.holder.state.value.messages.isEmpty())
            assertEquals(7L, f.connection.uid)
            f.connection.packets = (1..305).map { LiveDanmakuPacket(1, 5, it, "$it".toByteArray()) }
            f.holder.receiveDanmaku()
            assertEquals(300, f.holder.state.value.messages.size)
            assertEquals("6", f.holder.state.value.messages.first().text)
        }
    }
    @Test fun closeRejectsLateResponseAndReleasesExactlyOnce() = runBlocking {
        Fixture(9).use { f ->
            f.source.gate = CompletableDeferred()
            val request = launch(Dispatchers.Unconfined) { f.holder.loadRoom() }
            f.holder.close(); f.holder.close()
            f.source.gate!!.complete(Unit); request.join()
            assertEquals(listOf("info:9"), f.source.calls)
            assertNull(f.holder.state.value.content.room)
            assertEquals(1, f.playback.releases)
            assertEquals(1, f.connection.disconnects)
        }
    }
}
