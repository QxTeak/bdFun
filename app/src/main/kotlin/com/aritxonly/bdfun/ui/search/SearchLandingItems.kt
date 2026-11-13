package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(colors.surfaceContainer)
                    .padding(horizontal = 14.dp, vertical = 4.dp),
            ) {
                suggestions.forEach { word ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onPickKeyword(word) }
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

internal fun androidx.compose.foundation.lazy.LazyListScope.searchHistoryItems(
    history: List<String>, colors: top.yukonga.miuix.kmp.theme.Colors, onPickKeyword: (String) -> Unit, onClearHistory: () -> Unit,
) {
    if (history.isNotEmpty()) {
        item(key = "history-title") {
            SectionHeader(
                title = "搜索记录",
                action = "清空",
                onAction = onClearHistory,
            )
        }
        item(key = "history-grid") {
            // 与热搜一致的圆角卡片，但去掉排名序号，改成双排词条。
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(colors.surfaceContainer)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                history.chunked(2).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        row.forEach { word ->
                            HistoryWord(
                                word = word,
                                onClick = { onPickKeyword(word) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
        }
    }

}

internal fun androidx.compose.foundation.lazy.LazyListScope.searchHotItems(
    hot: List<String>, colors: top.yukonga.miuix.kmp.theme.Colors, onPickKeyword: (String) -> Unit,
) {
    if (hot.isNotEmpty()) {
        item(key = "hot-title") {
            SectionHeader(title = "热搜", action = null, onAction = null)
        }
        item(key = "hot-list") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(colors.surfaceContainer)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            ) {
                hot.forEachIndexed { index, word ->
                    HotRow(
                        rank = index + 1,
                        word = word,
                        onClick = { onPickKeyword(word) },
                    )
                }
            }
        }
    }}
