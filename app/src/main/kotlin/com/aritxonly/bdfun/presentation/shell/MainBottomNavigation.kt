package com.aritxonly.bdfun


import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.size
import com.aritxonly.teak.common.navigation.TeakFloatingTabItem
import com.aritxonly.teak.common.theme.TeakUiStyle
import com.aritxonly.bdfun.feature.settings.presentation.SettingsBindings
import com.aritxonly.bdfun.core.model.video.VideoItem
import com.aritxonly.teak.common.interaction.blockTouches

@Composable
internal fun androidx.compose.foundation.layout.BoxScope.MainBottomNavigation(
    settings: SettingsBindings, dark: Boolean, wideNavigation: Boolean, searchVisible: Boolean,
    tabItems: List<TeakFloatingTabItem>, selectedKey: String,
    onBarItem: (TeakFloatingTabItem) -> Unit, onSearch: () -> Unit, onOpenVideo: (VideoItem) -> Unit,
    onBottomSearch: () -> Unit,
) {
    val visual = rememberMainBottomNavigationVisual(settings, dark, wideNavigation, searchVisible, tabItems.size)
    if (visual.tabBarAlpha > 0.01f) {
        MainBottomNavigationShade(settings, wideNavigation, visual.tabBarAlpha)
        if (wideNavigation) {
            MainWideNavigation(tabItems, selectedKey, onBarItem, visual, onSearch)
        } else if (TeakUiStyle.isClassic) {

            // 经典样式：底栏贴着屏幕底边，上方一条分割线，不做悬浮胶囊。
            ClassicTabBar(
                items = tabItems,
                selectedKey = selectedKey,
                onItemSelected = onBarItem,
                selectedContentColor = visual.barSelectedColor,
                showDivider = true,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = visual.tabBarAlpha
                        translationX = -visual.discSlot * visual.discShift.toPx()
                    }
                    .blockTouches(visual.tabBarCovered)
                    .navigationBarsPadding(),
            )
        } else {
            MainFloatingNavigation(settings, tabItems, selectedKey, onBarItem, visual, onBottomSearch)
        }
        if (visual.discSlot > 0.01f) MainNavigationDisc(settings, visual, onOpenVideo)
    }
}
