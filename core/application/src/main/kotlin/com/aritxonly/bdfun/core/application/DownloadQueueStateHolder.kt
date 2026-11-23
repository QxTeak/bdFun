package com.aritxonly.bdfun.core.application

import com.aritxonly.bdfun.core.domain.*
import com.aritxonly.bdfun.core.model.cache.*
import com.aritxonly.bdfun.core.model.video.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.ConcurrentHashMap

/** 进程下载队列；每个任务有独立身份，旧回调和 finally 不得覆盖同 id 的重下任务。 */
class DownloadQueueStateHolder(private val storage: VideoCacheStorage, transfer: VideoCacheTransfer,
    private val scope: CoroutineScope, private val uiDispatcher: CoroutineDispatcher,
    private val startService: () -> Unit, private val now: () -> Long = System::currentTimeMillis) {
    private val mutable = MutableStateFlow<List<CachedVideo>>(emptyList())
    val state = mutable.asStateFlow()
    val items get() = state.value
    private val playback = CachedVideoPlayback(storage)
    private val runner = CacheDownloadRunner(storage, transfer, now)
    private val jobs = ConcurrentHashMap<String, Job>()
    private val tokens = ConcurrentHashMap<String, Any>()
    private val deletedDuringLoad = ConcurrentHashMap.newKeySet<String>()
    private var loadJob: Job? = null
    @Volatile private var loaded = false
    @Volatile private var started = false
    @Volatile private var closed = false
    fun initialize() {
        synchronized(this) {
            if (started || closed) return
            started = true
        }
        loadJob = scope.launch {
            val cached = storage.read()
            withContext(uiDispatcher) {
                if (closed) return@withContext
                mutable.update { current ->
                    val ids = current.mapTo(HashSet()) { it.id }
                    current + cached.filter { it.id !in ids && it.id !in deletedDuringLoad }
                }
                loaded = true
                val hadDeletions = deletedDuringLoad.isNotEmpty()
                deletedDuringLoad.clear()
                if (hadDeletions) persistNow()
            }
        }
    }
    fun hasActive() = items.any { it.status == CacheStatus.Queued || it.status == CacheStatus.Downloading }
    fun findPlayable(bvid: String, cid: Long): CachedVideo? = items.firstOrNull { entry ->
        entry.status == CacheStatus.Completed && entry.bvid == bvid && (cid <= 0L || entry.cid == cid) &&
            storage.exists(entry.videoPath) && (entry.audioPath.isBlank() || storage.exists(entry.audioPath))
    }
    fun cover(entry: CachedVideo) = playback.cover(entry)
    fun video(entry: CachedVideo) = playback.video(entry)
    fun detail(entry: CachedVideo) = playback.detail(entry)
    fun playInfo(entry: CachedVideo) = playback.playInfo(entry)
    fun enqueue(detail: VideoDetail, quality: Quality, cid: Long, audioUrl: String?, durationSec: Int): Boolean {
        if (!started || closed || audioUrl.isNullOrBlank()) return false
        val id = "${detail.bvid}_${cid}_${quality.id}"
        val existing = items.firstOrNull { it.id == id }
        if (existing != null && existing.status != CacheStatus.Failed) return false
        val destination = storage.prepare(id)
        val entry = CachedVideo(id, CacheVideoInfo(detail.bvid, detail.aid, cid, detail.title, detail.cover,
            detail.ownerName, detail.ownerMid, durationSec), CacheVideoFiles(quality.id, quality.label), CacheTransferState(), now())
        mutable.update { it.filterNot { old -> old.id == id } + entry }
        val token = Any()
        tokens[id] = token
        persistNow()
        startService()
        val job = scope.launch(start = CoroutineStart.LAZY) { run(CacheDownloadRequest(entry, quality.videoUrl, audioUrl, destination), token) }
        jobs[id] = job
        job.start()
        return true
    }
    /** 删除一条缓存：递归删文件在 IO 上做，删完再更新列表并落盘，界面线程不再卡住。 */
    suspend fun delete(id: String) {
        if (!loaded) deletedDuringLoad += id
        tokens.remove(id)
        jobs.remove(id)?.cancel()
        withContext(Dispatchers.IO) { storage.delete(id) }
        withContext(uiDispatcher) {
            mutable.update { it.filterNot { entry -> entry.id == id } }
            persistNow()
        }
    }
    fun close() {
        closed = true
        loadJob?.cancel()
        tokens.clear()
        jobs.values.forEach { it.cancel() }
        jobs.clear()
    }
    private suspend fun run(request: CacheDownloadRequest, token: Any) {
        val id = request.initial.id
        val current = { !closed && tokens[id] === token }
        try {
            runner.run(request, current, { tokens[id] == null || tokens[id] === token },
                publish = { entry -> withContext(uiDispatcher) {
                    if (current()) mutable.update { list -> list.map { if (it.id == id) entry else it } }
                } }, persist = { withContext(uiDispatcher) { if (current()) persistNow() } })
        } finally {
            tokens.remove(id, token)
            currentCoroutineContext()[Job]?.let { jobs.remove(id, it) }
        }
    }
    private fun persistNow() { if (loaded) storage.write(items) }
}
