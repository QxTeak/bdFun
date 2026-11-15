package com.aritxonly.bdfun.presentation.pgc

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.domain.PgcDiscoverRepository
import com.aritxonly.bdfun.core.model.pgc.BangumiCard
import com.aritxonly.bdfun.presentation.common.discover.readable
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * 首页「番剧」段的状态：最近追番 + 推荐。
 * 刷新/滚动状态与推荐页同一套（shell 绑定 gridState，App 层画下拉小电视）。
 */
internal class BangumiPageStateHolder(private val repository: PgcDiscoverRepository) {
    val gridState = LazyGridState()
    var following by mutableStateOf<List<BangumiCard>>(emptyList())
        private set
    var cards by mutableStateOf<List<BangumiCard>>(emptyList())
        private set
    /** 正在更新的新番时间表；头图优先用它，失败时优雅退回榜单/推荐。 */
    var timeline by mutableStateOf<List<BangumiCard>>(emptyList())
        private set
    var loading by mutableStateOf(false)
        private set
    var refreshing by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var pullFraction by mutableFloatStateOf(0f)
    var pullOffsetDp by mutableFloatStateOf(0f)
    private var page = 1
    private var hasMore = true
    private var token = 0

    suspend fun ensureLoaded() {
        if (cards.isNotEmpty() || loading) return
        load()
    }

    suspend fun refresh() {
        if (refreshing) return
        refreshing = true
        try { load() } finally { refreshing = false }
    }

    suspend fun loadMore() {
        if (loading || refreshing || !hasMore) return
        val next = page + 1
        val current = token
        loading = true
        try {
            when (val result = repository.recommend(next)) {
                is Outcome.Success -> if (current == token) {
                    val more = result.value
                    if (more.isEmpty()) hasMore = false else {
                        cards = cards + more
                        page = next
                    }
                }
                is Outcome.Failure -> Unit
            }
        } finally {
            if (current == token) loading = false
        }
    }

    fun retry() { error = null }

    /**
     * 头图简介后补：时间表先带标题/封面出图，简介拉到一个补一个。
     * 失败不重试到死循环，等下一次刷新时间表时再补。
     */
    suspend fun ensureTimelineIntros() {
        val source = timeline
        if (source.isEmpty() || source.all { it.subtitle.isNotBlank() }) return
        val intros = coroutineScope {
            source.map { card ->
                async { card.seasonId to repository.seasonIntro(card.seasonId) }
            }.map { it.await() }
        }.mapNotNull { (seasonId, result) ->
            val text = (result as? Outcome.Success)?.value?.takeIf { it.isNotBlank() }
            if (text == null) null else seasonId to text
        }.toMap()
        if (intros.isEmpty()) return
        timeline = timeline.map { card ->
            intros[card.seasonId]?.let { card.copy(subtitle = it) } ?: card
        }
    }

    private suspend fun load() {
        token++
        val current = token
        if (cards.isEmpty()) loading = true
        try {
            // 三条线并行：头图时间表最先出，最近追番/推荐各自完成就上屏，互不等待。
            coroutineScope {
                launch {
                    val followed = when (val result = repository.following(1)) {
                        is Outcome.Success -> result.value
                        is Outcome.Failure -> emptyList()
                    }
                    if (current == token) following = followed
                }
                launch {
                    val fresh = when (val result = repository.timeline()) {
                        is Outcome.Success -> result.value
                        is Outcome.Failure -> emptyList()
                    }
                    if (current == token) timeline = fresh
                }
                launch {
                    try {
                        val list = when (val result = repository.recommend(1)) {
                            is Outcome.Success -> result.value
                            is Outcome.Failure -> {
                                if (current == token && cards.isEmpty()) {
                                    error = result.error.readable()
                                }
                                null
                            }
                        }
                        if (current != token || list == null) return@launch
                        cards = list
                        page = 1
                        hasMore = list.isNotEmpty()
                        error = null
                    } finally {
                        if (current == token) loading = false
                    }
                }
            }
        } finally {
            if (current == token) loading = false
        }
    }
}
