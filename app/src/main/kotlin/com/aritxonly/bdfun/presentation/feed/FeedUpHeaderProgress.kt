package com.aritxonly.bdfun.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import com.aritxonly.bdfun.feature.feed.presentation.FeedState
import com.aritxonly.teak.common.chrome.TeakChromeDefaults

/** 列表还停在最顶部（含下拉回弹归零之后）。 */
@Composable
internal fun rememberFeedUpAtTop(state: FeedState): State<Boolean> =
    remember(state.listState) {
        derivedStateOf {
            val list = state.listState
            list.firstVisibleItemIndex == 0 && list.firstVisibleItemScrollOffset == 0
        }
    }

/**
 * UP 页顶栏材质的进度：顶部为 0（没有模糊），一离开顶部补到 1。
 *
 * 固定 260ms 补间，所以和手指划多快无关；名字淡出、模糊出现共用这**同一个 State**，
 * 两者天然同一拍。返回 State 而不是 Float，调用方在绘制期读，补间期间不重组。
 */
@Composable
internal fun rememberFeedUpHeaderProgress(
    atTop: Boolean,
    animationsEnabled: Boolean,
): State<Float> = animateFloatAsState(
    targetValue = if (atTop) 0f else 1f,
    animationSpec = tween(
        durationMillis = if (animationsEnabled) {
            TeakChromeDefaults.SmallTitleRevealDurationMs.toInt()
        } else {
            0
        },
        easing = FastOutSlowInEasing,
    ),
    label = "feedUpHeader",
)
