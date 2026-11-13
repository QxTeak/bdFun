package com.aritxonly.bdfun.feature.mine.application

import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.domain.MineRepository
import com.aritxonly.bdfun.core.model.user.UserProfile
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal data class MineUiState(val profile: UserProfile?, val loading: Boolean = false)

/** 三个读请求沿用原顺序与逐字段兜底，不把补全资料写回登录态。 */
internal class MineStateHolder(
    private val initial: UserProfile?, private val repository: MineRepository,
    private val cache: MineProfileStore, private val currentOwner: () -> Boolean = { true },
) {
    private val mutable = MutableStateFlow(MineUiState(initial))
    val state = mutable.asStateFlow()
    private var closed = false
    private val active get() = !closed && currentOwner()
    fun close() { closed = true; mutable.value = mutable.value.copy(loading = false) }
    suspend fun load() {
        val profile = initial ?: return
        if (!active || mutable.value.loading) return
        cache.cached(profile.mid)?.let { mutable.value = MineUiState(it); return }
        val startedAt = cache.timestamp()
        mutable.value = mutable.value.copy(loading = true)
        try {
            val nav = request { repository.profile() }
            val relation = request { repository.relationStat(profile.mid) }
            val likes = request { repository.likes(profile.mid) }
            val decoration = request { repository.decoration(profile.mid) }
            val selected = nav ?: profile
            val merged = selected.copy(decoration = decoration ?: selected.decoration, stats = selected.stats.copy(
                following = relation?.first ?: profile.following,
                follower = relation?.second ?: profile.follower, likes = likes ?: profile.likes))
            mutable.value = MineUiState(merged)
            cache.put(profile.mid, merged, startedAt)
        } finally { mutable.value = mutable.value.copy(loading = false) }
    }
    private suspend fun <T> request(action: suspend () -> Outcome<T>): T? {
        checkActive()
        val result = action()
        checkActive()
        return (result as? Outcome.Success)?.value
    }
    private suspend fun checkActive() {
        currentCoroutineContext().ensureActive()
        if (!active) throw kotlinx.coroutines.CancellationException("个人页已失效")
    }
}
