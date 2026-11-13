package com.aritxonly.bdfun.feature.home.presentation

import android.os.SystemClock
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalView
import kotlinx.coroutines.delay

/** 连续可见才提交曝光；预取、组合、相邻页和后台均不构成观看证据。 */
@Composable
internal fun HomeExposureEffect(state: HomeFeedState, interactive: Boolean) {
    val view = LocalView.current
    LaunchedEffect(state, interactive, view) {
        val since = mutableMapOf<String, Long>()
        var previousTicket = state.exposureTicket()
        while (true) {
            delay(100)
            val ticket = state.exposureTicket()
            if (!interactive || !view.hasWindowFocus() || !view.isShown || !state.optimizationEnabled || ticket == null) {
                since.clear()
                continue
            }
            if (ticket != previousTicket) { since.clear(); previousTicket = ticket }
            val layout = state.gridState.layoutInfo
            val time = SystemClock.elapsedRealtime()
            val visible = layout.visibleItemsInfo.filter { item ->
                val height = item.size.height
                val start = maxOf(item.offset.y, layout.viewportStartOffset)
                val end = minOf(item.offset.y + height, layout.viewportEndOffset)
                height > 0 && end - start >= height * 0.5f
            }.mapNotNull { it.key as? String }.toSet()
            since.keys.retainAll(visible)
            for (bvid in visible) {
                val started = since.getOrPut(bvid) { time }
                if (time - started >= 800) {
                    state.videos.firstOrNull { it.bvid == bvid }?.let { state.expose(it, ticket) }
                }
            }
        }
    }
}
