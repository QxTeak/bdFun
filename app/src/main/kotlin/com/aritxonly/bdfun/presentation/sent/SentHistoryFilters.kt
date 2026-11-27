package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.feature.sent.application.*
import com.aritxonly.teak.common.navigation.TeakChip
import com.aritxonly.teak.common.navigation.TeakSlidingChipRow

/**
 * 我的评论/弹幕筛选：直接复用直播分区、收藏夹同一套 [TeakSlidingChipRow]，
 * 形状、字号、间距和弹簧切换动画都与直播间分类标签一致。
 */
@Composable
internal fun SentHistoryFilters(state: SentHistoryState, onSelect: (Int) -> Unit) {
    val labels = listOf("全部", "评论", "弹幕")
    // 计数变化时重建 chips；否则一帧内会根据旧计数重新量文字。
    val chips = remember(state.filter, state.count(1), state.count(2)) {
        labels.mapIndexed { index, label -> TeakChip(index, "$label ${state.count(index)}") }
    }
    TeakSlidingChipRow(
        chips = chips,
        selectedKey = state.filter,
        onSelect = { chip -> (chip.key as? Int)?.let(onSelect) },
        bleed = 12.dp,
        verticalPadding = 6.dp,
        modifier = Modifier,
    )
}
