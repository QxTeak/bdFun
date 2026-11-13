package com.aritxonly.bdfun

import com.aritxonly.teak.common.chrome.LocalTopBarScrollProgress
import com.aritxonly.teak.common.chrome.ProgressiveTopBarMaterial

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import com.aritxonly.teak.common.material.LocalTeakBackdrop
import com.aritxonly.bdfun.feature.settings.presentation.SettingsBindings
import com.aritxonly.bdfun.ui.HomePullIndicator
import com.aritxonly.bdfun.ui.HomeTopBarHeight

@Composable
internal fun androidx.compose.foundation.layout.BoxScope.MainHomeChrome(settings: SettingsBindings, shell: MainShellState, pageFloat: Float) {
    val ui = shell.ui
    val homeFeed = shell.feeds.home
    val homeChrome = shell.chrome.home
    val searchChrome = shell.chrome.search
    val searchBackdrop = shell.backdrops.search
    // 首页顶栏：挂在采集层之外，所以能直接用底栏同款玻璃（真模糊）+ 渐变式模糊
    // 顶栏跟首页一起走：位置随 pager 偏移平移，透明度同步降低
    if (pageFloat < 0.99f) {
        // 搜索态顶栏改采样搜索页自己的 backdrop：
        // 结果列表从下面滚过时，顶部就是首页同款渐进式模糊。
        CompositionLocalProvider(
            LocalTeakBackdrop provides if (ui.showSearch) {
                searchBackdrop
            } else {
                LocalTeakBackdrop.current
            },
        ) {
            CompositionLocalProvider(
                LocalTopBarScrollProgress provides if (ui.showSearch) {
                    searchChrome.materialProgress
                } else {
                    homeChrome.materialProgress
                },
            ) {
                ProgressiveTopBarMaterial(
                    modifier = Modifier.align(Alignment.TopCenter)
                        .graphicsLayer {
                            val searchScale = 1f - 0.1f * shell.bottom.bottomSearchProgress.value
                            scaleX = searchScale
                            scaleY = searchScale
                            translationX = -pageFloat * size.width * 0.92f
                            alpha = (1f - pageFloat).coerceIn(0f, 1f) * (1f - shell.bottom.bottomTopBarAlpha.value)
                        },
                ) {
                    MainHomeBarContent(settings, shell)
                }
            }
        }

        // 使用须知改由独立的 TermsActivity 在启动时展示。

        // 下拉刷新指示器：独立覆盖层，挂在 App 层，探出顶栏也不会被裁掉。
        // 透明度直接由下拉位移决定：位移 58dp 开始淡入、84dp 全显；
        // 卡片在位移 <46dp 时才会撞到小电视，因此永远追不上。
        HomePullIndicator(
            // 直播 / 番剧还没接入下拉刷新，切到这两段时不要把推荐的下拉指示器带过去。
            reveal = if (ui.homeSection != 0) {
                0f
            } else if (homeFeed.refreshing) {
                1f
            } else {
                ((homeFeed.pullOffsetDp - 58f) / 26f).coerceIn(0f, 1f)
            },
            animated = homeFeed.refreshing,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = HomeTopBarHeight + 4.dp),
        )
    }

}
