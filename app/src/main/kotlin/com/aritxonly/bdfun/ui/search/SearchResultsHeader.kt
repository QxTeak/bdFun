package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.core.domain.SearchFilterState
import com.aritxonly.teak.common.icons.AppGlyph
import com.aritxonly.teak.common.material.LocalTeakBackdrop
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
    filterOpen: Boolean,
    onFilterOpenChange: (Boolean) -> Unit,
    filters: SearchFilterState,
    onFiltersChange: (SearchFilterState) -> Unit,
    onApplyFilters: (SearchFilterState) -> Unit,
    modifier: Modifier = Modifier,
) {
    val wide = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp >= 600
    val anchor = remember { SearchFilterAnchorStateHolder() }
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
        // 漏斗：真正采样 Tab 后面的内容，和设置页二级菜单一样是 Miuix 玻璃。
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                // 顶栏清空按钮的右边距也是 12dp，这里正好对齐。
                .padding(end = 12.dp)
                .onGloballyPositioned { anchor.bounds = it.boundsInWindow() },
        ) {
            com.aritxonly.teak.common.material.TeakGlassIconButton(
                onClick = { onFilterOpenChange(!filterOpen) },
                size = 40.dp,
                backdrop = LocalTeakBackdrop.current,
            ) {
                FunnelGlyph()
            }
        }
        TeakGlassMenu(
            visible = filterOpen,
            onDismiss = { onFilterOpenChange(false) },
            anchorInWindow = anchor.bounds,
            minWidth = 280.dp,
            maxWidth = 340.dp,
            maxHeight = 520.dp,
        ) {
            FilterMenu(
                value = filters,
                onChange = {
                    onFiltersChange(it)
                    onApplyFilters(it)
                },
            )
        }
    }
}

@Composable
internal fun FunnelGlyph() {
    AppGlyph(MiuixIcons.Filter, MiuixTheme.colorScheme.onBackgroundVariant, size = 17.dp)
}

internal class SearchFilterAnchorStateHolder {
    var bounds by mutableStateOf(androidx.compose.ui.geometry.Rect.Zero)
}
