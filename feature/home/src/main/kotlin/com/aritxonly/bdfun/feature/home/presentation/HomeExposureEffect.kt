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
        val exposed = HashSet<String>()
        var previousTicket = state.exposureTicket()
        while (true) {
            // 停留判定只需要 800ms 的粒度；200ms 轮询足够，也少一半空转。
            delay(200)
            val ticket = state.exposureTicket()
            if (!interactive || !view.hasWindowFocus() || !view.isShown || !state.optimizationEnabled || ticket == null) {
                since.clear()
                exposed.clear()
                continue
            }
            if (ticket != previousTicket) { since.clear(); exposed.clear(); previousTicket = ticket }
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
                if (bvid in exposed) continue
                val started = since.getOrPut(bvid) { time }
                if (time - started >= 800) {
                    exposed += bvid
                    state.videos.firstOrNull { it.bvid == bvid }?.let { state.expose(it, ticket) }
                }
            }
        }
    }
}
