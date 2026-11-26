package com.aritxonly.bdfun.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.core.model.library.FavoriteFolder
import com.aritxonly.bdfun.feature.library.application.*
import com.aritxonly.teak.common.navigation.TeakChip
import com.aritxonly.teak.common.navigation.TeakSlidingChipRow
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 收藏夹标签：和直播分区同一套 [TeakSlidingChipRow]——胶囊彼此分开，
 * 选中底色用弹簧在它们之间滑动，不再是点谁谁瞬间变色。
 */
@Composable
internal fun LibraryFolderBar(
    folders: List<FavoriteFolder>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    val selectedId = folders.getOrNull(selectedIndex)?.id
    // 稳定 chips 列表，避免每次重组都让 TeakSlidingChipRow 重新量文字。
    val chips = remember(folders) { folders.map { TeakChip(it.id, "${it.title} ${it.count}") } }
    TeakSlidingChipRow(
        chips = chips,
        selectedKey = selectedId,
        onSelect = { chip ->
            folders.indexOfFirst { it.id == chip.key }.takeIf { it >= 0 }?.let(onSelect)
        },
        // 抵消网格的 12dp 内容边距，滚到屏幕边缘再被裁切。
        bleed = 12.dp,
        verticalPadding = 6.dp,
    )
}

/** 历史页分类胶囊：与收藏夹胶囊同一套滑动指示器，只负责筛选当前已加载的历史条目。 */
@Composable
internal fun LibraryHistoryCategoryBar(
    selected: HistoryCategory,
    onSelect: (HistoryCategory) -> Unit,
) {
    val categories = HistoryCategory.entries
    val chips = remember { categories.map { TeakChip(it.name, it.label) } }
    TeakSlidingChipRow(
        chips = chips,
        selectedKey = selected.name,
        onSelect = { chip -> categories.firstOrNull { it.name == chip.key }?.let(onSelect) },
        bleed = 12.dp,
        verticalPadding = 6.dp,
    )
}
