package com.aritxonly.bdfun

import com.aritxonly.teak.common.chrome.TeakChromeDefaults
import com.aritxonly.teak.common.chrome.isWideFoldTopBar

import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.aritxonly.teak.common.navigation.TeakFloatingTabBar
import com.aritxonly.teak.common.navigation.TeakFloatingTabBarDefaults
import com.aritxonly.teak.common.navigation.TeakFloatingTabItem
import com.aritxonly.teak.common.navigation.TeakFloatingTabLayout
import com.aritxonly.bdfun.feature.settings.presentation.SettingsBindings
import com.aritxonly.teak.common.interaction.blockTouches

@Composable
internal fun androidx.compose.foundation.layout.BoxScope.MainFloatingNavigation(
    settings: SettingsBindings, tabItems: List<TeakFloatingTabItem>, selectedKey: String,
    onBarItem: (TeakFloatingTabItem) -> Unit, visual: MainBottomNavigationVisual, onBottomSearch: () -> Unit,
) {

    val wideFold = isWideFoldTopBar()
    val bottomSpacing = if (wideFold) 0.dp else 4.dp
    val hasSearch = settings.interfaceStyle == 2
    val maximumDockWidth = minOf(
        TeakFloatingTabBarDefaults.MaximumWidth,
        TeakFloatingTabBarDefaults.MaximumItemWidth * tabItems.size,
    ) + (if (hasSearch) TeakFloatingTabBarDefaults.Height + TeakFloatingTabBarDefaults.DetachedSpacing else 0.dp) +
        TeakFloatingTabBarDefaults.DockHorizontalPadding * 2
    Column(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .graphicsLayer {
                alpha = visual.tabBarAlpha
                translationX = -visual.discSlot * visual.discShift.toPx()
            }
            .blockTouches(visual.tabBarCovered)
            .navigationBarsPadding()
            .padding(
                start = if (wideFold) 12.dp else 16.dp,
                top = 12.dp,
                end = if (wideFold) 12.dp else 16.dp,
                bottom = if (settings.bottomBarRaise) {
                    bottomSpacing + TeakChromeDefaults.RaisedBottomBarExtra
                } else {
                    bottomSpacing
                },
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.wrapContentWidth().widthIn(max = maximumDockWidth).fillMaxWidth()
                .padding(horizontal = TeakFloatingTabBarDefaults.DockHorizontalPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TeakFloatingTabBar(
                items = tabItems,
                selectedKey = selectedKey,
                onItemSelected = onBarItem,
                modifier = Modifier.weight(1f, fill = false),
                layout = TeakFloatingTabLayout.Stacked,
                selectedContentColor = visual.barSelectedColor,
            )
            if (hasSearch) {
                androidx.compose.foundation.layout.Spacer(Modifier.width(TeakFloatingTabBarDefaults.DetachedSpacing))
                MainBottomSearchButton(visual.tabBarAlpha, onBottomSearch)
            }
        }
    }
}
