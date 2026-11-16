package com.aritxonly.bdfun.core.application

import com.aritxonly.bdfun.core.domain.*
import com.aritxonly.bdfun.core.model.feed.*
import com.aritxonly.bdfun.core.model.video.VideoItem
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*

data class RecommendationMode(val ready: Boolean = false, val enabled: Boolean = false, val generation: Long = 0,
    val error: String? = null)
data class RecommendationTicket(val owner: Pair<String, Long>, val generation: Long)

/** 所有提交在同一锁内核对归属；持久化串行且合并快照，旧写入不能越过清除快照。 */
class RecommendationOptimizer(
    private val storage: RecommendationStorage, scope: CoroutineScope,
    private val owner: () -> Pair<String, Long>, private val now: () -> Long = System::currentTimeMillis,
    private val io: CoroutineDispatcher = Dispatchers.IO,
    private val log: (String) -> Unit = {},
) {
    private val mutable = MutableStateFlow(RecommendationMode())
    val state = mutable.asStateFlow()
    private var archive = RecommendationArchive()
    private val writes = Channel<RecommendationArchive>(Channel.CONFLATED)
    private var currentOwner = owner()
    private val guest = linkedMapOf<String, RecommendationMemory>()
    private val session = linkedMapOf<String, RecommendationMemory>()
    private val exposed = hashSetOf<String>()
    private val opened = linkedMapOf<String, Pair<RecommendationTicket, Long>>()

    init {
        scope.launch {
            try {
                val loaded = withContext(io) { storage.read() }
                synchronized(this@RecommendationOptimizer) {
                    archive = loaded.copy(accounts = bounded(loaded.accounts))
                    mutable.value = mutable.value.copy(ready = true, enabled = loaded.enabled,
                        generation = if (loaded.enabled) mutable.value.generation + 1 else mutable.value.generation)
                }
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (_: Exception) {
                mutable.value = RecommendationMode(ready = true, error = "推荐学习记录读取失败，可清除后重试")
            }
            for (snapshot in writes) {
                try {
                    withContext(io) { storage.write(snapshot) }
                } catch (cancelled: CancellationException) { throw cancelled
                } catch (_: Exception) {
                    synchronized(this@RecommendationOptimizer) {
                        mutable.value = mutable.value.copy(error = "推荐设置或学习记录保存失败")
                    }
                }
            }
        }
    }

    @Synchronized fun ticket(): RecommendationTicket {
        syncOwner()
        return RecommendationTicket(currentOwner, state.value.generation)
    }
    @Synchronized fun valid(ticket: RecommendationTicket): Boolean =
        ticket.owner == owner() && ticket.generation == state.value.generation

    @Synchronized fun setEnabled(enabled: Boolean) {
        if (!state.value.ready || state.value.enabled == enabled) return
        invalidate()
        archive = archive.copy(enabled = enabled)
        mutable.value = mutable.value.copy(enabled = enabled, error = null)
        writes.trySend(archive)
    }

    @Synchronized fun clear() {
        if (!state.value.ready) return
        syncOwner()
        invalidate()
        archive = archive.copy(accounts = archive.accounts - currentOwner.first)
        guest.clear()
        mutable.value = mutable.value.copy(error = null)
        writes.trySend(archive)
    }

    @Synchronized fun accountChanged() { syncOwner() }
    private fun syncOwner() {
        val next = owner()
        if (next == currentOwner) return
        currentOwner = next
        guest.clear()
        invalidate()
    }
    private fun invalidate() {
        session.clear(); exposed.clear(); opened.clear()
        mutable.value = mutable.value.copy(generation = mutable.value.generation + 1)
    }

    fun rank(items: List<VideoItem>, previous: List<VideoItem>, ticket: RecommendationTicket): List<VideoItem> {
        val input = synchronized(this) {
            if (!accepts(ticket)) return items
            records() to session.values.toList()
        }
        return try {
            RecommendationRankingPolicy.rank(items.take(120), input.first, input.second, previous, now()) + items.drop(120)
        } catch (cancelled: CancellationException) { throw cancelled
        } catch (_: Exception) {
            log("policy=1 fallback=ranking-failure count=${items.size}")
            items
        }
    }

    @Synchronized fun expose(video: VideoItem, ticket: RecommendationTicket) {
        if (!accepts(ticket) || !exposed.add(video.bvid)) return
        if (exposed.size > 5000) exposed.clear()
        update(video) { it.copy(exposedAt = now()) }
    }

    @Synchronized fun open(video: VideoItem): RecommendationTicket? {
        val ticket = ticket()
        if (!accepts(ticket)) return null
        opened[video.bvid] = ticket to now()
        while (opened.size > 20) opened.remove(opened.keys.first())
        update(video) { it.copy(clickedAt = now()) }
        return ticket
    }

    @Synchronized fun playbackTicket(bvid: String): RecommendationTicket? {
        val (ticket, time) = opened.remove(bvid) ?: return null
        return ticket.takeIf { accepts(it) && now() - time in 0..600_000 }
    }

    @Synchronized fun watch(video: VideoItem, ticket: RecommendationTicket, satisfaction: Double) {
        if (!accepts(ticket) || !satisfaction.isFinite() || satisfaction <= 0) return
        update(video) { it.copy(satisfaction = maxOf(it.satisfaction, satisfaction.coerceIn(0.0, 1.0)), watchedAt = now()) }
        val value = records().firstOrNull { it.bvid == video.bvid } ?: return
        session[video.bvid] = value
        while (session.size > 300) session.remove(session.keys.first())
    }

    private fun accepts(ticket: RecommendationTicket) = state.value.ready && state.value.enabled && valid(ticket)
    private fun records(): List<RecommendationMemory> =
        if (currentOwner.first.isEmpty()) guest.values.toList() else archive.accounts[currentOwner.first].orEmpty()

    private fun update(video: VideoItem, transform: (RecommendationMemory) -> RecommendationMemory) {
        val records = records().associateByTo(linkedMapOf()) { it.bvid }
        val old = records.remove(video.bvid) ?: RecommendationMemory(video.bvid, video.ownerMid, video.categoryName, now())
        records[video.bvid] = transform(old).copy(updatedAt = now())
        if (currentOwner.first.isEmpty()) {
            guest.clear(); guest.putAll(records.entries.toList().takeLast(5000).associate { it.toPair() })
        } else {
            archive = archive.copy(accounts = bounded(archive.accounts + (currentOwner.first to records.values.toList())))
            writes.trySend(archive)
        }
    }

    private fun bounded(accounts: Map<String, List<RecommendationMemory>>): Map<String, List<RecommendationMemory>> {
        val time = now()
        return accounts.flatMap { (key, values) -> values.filter {
            time - it.updatedAt in 0..30 * RecommendationRankingPolicy.DAY
        }.map { key to it } }.sortedByDescending { it.second.updatedAt }.take(5000)
            .groupBy({ it.first }, { it.second })
    }
}
