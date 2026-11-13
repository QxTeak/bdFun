package com.aritxonly.bdfun

import com.aritxonly.teak.common.chrome.LocalTopBarScrollProgress
import com.aritxonly.teak.common.chrome.ProgressiveTopBarMaterial

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.size
import com.aritxonly.bdfun.ui.DynamicTopBar
import com.aritxonly.teak.common.search.TeakSearchTransitionDefaults

@Composable
internal fun androidx.compose.foundation.layout.BoxScope.MainDynamicChrome(shell: MainShellState, pageFloat: Float) {
    val ui = shell.ui
    val session = shell.feeds.session
    val dynamicFeed = shell.feeds.dynamic
    val dynamicChrome = shell.chrome.dynamic
    // 同 MainHomeChrome：一镜到底期间把位移和模糊补偿给顶栏采样。
    val bottomProgress = shell.bottom.bottomSearchProgress.value
    val underlayLiftPx = with(androidx.compose.ui.platform.LocalDensity.current) {
        TeakSearchTransitionDefaults.ContentLift.toPx()
    } * bottomProgress
    val underlayBlurPx = TeakSearchTransitionDefaults.UnderlayBlurPx * shell.bottom.bottomBlurProgress.value
    // 动态页固定顶栏：只有「全部 / 视频」玻璃分段栏。
    // 头像卡片已经放进动态列表里，不再固定在顶栏。
    if (pageFloat > 0.01f && pageFloat < 1.99f &&
        session != null && !ui.showSearch
    ) {
        CompositionLocalProvider(LocalTopBarScrollProgress provides dynamicChrome.materialProgress) {
            ProgressiveTopBarMaterial(
                underlayLiftPx = underlayLiftPx,
                underlayBlurPx = underlayBlurPx,
                modifier = Modifier.align(Alignment.TopCenter)
                    .graphicsLayer {
                        translationX = (1f - pageFloat) * size.width * 0.92f
                        alpha = (1f - kotlin.math.abs(pageFloat - 1f)).coerceIn(0f, 1f)
                    },
            ) {
                DynamicTopBar(
                    state = dynamicFeed,
                    loggedIn = session != null,
                    modifier = Modifier,
                )
            }
        }
    }

}
