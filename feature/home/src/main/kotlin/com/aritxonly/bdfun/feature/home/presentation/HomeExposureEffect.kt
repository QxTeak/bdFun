package com.aritxonly.bdfun.feature.home.presentation

import android.os.SystemClock
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalView
import kotlinx.coroutines.delay

private class HomeExposureDwell(val startedAt: Long, var lastSeenAt: Long)

/** 连续可见才提交曝光；预取、组合、相邻页和后台均不构成观看证据。 */
@Composable
internal fun HomeExposureEffect(state: HomeFeedState, interactive: Boolean) {
    val view = LocalView.current
    LaunchedEffect(state, interactive, view) {
        if (!interactive) return@LaunchedEffect
        val pending = HashMap<String, HomeExposureDwell>()
        val exposed = HashSet<String>()
        var previousTicket = state.exposureTicket()
        while (true) {
            // 停留判定只需要 800ms 的粒度；200ms 轮询足够，也少一半空转。
            delay(200)
            val ticket = state.exposureTicket()
            if (!view.hasWindowFocus() || !view.isShown || !state.optimizationEnabled || ticket == null) {
                pending.clear()
                exposed.clear()
                continue
            }
            if (ticket != previousTicket) {
                pending.clear()
                exposed.clear()
                previousTicket = ticket
            }
            val layout = state.gridState.layoutInfo
            val time = SystemClock.elapsedRealtime()
            for (item in layout.visibleItemsInfo) {
                val bvid = item.key as? String ?: continue
                if (bvid in exposed) continue
                val height = item.size.height
                val start = maxOf(item.offset.y, layout.viewportStartOffset)
                val end = minOf(item.offset.y + height, layout.viewportEndOffset)
                if (height <= 0 || end - start < height * 0.5f) continue
                val dwell = pending.getOrPut(bvid) { HomeExposureDwell(time, time) }
                dwell.lastSeenAt = time
                if (time - dwell.startedAt >= 800) {
                    exposed += bvid
                    pending.remove(bvid)
                    val videos = state.videos
                    // 加载项或列表更新期间，下标与视频下标可能暂时不同。
                    val video = videos.getOrNull(item.index)?.takeIf { it.bvid == bvid }
                        ?: videos.firstOrNull { it.bvid == bvid }
                    if (video != null) state.expose(video, ticket)
                }
            }
            // 只保留这一轮确实可见的候选，持续可见的卡片跨采样复用同一份记录。
            pending.entries.removeAll { it.value.lastSeenAt != time }
        }
    }
}
