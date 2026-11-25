package com.aritxonly.bdfun.core.application

import com.aritxonly.bdfun.core.model.video.VideoDetailOwner

import com.aritxonly.bdfun.core.model.video.VideoDetailInfo

import com.aritxonly.bdfun.core.model.video.VideoDetailCounts

import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.domain.*
import com.aritxonly.bdfun.core.model.cache.*
import com.aritxonly.bdfun.core.model.video.*
import kotlinx.coroutines.*

internal fun cacheEntry(status: CacheStatus = CacheStatus.Completed, id: String = "BV1234567890_7_80") = CachedVideo(id,
    CacheVideoInfo("BV1234567890", 3, 7, "title", "https://cover", "owner", 5, 12),
    CacheVideoFiles(80, "1080P", "$id/video.m4s", "$id/audio.m4s", "$id/cover.jpg"),
    CacheTransferState(1f, 120, 120, status), 100)
internal fun cacheDetail() = VideoDetail(
    bvid = "BV1234567890",
    aid = 3,
    title = "title",
    cover = "https://cover",
    desc = "",
    owner = VideoDetailOwner(
        ownerName = "owner",
        ownerFace = "",
        ownerMid = 5,
        ownerSign = "",
    ),
    counts = VideoDetailCounts(
        play = 0,
        danmaku = 0,
        like = 0,
        coin = 0,
        favorite = 0,
        share = 0,
        reply = 0,
    ),
    info = VideoDetailInfo(
        pubdate = 0,
        duration = 12,
        cid = 7,
        tname = "",
        pages = emptyList(),
    ),
)
internal fun cacheQuality() = Quality(80, "1080P", 1920, 1080, "30", "codec", "video")
internal open class CacheFilesFake : VideoCacheStorage {
    var restored = emptyList<CachedVideo>()
    val sizes = java.util.concurrent.ConcurrentHashMap<String, Long>()
    val written = mutableListOf<List<CachedVideo>>()
    val events = mutableListOf<String>()
    override fun read() = restored
    override fun write(entries: List<CachedVideo>) { written += entries.toList() }
    override fun prepare(id: String) = CacheDestination(id, "$id/video.part", "$id/audio.part", "$id/video.m4s", "$id/audio.m4s", "$id/cover.jpg")
    override fun delete(id: String) { events += "delete:$id"; sizes.keys.removeAll { it.startsWith("$id/") } }
    override fun exists(path: String) = sizes.containsKey(path)
    override fun uri(path: String) = "file:///$path"
    override fun size(path: String) = sizes[path] ?: 0L
    override fun commit(part: String, target: String) { events += "commit:$target"; sizes[target] = sizes.remove(part) ?: error("missing $part") }
    override fun removePartial(destination: CacheDestination) {
        events += "partials:${destination.folder}"; sizes.remove(destination.videoPart); sizes.remove(destination.audioPart)
    }
}
internal open class CacheTransferFake(val storage: CacheFilesFake) : VideoCacheTransfer {
    val calls = mutableListOf<String>()
    var gate: CompletableDeferred<Unit>? = null
    var clock = 10_000L
    var result: Outcome<Long>? = null
    override suspend fun contentLength(url: String): Long { calls += "HEAD:$url"; return if (url == "video") 100 else 20 }
    override suspend fun download(url: String, target: String, onBytes: suspend (Long) -> Unit): Outcome<Long> {
        calls += "GET:$url"; gate?.await()
        val bytes = if (url == "video") 100L else 20L
        storage.sizes[target] = bytes; clock += 220; onBytes(bytes)
        return result ?: Outcome.Success(bytes)
    }
    override suspend fun cover(url: String, target: String): String { calls += "COVER:$url"; storage.sizes[target] = 1; return target }
}
internal fun cacheQueue(storage: CacheFilesFake, transfer: CacheTransferFake,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined), start: () -> Unit = {}) =
    DownloadQueueStateHolder(storage, transfer, scope, Dispatchers.Unconfined, start, { transfer.clock },
        persistDispatcher = Dispatchers.Unconfined)
