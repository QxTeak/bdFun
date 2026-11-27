package com.aritxonly.bdfun

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import com.aritxonly.bdfun.core.domain.DynamicTabPolicy
import com.aritxonly.bdfun.feature.settings.presentation.LocalSettingsBindings
import com.aritxonly.bdfun.ui.FeedCardMenuHost
import com.aritxonly.bdfun.ui.FeedTopBar
import com.aritxonly.bdfun.ui.showToast

/**
 * 动态页固定顶栏：只有「全部 / 视频 / 番剧」玻璃分段栏。
 * 头像卡片已经放进动态列表里，不再固定在顶栏。
 *
 * 进入 UP 页后这条顶栏整条让位（淡出）：那一页的顶栏是头像行本身，
 * 分类分段不再存在。退出 UP 页时同一条时间线淡回来。
 *
 * 搜索过渡与首页三段共用 [MainTopBarChrome]，不再单独维护一份参数。
 */
@Composable
internal fun androidx.compose.foundation.layout.BoxScope.MainFeedChrome(
    shell: MainShellState,
    pageFloat: Float,
) {
    val session = shell.feeds.session ?: return
    val settings = LocalSettingsBindings.current
    val tabs = DynamicTabPolicy.ordered(settings.dynamicFeedTabs, settings.dynamicFeedTabOrder)
    val selected = tabs.indexOfFirst { it.key == shell.ui.dynamicTab }.coerceAtLeast(0)
    // 读 FeedState 的快照字段：进出 UP 页时这里会自动重组。
    val upFocus = AppGraph.dynamicFeedStateFor(shell.ui.dynamicTab).filterMid != null
    // 和搜索一镜到底的旧 chrome 一样：向内缩到 0.9 再淡出，不是整条硬消失。
    val barProgress by animateFloatAsState(
        targetValue = if (upFocus) 1f else 0f,
        animationSpec = tween(if (settings.animationsEnabled) FeedTopBarSwapMs else 0),
        label = "feedTopBarUpPage",
    )
    val barAlpha = (1f - barProgress) * (1f - barProgress)
    MainTopBarChrome(
        shell = shell,
        pageIndex = 1,
        pageFloat = pageFloat,
        surface = shell.chrome.dynamic,
        // 动态页顶栏没有搜索框，搜索页盖上来时跟着搜索进度淡掉，不要硬切。
        fadesWhenSearchOpens = true,
        extraAlpha = barAlpha,
    ) {
        // 完全淡透后把分段栏整条撤掉：graphicsLayer 的 alpha 不影响命中测试，
        // 留着它的话，UP 页顶栏那颗头像的点击会被这条看不见的分段吃掉。
        if (barAlpha > 0.01f) {
            androidx.compose.foundation.layout.Box(
                modifier = androidx.compose.ui.Modifier
                    .graphicsLayer {
                        val scale = 1f - 0.1f * barProgress
                        scaleX = scale
                        scaleY = scale
                        transformOrigin = TransformOrigin(0.5f, 0.35f)
                    },
            ) {
                FeedTopBar(
                    tabs = tabs,
                    selectedIndex = selected,
                    onSelect = { index ->
                        tabs.getOrNull(index)?.let {
                            // 点任意分段（包括当前项）都算一次明确操作：动态页据此退出 UP 模式。
                            shell.ui.dynamicTabClickTick++
                            shell.ui.dynamicTab = it.key
                        }
                    },
                    modifier = androidx.compose.ui.Modifier,
                    position = shell.ui.dynamicTabPosition,
                )
            }
        }
    }

    // 动态卡片省略号/长按的形变菜单：和首页一样挂在玻璃采样树外，避免 RenderNode 自采样。
    val menuContext = androidx.compose.ui.platform.LocalContext.current
    FeedCardMenuHost(AppGraph.dynamicFeedStateFor(shell.ui.dynamicTab)) {
        menuContext.showToast(it)
    }
}

/** 分段栏让位给头像顶栏的时间：比 UP 页的弹簧短，旧顶栏要在新页升上来之前退干净。 */
private const val FeedTopBarSwapMs = 140
