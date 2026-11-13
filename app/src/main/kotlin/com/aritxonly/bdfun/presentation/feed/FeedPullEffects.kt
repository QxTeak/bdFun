package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.delay

@Composable
internal fun FeedPullEffects(state: FeedState, ui: FeedPullStateHolder, session: FeedPullSession) {
    val density = LocalDensity.current
    LaunchedEffect(ui.pull) {
        state.pullOffsetDp = with(density) { ui.pull.toDp().value }
        state.pullFraction = (ui.pull / session.thresholdPx).coerceIn(0f, 1f)
    }
    LaunchedEffect(state.refreshing) {
        // 和首页一致：刷新结束后先让小电视收掉，再 260ms 平滑回落
        if (!state.refreshing && ui.pull > 0f) {
            state.pullFraction = 0f
            delay(160)
            session.animatePullTo(0f, 260f)
        }
    }

}
