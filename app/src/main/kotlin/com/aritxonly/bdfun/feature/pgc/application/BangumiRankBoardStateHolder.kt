package com.aritxonly.bdfun.feature.pgc.application

import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.domain.PgcRankRepository
import com.aritxonly.bdfun.core.model.pgc.BangumiRankEntry
import com.aritxonly.bdfun.core.model.pgc.BangumiRankTab
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** 单榜全量页的状态：一个 tab 只取一次，失败可重试。 */
internal data class BangumiRankBoardUiState(
    val note: String = "",
    val entries: List<BangumiRankEntry> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
)

/**
 * 「查看更多」进去的单榜页状态。
 *
 * 榜单接口一次给整榜（番剧热门榜 99 条），没有分页，所以这里只加载一次。
 */
internal class BangumiRankBoardStateHolder(
    private val repository: PgcRankRepository,
    private val tab: BangumiRankTab,
) {
    private val mutableState = MutableStateFlow(BangumiRankBoardUiState())
    val state: StateFlow<BangumiRankBoardUiState> = mutableState.asStateFlow()

    private var requested = false
    private var token = 0

    suspend fun ensureLoaded() {
        if (requested) return
        requested = true
        load()
    }

    suspend fun retry() {
        if (mutableState.value.loading) return
        requested = true
        load()
    }

    private suspend fun load() {
        token++
        val current = token
        mutableState.update { it.copy(loading = true, error = null) }
        try {
            when (val result = repository.board(tab)) {
                is Outcome.Success -> {
                    if (current != token) return
                    mutableState.update {
                        it.copy(note = result.value.note, entries = result.value.entries, error = null)
                    }
                }
                is Outcome.Failure -> {
                    if (current != token) return
                    mutableState.update { it.copy(error = result.error.readable()) }
                }
            }
        } finally {
            if (current == token) mutableState.update { it.copy(loading = false) }
        }
    }
}
