package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.core.domain.SearchFilterState
import com.aritxonly.teak.common.icons.AppGlyph
import com.aritxonly.teak.common.material.LocalTeakBackdrop
import com.aritxonly.teak.common.material.TeakGlassIconButton
import com.aritxonly.teak.common.navigation.TeakMorphMenuAnchor
import com.aritxonly.teak.common.navigation.TeakPagerTabs
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.*
import top.yukonga.miuix.kmp.icon.extended.*
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun ResultsTabHeader(
    tab: Int,
    onTabChange: (Int) -> Unit,
    position: androidx.compose.runtime.State<Float>,
    filterMenu: SearchFilterMenuController,
    filters: SearchFilterState,
    onFiltersChange: (SearchFilterState) -> Unit,
    onApplyFilters: (SearchFilterState) -> Unit,
    modifier: Modifier = Modifier,
) {
    val wide = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp >= 600
    Box(
        modifier = modifier.height(42.dp),
    ) {
        TeakPagerTabs(
            options = listOf("视频", "番剧", "影视", "用户"),
            selected = tab,
            onSelect = onTabChange,
            itemWidth = 58.dp,
            position = position,
            modifier = Modifier.align(if (wide) Alignment.CenterEnd else Alignment.Center)
                .padding(end = if (wide) 64.dp else 0.dp),
        )
        // 漏斗：和收藏页同款的玻璃形变菜单，从按钮自身矩形长成筛选面板。
        TeakMorphMenuAnchor(
            state = filterMenu.state,
            // 顶栏清空按钮的右边距也是 12dp，这里正好对齐。
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp),
            onClick = filterMenu::requestOpen,
            // 打开交给外部状态 effect：宿主窗口要先组合好，形变起点才有效。
            autoOpen = false,
        ) { onClick ->
            TeakGlassIconButton(
                onClick = onClick,
                size = 40.dp,
                backdrop = LocalTeakBackdrop.current,
            ) {
                FunnelGlyph()
            }
        }
    }
}

@Composable
internal fun FunnelGlyph() {
    AppGlyph(MiuixIcons.Filter, MiuixTheme.colorScheme.onBackgroundVariant, size = 17.dp)
}
