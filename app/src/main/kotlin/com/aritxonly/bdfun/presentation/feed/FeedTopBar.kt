package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.core.domain.DynamicTabPolicy
import com.aritxonly.bdfun.feature.settings.presentation.LocalSettingsBindings
import com.aritxonly.teak.common.chrome.teakClassicBarGlass
import kotlinx.coroutines.launch

@Composable
fun FeedTopBar(
    state: FeedState,
    loggedIn: Boolean,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val statusTop = rememberStatusBarTop()
    val settings = LocalSettingsBindings.current
    val tabs = DynamicTabPolicy.ordered(settings.dynamicFeedTabs, settings.dynamicFeedTabOrder)
    val keys = tabs.map { it.key }
    // 设置里关掉当前 Tab 后，回到第一个仍可见的 Tab。
    LaunchedEffect(keys, state.type) {
        if (keys.isNotEmpty() && state.type !in keys) {
            state.switchTo(DynamicTabPolicy.fallbackKey(settings.dynamicFeedTabs, settings.dynamicFeedTabOrder, state.type), loggedIn)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(FeedTopBarHeight + statusTop),
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .then(
                    if (com.aritxonly.teak.common.theme.TeakUiStyle.isClassic) {
                        Modifier.teakClassicBarGlass()
                    } else {
                        Modifier.teakBarBlur()
                    },
                ),
        )
        if (com.aritxonly.teak.common.theme.TeakUiStyle.isClassic) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.62f)),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(vertical = 8.dp),
        ) {
            TeakSegmentedBar(
                options = tabs.map { it.title },
                selected = keys.indexOf(state.type).coerceAtLeast(0),
                itemWidth = 64.dp,
                materialAlpha = com.aritxonly.teak.common.chrome.LocalTopBarButtonMaterialProgress.current,
                onSelect = { index ->
                    keys.getOrNull(index)?.let { key ->
                        scope.launch { state.switchTo(key, loggedIn) }
                    }
                },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

/**
 * UP 头像卡片：不再固定在顶栏，而是列表第一项，跟着动态一起滚走。
 * 单个圆角矩形卡片内放「全部动态 + 关注 UP 头像」的横向滚动条。
 */
