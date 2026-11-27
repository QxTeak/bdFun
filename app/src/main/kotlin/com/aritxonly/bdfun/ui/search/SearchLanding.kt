package com.aritxonly.bdfun.ui

import com.aritxonly.teak.common.theme.TeakUiStyle

import com.aritxonly.teak.common.chrome.TeakScrollContainer
import com.aritxonly.teak.common.chrome.TrackTeakPageScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun SearchLanding(
    history: List<String>,
    hot: List<com.aritxonly.bdfun.core.domain.SearchHotWord>,
    discovery: List<String>,
    query: String,
    suggestions: List<String>,
    onPickKeyword: (String) -> Unit,
    onClearHistory: () -> Unit,
    /**
     * 每次从外面打开搜索页会变化。搜索页关闭后列表可能还没被回收（收起动画期间），
     * 再打开时会带着上次的位置；这里按这个 key 把列表顶回顶部。
     */
    resetKey: Int = 0,
    /** 版式差异（媒体库搜索与首页搜索的差别），合并成一个参数控制。 */
    style: SearchLandingStyle = SearchLandingStyle(),
    /** 首页搜索的记录编辑态；为 null 时不支持长按删除。 */
    historyEdit: HistoryEditSlots? = null,
) {
    val colors = MiuixTheme.colorScheme
    // 内容从顶栏下方开始，但整块可以滚到顶栏下面（顶部留白放 contentPadding 里），
    // 这样滚动时是顶栏的渐进模糊，而不是被一条白边裁断。
    val statusTop = if (style.includeStatusBar) {
        WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    } else {
        0.dp
    }

    val listState = rememberLazyListState()
    TrackTeakPageScroll(listState)
    // 热搜/搜索发现是异步到的：数据插到列表最前面时，带 key 的 LazyColumn 会按「原来第一项」
    // 锚定，列表看起来就停在了热搜中间。所以打开页面后、数据到齐时各顶一次。
    // 这里用落盘的 history.size（编辑态下它不会随草稿变化），
    // 删除某条只是换了渲染的数据源，不会把列表顶回顶部。
    LaunchedEffect(resetKey, hot.size, discovery.size, history.size) {
        listState.scrollToItem(0)
    }
    TeakScrollContainer(listState, Modifier.fillMaxSize()) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = TeakUiStyle.design.contentInset(16.dp),
            // 顶栏高度放进列表留白：列表本身铺满窗口，滚上去的内容由顶栏做渐进模糊。
            // 媒体库搜索的输入栏是页面里真实占位的一行，不再额外留顶栏高度，
            // 搜索记录紧贴输入框下方（只留 6dp）。
            top = statusTop + style.topContentPadding + if (style.includeStatusBar) 14.dp else 6.dp,
            end = TeakUiStyle.design.contentInset(16.dp),
            bottom = 32.dp,
        ),
    ) {
        // 有关键词时优先给联想；联想还没回来/为空时继续显示搜索记录，
        // 不能把整页留白，否则从结果页返回会像“什么都没有”。
        if (query.isNotBlank() && suggestions.isNotEmpty()) {
            searchSuggestionItems(suggestions, colors, onPickKeyword)
            return@LazyColumn
        }
        // 顺序固定：热搜 → 搜索记录 → 搜索发现。
        searchHotItems(hot, colors, onPickKeyword)
        searchHistoryItems(
            // 只有编辑态才用草稿：非编辑态直接读落盘的列表，外部新增记录不会漏。
            history = if (historyEdit?.editing == true) historyEdit.visible else history,
            colors = colors,
            onPickKeyword = onPickKeyword,
            onClearHistory = onClearHistory,
            flat = style.flatHistory,
            roomy = style.roomyHistory,
            editing = historyEdit?.editing == true,
            onBeginEdit = historyEdit?.onBegin,
            selected = historyEdit?.selected.orEmpty(),
            onToggle = historyEdit?.onToggle,
            onDelete = historyEdit?.onDelete,
            onCancelEdit = historyEdit?.onCancel ?: {},
        )
        searchDiscoveryItems(discovery, colors, onPickKeyword)

    }
    }
}

/**
 * 搜索首页的版式差异：首页搜索用默认值，媒体库搜索（收藏 / 历史 / 稍后再看）另传一份。
 *
 * 打包成一个参数是因为 [SearchLanding] 的参数个数有上限（tools/kotlin-audit 的 12 个），
 * 而这三项永远是成套出现的同一件事：媒体库的输入栏是页面里真实占位的一行。
 */
internal data class SearchLandingStyle(
    /** 顶部给搜索栏留出的高度；媒体库搜索的输入栏比首页顶栏矮。 */
    val topContentPadding: androidx.compose.ui.unit.Dp = HomeTopBarHeight,
    /** 外层已经消费过状态栏避让时传 false，避免再垫一条状态栏高度。 */
    val includeStatusBar: Boolean = true,
    /** 媒体库搜索：搜索记录不再套圆角卡片，直接铺在页面背景上。 */
    val flatHistory: Boolean = false,
    /** 媒体库搜索：胶囊略大、行距略宽。 */
    val roomyHistory: Boolean = false,
)

/**
 * 搜索记录编辑态的渲染槽位：谁负责管状态由调用方决定，这里只声明要显示什么。
 * [visible] 在非编辑态下必须等于外面传进来的历史，否则会漏掉刚写入的新记录。
 */
internal class HistoryEditSlots(
    val editing: Boolean,
    val visible: List<String>,
    val selected: Set<String>,
    val onBegin: () -> Unit,
    val onToggle: (String) -> Unit,
    val onDelete: () -> Unit,
    val onCancel: () -> Unit,
    val onCommit: () -> Unit,
)
