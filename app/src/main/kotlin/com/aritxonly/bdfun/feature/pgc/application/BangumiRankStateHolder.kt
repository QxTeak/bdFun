package com.aritxonly.bdfun.feature.pgc.application

import com.aritxonly.bdfun.core.common.AppError
import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.domain.PgcRankRepository
import com.aritxonly.bdfun.core.model.pgc.BangumiRankEntry
import com.aritxonly.bdfun.core.model.pgc.BangumiRankTab
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 排行榜的一节：榜名 + 一个或两个榜单（首节「热门动画榜」是番剧+国创合并）。 */
internal data class BangumiRankSection(
    val title: String,
    val tabs: List<BangumiRankTab>,
    val entries: List<BangumiRankEntry> = emptyList(),
    val loading: Boolean = false,
    val loaded: Boolean = false,
    val error: String? = null,
) {
    val key: String get() = tabs.joinToString("-") { it.rankId.toString() }
}

/** 页面一次性需要的状态；展示层只读。 */
internal data class BangumiRankUiState(
    val sections: List<BangumiRankSection> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
)

/**
 * 番剧页「排行榜」状态。
 *
 * 目录是两个 index（番剧 1 / 国创 4），首屏只取前 3 个榜，其余等滚到再取，
 * 避免一次打十几个请求。单榜失败只影响那一节，不整页报错。
 */
internal class BangumiRankStateHolder(private val repository: PgcRankRepository) {

    private val mutableState = MutableStateFlow(BangumiRankUiState())
    val state: StateFlow<BangumiRankUiState> = mutableState.asStateFlow()

    private var requested = false
    private var token = 0

    /**
     * 单节加载用它自己的作用域。
     *
     * 之前是挂在每个 item 的 `LaunchedEffect` 上：item 一滑出屏幕协程就被取消，
     * `loading` 停在 true 或者反复「取消-重启」，表现就是榜单一直空白。
     */
    private val sectionScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** 需要大会员的 season_id；榜单接口没有，靠网页榜单对照补。 */
    private var vipSeasons: Set<Long> = emptySet()

    suspend fun ensureLoaded() {
        if (requested) return
        requested = true
        load()
    }

    suspend fun refresh() {
        if (mutableState.value.loading) return
        load(force = true)
    }

    /** 滚到哪一节就取哪一节；重复调用会被 loaded/loading 挡掉。 */
    fun ensureSection(index: Int) {
        val section = mutableState.value.sections.getOrNull(index) ?: return
        if (section.loaded || section.loading) return
        updateSection(index) { it.copy(loading = true, error = null) }
        val current = token
        sectionScope.launch { loadSection(index, section.tabs, current) }
    }

    /** 用户点「重试」：清掉错误再取一次。 */
    fun retrySection(index: Int) {
        updateSection(index) { it.copy(error = null, loading = false) }
        ensureSection(index)
    }

    private suspend fun loadSection(index: Int, tabs: List<BangumiRankTab>, current: Int) {
        try {
            var failure: String? = null
            val collected = ArrayList<BangumiRankEntry>()
            for (tab in tabs) {
                when (val result = repository.board(tab)) {
                    is Outcome.Success -> collected += result.value.entries
                    is Outcome.Failure -> failure = result.error.readable()
                }
            }
            if (current != token) return

            // 合并榜（番剧热门 + 国创热门）两边的名次各自从 1 开始，直接拼会出现两个 1/2/3，
            // 所以统一按合并后的顺序重编号。
            val merged = collected
                .distinctBy { it.seasonId }
                .sortedBy { it.rank }
                .mapIndexed { position, entry ->
                    entry.copy(rank = position + 1, vip = entry.seasonId in vipSeasons)
                }
            updateSection(index) {
                it.copy(
                    entries = merged,
                    loaded = merged.isNotEmpty(),
                    error = if (merged.isEmpty()) failure ?: "榜单加载失败" else null,
                )
            }
        } finally {
            updateSection(index) { if (it.loading) it.copy(loading = false) else it }
        }
    }

