package com.aritxonly.bdfun


import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Row
import top.yukonga.miuix.kmp.icon.extended.Search
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import com.aritxonly.teak.common.navigation.TeakFloatingTabBar
import com.aritxonly.teak.common.navigation.TeakFloatingTabBarDefaults
import com.aritxonly.teak.common.navigation.TeakFloatingTabItem
import com.aritxonly.teak.common.navigation.TeakFloatingTabLayout
import com.aritxonly.teak.common.interaction.blockTouches
import top.yukonga.miuix.kmp.icon.MiuixIcons

internal val MainWideNavigationEdgePadding = 20.dp

@Composable
internal fun androidx.compose.foundation.layout.BoxScope.MainWideNavigation(
    tabItems: List<TeakFloatingTabItem>, selectedKey: String,
    onBarItem: (TeakFloatingTabItem) -> Unit, visual: MainBottomNavigationVisual, onSearch: () -> Unit,
) {

    Row(
        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
            .padding(MainWideNavigationEdgePadding)
            .graphicsLayer { alpha = visual.tabBarAlpha }.blockTouches(visual.tabBarCovered),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TeakFloatingTabBar(
            items = tabItems,
            selectedKey = selectedKey,
            onItemSelected = onBarItem,
            layout = TeakFloatingTabLayout.Stacked,
            selectedContentColor = visual.barSelectedColor,
        )
        androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
        com.aritxonly.teak.common.material.TeakGlassIconButton(
            onClick = onSearch,
            size = TeakFloatingTabBarDefaults.CurrentHeight,
        ) {
            Icon(top.yukonga.miuix.kmp.icon.MiuixIcons.Search,
                contentDescription = "搜索", tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(TeakFloatingTabBarDefaults.DetachedIconSize))
        }
    }
}
