package com.aritxonly.bdfun.core.application

import com.aritxonly.bdfun.core.domain.DANMAKU_SEGMENT_MS
import com.aritxonly.bdfun.core.domain.danmakuSegmentWindow
import com.aritxonly.bdfun.core.model.video.DanmakuItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 视频弹幕的分段窗口：只保证「当前播放位置 ±1 段」已加载。
 *
 * 首次进视频时详情接口只会带回第 1、2 段；播放推进到新的一段时，
 * 由 [ensure] 补下前后各一段。这样不用在打开视频时把两小时的分段全部拉下来。
 */
class DanmakuSegmentFeed(
    private val loader: suspend (cid: Long, index: Int) -> List<DanmakuItem>,
) {
    private val lock = Mutex()
    private val segments = object : LinkedHashMap<Pair<Long, Int>, List<DanmakuItem>>(12, 0.75f, true) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<Pair<Long, Int>, List<DanmakuItem>>?,
        ): Boolean = size > 12
    }
    private val inFlight = HashSet<Pair<Long, Int>>()

    private val mutableRevision = MutableStateFlow(0L)

    /** 段数据变化时自增；界面用它触发一次合并。 */
    val revision = mutableRevision.asStateFlow()

    /** 把详情接口已经带回的分段登记进缓存，避免又下载一遍。 */
    fun seed(cid: Long, items: List<DanmakuItem>) {
        if (cid <= 0L || items.isEmpty()) return
        val grouped = items.groupBy { (it.timeMs.coerceAtLeast(0L) / DANMAKU_SEGMENT_MS).toInt() + 1 }
        var changed = false
        synchronized(segments) {
            grouped.forEach { (index, list) ->
                if (segments[cid to index] == null) {
                    segments[cid to index] = list
                    changed = true
                }
            }
        }
        if (changed) mutableRevision.update { it + 1 }
    }

    /** 保证当前段前后各一段已加载；同一段不会并发重复请求。 */
    suspend fun ensure(cid: Long, positionMs: Long) {
        if (cid <= 0L) return
        val window = danmakuSegmentWindow(positionMs)
        val missing = synchronized(segments) {
            window.filter { (cid to it) !in segments && (cid to it) !in inFlight }
        }
        if (missing.isEmpty()) return
        lock.withLock {
            for (index in missing) {
                val key = cid to index
                val already = synchronized(segments) {
                    if (key in segments || !inFlight.add(key)) true else false
                }
                if (already) continue
                try {
                    val items = try {
                        loader(cid, index)
                    } catch (cancel: kotlinx.coroutines.CancellationException) {
                        throw cancel
                    } catch (_: Exception) {
                        emptyList()
                    }
                    synchronized(segments) { segments[key] = items }
                    mutableRevision.update { it + 1 }
                } finally {
                    // 取消时也要把在途标记清掉，否则这一段以后再也不会重试。
                    synchronized(segments) { inFlight.remove(key) }
                }
            }
        }
    }

    /** 当前 cid 已缓存的全部弹幕（按时间排序）。 */
    fun items(cid: Long): List<DanmakuItem> = synchronized(segments) {
        segments.entries.filter { it.key.first == cid }
            .flatMap { it.value }
            .sortedBy { it.timeMs }
    }
}
