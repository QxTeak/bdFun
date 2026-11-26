package com.aritxonly.bdfun.ui

import com.aritxonly.teak.common.theme.TeakUiStyle

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal fun androidx.compose.foundation.lazy.LazyListScope.searchSuggestionItems(
    suggestions: List<String>, colors: top.yukonga.miuix.kmp.theme.Colors, onPickKeyword: (String) -> Unit,
) {
    if (suggestions.isNotEmpty()) {
        item(key = "suggest-title") {
            SectionHeader(title = "搜索建议", action = null, onAction = null)
        }
        item(key = "suggest-list") {
            val classic = TeakUiStyle.isClassic
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(TeakUiStyle.design.cardShape(RoundedCornerShape(18.dp)))
                    .background(colors.surfaceContainer)
                    .padding(horizontal = if (classic) 16.dp else 14.dp, vertical = 4.dp),
            ) {
                suggestions.forEach { word ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(interactionSource = null, indication = null) { onPickKeyword(word) }
                            .padding(vertical = 12.dp, horizontal = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SearchSuggestGlyph()
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = word,
                            color = colors.onBackground,
                            fontSize = 13.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

/** 板块之间的留白：热搜 / 搜索记录 / 搜索发现三块统一用它，避免看起来挤在一起。 */
private val SearchSectionGap = 22.dp

/** 热搜：两列（每行两个）排列，和搜索发现共用同一套网格。 */
internal fun androidx.compose.foundation.lazy.LazyListScope.searchHotItems(
    hot: List<com.aritxonly.bdfun.core.domain.SearchHotWord>,
    colors: top.yukonga.miuix.kmp.theme.Colors, onPickKeyword: (String) -> Unit,
) {
    if (hot.isEmpty()) return
    item(key = "hot-title") {
        SectionHeader(title = "热搜", action = null, onAction = null)
    }
    item(key = "hot-grid") {
        val classic = TeakUiStyle.isClassic
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(TeakUiStyle.design.cardShape(RoundedCornerShape(18.dp)))
                .background(colors.surfaceContainer)
                .padding(horizontal = if (classic) 16.dp else 14.dp, vertical = 6.dp),
        ) {
            hot.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    row.forEach { word ->
                        HotWordCell(
                            word = word.showName.ifBlank { word.keyword },
                            badgeIcon = word.icon,
                            onClick = { onPickKeyword(word.keyword) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        Spacer(Modifier.height(SearchSectionGap))
    }
}

/**
 * 搜索历史：一个个胶囊横向排，行放满再换下一行。
 *
 * 首页搜索支持长按进入编辑态（[editing] / [onBeginEdit] / [onRemove] / [onCancelEdit] / [onCommitEdit]）；
 * 媒体库搜索只传 [onClearHistory]，保持「只能一键清空」的原样。
 */
internal fun androidx.compose.foundation.lazy.LazyListScope.searchHistoryItems(
    history: List<String>, colors: top.yukonga.miuix.kmp.theme.Colors, onPickKeyword: (String) -> Unit, onClearHistory: () -> Unit,
    flat: Boolean = false,
    roomy: Boolean = false,
    /** 是否处于编辑态；true 时标题右侧换成「取消」「保存」。 */
    editing: Boolean = false,
    /** 长按某条记录进入编辑；为 null 表示不支持编辑。 */
    onBeginEdit: (() -> Unit)? = null,
    /** 编辑态点叉号删除某条（只改草稿）。 */
    onRemove: ((String) -> Unit)? = null,
    onCancelEdit: () -> Unit = {},
    onCommitEdit: () -> Unit = {},
) {
    if (history.isEmpty() && !editing) return
    item(key = "history-title") {
        if (editing) {
            SectionHeader(
                title = "搜索记录",
                action = "取消",
                onAction = onCancelEdit,
                primaryAction = "保存",
                onPrimaryAction = onCommitEdit,
            )
        } else {
            SectionHeader(
                title = "搜索记录",
                action = "清空",
                onAction = onClearHistory,
            )
        }
    }
    if (history.isEmpty()) {
        // 编辑态里把记录删光了：留一句提示，否则整块凭空消失，用户不知道保存是否生效。
        item(key = "history-empty") {
            Text(
                text = "搜索记录已全部删除",
                color = colors.onBackgroundVariant,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            Spacer(Modifier.height(SearchSectionGap))
        }
        return
    }
    item(key = "history-pills") {
        val classic = TeakUiStyle.isClassic
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (flat) Modifier
                    else Modifier
                        .clip(TeakUiStyle.design.cardShape(RoundedCornerShape(18.dp)))
                        .background(colors.surfaceContainer)
                        .padding(horizontal = if (classic) 16.dp else 14.dp, vertical = 12.dp),
                ),
        ) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(if (roomy) 10.dp else 8.dp),
                verticalArrangement = Arrangement.spacedBy(if (roomy) 12.dp else 8.dp),
            ) {
                history.forEach { word ->
                    HistoryPill(
                        word = word,
                        onClick = { onPickKeyword(word) },
                        roomy = roomy,
                        onLongClick = onBeginEdit,
                        onRemove = if (editing) onRemove?.let { remove -> { remove(word) } } else null,
                    )
                }
            }
        }
        Spacer(Modifier.height(SearchSectionGap))
    }
}

/** 搜索发现：接口取自 PiliPlus 的 `/x/v2/search/recommend`，排版与热搜一致。 */
internal fun androidx.compose.foundation.lazy.LazyListScope.searchDiscoveryItems(
    discovery: List<String>, colors: top.yukonga.miuix.kmp.theme.Colors, onPickKeyword: (String) -> Unit,
) {
    if (discovery.isEmpty()) return
    item(key = "discovery-title") {
        SectionHeader(title = "搜索发现", action = null, onAction = null)
    }
    item(key = "discovery-grid") {
        KeywordGrid(words = discovery, colors = colors, onPickKeyword = onPickKeyword)
    }
}

/** 热搜 / 搜索发现共用的两列词条网格：一行两个，放不满时用等宽空位撑住。 */
@androidx.compose.runtime.Composable
private fun KeywordGrid(
    words: List<String>,
    colors: top.yukonga.miuix.kmp.theme.Colors,
    onPickKeyword: (String) -> Unit,
) {
    val classic = TeakUiStyle.isClassic
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(TeakUiStyle.design.cardShape(RoundedCornerShape(18.dp)))
            .background(colors.surfaceContainer)
            .padding(horizontal = if (classic) 16.dp else 14.dp, vertical = 6.dp),
    ) {
        words.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { word ->
                    HotWordCell(
                        word = word,
                        onClick = { onPickKeyword(word) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}