    private suspend fun load(force: Boolean = false) {
        if (!force && mutableState.value.sections.isNotEmpty()) return
        token++
        val current = token
        mutableState.update { it.copy(loading = true, error = null) }
        try {
            val bangumi = repository.tabs(PGC_INDEX_BANGUMI)
            val guochuang = repository.tabs(PGC_INDEX_GUOCHUANG)
            if (current != token) return

            val bangumiTabs = if (bangumi is Outcome.Success) bangumi.value else emptyList()
            val guochuangTabs = if (guochuang is Outcome.Success) guochuang.value else emptyList()
            val failure = when {
                bangumi is Outcome.Failure -> bangumi.error
                guochuang is Outcome.Failure -> guochuang.error
                else -> null
            }
            if (bangumiTabs.isEmpty() && guochuangTabs.isEmpty()) {
                mutableState.update {
                    it.copy(sections = emptyList(), error = failure?.readable() ?: "暂无榜单")
                }
                return
            }
            mutableState.update { it.copy(sections = buildSections(bangumiTabs, guochuangTabs)) }
        } finally {
            if (current == token) mutableState.update { it.copy(loading = false) }
        }

        // 大会员对照表：两个 season_type 各一次；拿不到就当没有会员内容，不影响榜单。
        vipSeasons = loadVipSeasons()
        if (current != token) return

        // 首屏三节；后面的由列表滚到再取。
        for (index in 0 until minOf(FIRST_SCREEN_SECTIONS, mutableState.value.sections.size)) {
            ensureSection(index)
        }
    }

    private suspend fun loadVipSeasons(): Set<Long> {
        val result = HashSet<Long>()
        listOf(PGC_SEASON_TYPE_BANGUMI, PGC_SEASON_TYPE_GUOCHUANG).forEach { seasonType ->
            when (val vip = repository.vipSeasons(seasonType)) {
                is Outcome.Success -> result += vip.value
                is Outcome.Failure -> Unit
            }
        }
        return result
    }

    private fun buildSections(
        bangumi: List<BangumiRankTab>,
        guochuang: List<BangumiRankTab>,
    ): List<BangumiRankSection> {
        val result = ArrayList<BangumiRankSection>(bangumi.size + guochuang.size)
        val hotBangumi = bangumi.firstOrNull()
        val hotGuochuang = guochuang.firstOrNull()
        if (hotBangumi != null || hotGuochuang != null) {
            result += BangumiRankSection(
                title = "热门动画榜",
                tabs = listOfNotNull(hotBangumi, hotGuochuang),
            )
        }
        // 番剧和国创都有「搞笑/热血/奇幻」等同名榜；同名只保留先出现的那个，
        // 否则页面上会出现第二个热血榜、奇幻榜、搞笑榜。
        val seen = HashSet<String>()
        (bangumi.drop(1) + guochuang.drop(1)).forEach { tab ->
            if (seen.add(tab.title)) result += BangumiRankSection(tab.title, listOf(tab))
        }
        return result
    }

    private fun updateSection(index: Int, block: (BangumiRankSection) -> BangumiRankSection) {
        mutableState.update { current ->
            val sections = current.sections
            if (index !in sections.indices) return@update current
            current.copy(
                sections = sections.toMutableList().also { it[index] = block(it[index]) },
            )
        }
    }

    private companion object {
        const val PGC_INDEX_BANGUMI = 1
        const val PGC_INDEX_GUOCHUANG = 4
        const val PGC_SEASON_TYPE_BANGUMI = 1
        const val PGC_SEASON_TYPE_GUOCHUANG = 4
        const val FIRST_SCREEN_SECTIONS = 3
    }
}

/**
 * 错误文案与直播页同款。放在这里而不是跨 feature 引用
 * `feature.live.application.readable()`，是为了不把两个 feature 绑在一起。
 */
internal fun AppError.readable(): String = when (this) {
    is AppError.RiskControl -> "请求被风控，稍后重试"
    AppError.LoginExpired -> "登录已失效"
    is AppError.Network -> message.ifBlank { "网络错误" }
    is AppError.Parsing -> "数据解析失败"
    is AppError.Unknown -> message.ifBlank { "加载失败" }
}
