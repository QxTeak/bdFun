package com.aritxonly.bdfun

import androidx.compose.runtime.Composable
import com.aritxonly.bdfun.core.domain.DynamicTabPolicy
import com.aritxonly.bdfun.feature.settings.presentation.LocalSettingsBindings
import com.aritxonly.bdfun.ui.FeedCardMenuHost
import com.aritxonly.bdfun.ui.FeedTopBar
import com.aritxonly.bdfun.ui.showToast

/**
 * 动态页固定顶栏：只有「全部 / 视频 / 番剧」玻璃分段栏。
 * 头像卡片已经放进动态列表里，不再固定在顶栏。
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
    MainTopBarChrome(
        shell = shell,
        pageIndex = 1,
        pageFloat = pageFloat,
        surface = shell.chrome.dynamic,
        // 动态页顶栏没有搜索框，搜索页盖上来时跟着搜索进度淡掉，不要硬切。
        fadesWhenSearchOpens = true,
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

    // 动态卡片省略号/长按的形变菜单：和首页一样挂在玻璃采样树外，避免 RenderNode 自采样。
    val menuContext = androidx.compose.ui.platform.LocalContext.current
    FeedCardMenuHost(AppGraph.dynamicFeedStateFor(shell.ui.dynamicTab)) {
        menuContext.showToast(it)
    }
}
