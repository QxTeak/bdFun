package com.aritxonly.bdfun

import androidx.compose.runtime.Composable
import com.aritxonly.bdfun.ui.FeedTopBar

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
    val dynamicFeed = shell.feeds.dynamic
    MainTopBarChrome(
        shell = shell,
        pageIndex = 1,
        pageFloat = pageFloat,
        surface = shell.chrome.dynamic,
        // 动态页顶栏没有搜索框，搜索页盖上来时跟着搜索进度淡掉，不要硬切。
        fadesWhenSearchOpens = true,
    ) {
        FeedTopBar(
            state = dynamicFeed,
            loggedIn = session != null,
            modifier = androidx.compose.ui.Modifier,
        )
    }
}
