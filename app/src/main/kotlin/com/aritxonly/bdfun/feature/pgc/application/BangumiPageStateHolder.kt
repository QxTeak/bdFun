package com.aritxonly.bdfun.feature.pgc.application

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aritxonly.bdfun.core.common.AppError
import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.domain.PgcDiscoverRepository
import com.aritxonly.bdfun.core.model.pgc.BangumiCard
import com.aritxonly.bdfun.feature.live.application.readable

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

    private suspend fun load() {
        token++
        val current = token
        if (cards.isEmpty()) loading = true
        try {
            var failure: AppError? = null
            val followed = when (val result = repository.following(1)) {
                is Outcome.Success -> result.value
                is Outcome.Failure -> emptyList()
            }
            val list = when (val result = repository.recommend(1)) {
                is Outcome.Success -> result.value
                is Outcome.Failure -> { failure = result.error; null }
            }
            if (current != token) return
            following = followed
            if (list == null) {
                if (cards.isEmpty()) error = failure?.readable() ?: "加载失败"
                return
            }
            cards = list
            page = 1
            hasMore = list.isNotEmpty()
            error = null
        } finally {
            if (current == token) loading = false
        }
    }
}
