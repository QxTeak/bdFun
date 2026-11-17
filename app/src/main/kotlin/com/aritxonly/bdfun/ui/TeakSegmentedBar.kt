package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aritxonly.teak.common.navigation.AdaptiveScrollableTabs

/** 兼容旧调用；基础分段控件统一由 TeakUI 提供。 */
@Composable
fun TeakSegmentedBar(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    itemWidth: Dp = 74.dp,
    materialAlpha: Float = 1f,
    position: State<Float>? = null,
    scrollable: Boolean = false,
    maxVisibleItems: Int = 3,
) {
    AdaptiveScrollableTabs(options, selected, onSelect, modifier, itemWidth, materialAlpha, position,
        scrollable, maxVisibleItems)
}
