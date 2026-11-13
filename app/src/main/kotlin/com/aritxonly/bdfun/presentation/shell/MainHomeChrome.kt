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
import com.aritxonly.teak.common.search.TeakSearchTransitionDefaults

@Composable
internal fun androidx.compose.foundation.layout.BoxScope.MainHomeChrome(settings: SettingsBindings, shell: MainShellState, pageFloat: Float) {
    val ui = shell.ui
    val homeFeed = shell.feeds.home
    val homeChrome = shell.chrome.home
    val searchChrome = shell.chrome.search
    val searchBackdrop = shell.backdrops.search
    // 底栏搜索的一镜到底会把内容整体上移并模糊；顶栏画在变换之外，
    // 要把这两个量交给它，采样才不会和画面对不上。
    val bottomProgress = shell.bottom.bottomSearchProgress.value
    val underlayLiftPx = with(androidx.compose.ui.platform.LocalDensity.current) {
        TeakSearchTransitionDefaults.ContentLift.toPx()
    } * bottomProgress
    val underlayBlurPx = TeakSearchTransitionDefaults.UnderlayBlurPx * shell.bottom.bottomBlurProgress.value
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
                    underlayLiftPx = underlayLiftPx,
                    underlayBlurPx = underlayBlurPx,
                    modifier = Modifier.align(Alignment.TopCenter)
                        .graphicsLayer {
                            // 不要在这里缩放顶栏。玻璃是拿着 backdrop 纹理按自身坐标采样的，
                            // 一旦父层被缩放，采样区域会跟着缩水，采到的不是正下方的内容，
                            // 边缘就会错出一条带子——底栏搜索动画里那条"撕裂"就是这么来的。
                            // 顶栏本来就在 150ms 内淡出，不缩也看不出来。
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
