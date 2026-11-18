package com.aritxonly.bdfun.ui

import android.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import androidx.core.graphics.drawable.toBitmap
import com.android.purebilibili.danmaku.engine.DanmakuTrailingAction
import com.aritxonly.bdfun.core.domain.isHighLikeScrollingDanmaku
import com.aritxonly.bdfun.core.model.video.DanmakuItem
import kotlinx.coroutines.delay

private const val VOTE_WINDOW_MS = 5_000L

@Composable
internal fun rememberScrollingDanmakuLike(hit: PlayerDanmakuHit, cid: Long): DanmakuTrailingAction {
    val resources = LocalResources.current
    val configuration = LocalConfiguration.current
    val page = LocalVideoPageState.current
    val icon = remember(resources, configuration) {
        resources.getDrawable(TeakIcon.like(true), null).toBitmap(64, 64)
    }
    return remember(icon, hit, page, cid) {
        DanmakuTrailingAction(
            icon, Color.WHITE, 0xFFFFC98D.toInt(),
            isActive = { id -> hit.votes.value[id]?.liked == true },
            onClick = { id -> if (page.cid == cid) hit.like?.invoke(id) },
        )
    }
}

@Composable
internal fun PrefetchScrollingDanmakuVotes(
    items: List<DanmakuItem>, cid: Long, positionMs: () -> Long, hit: PlayerDanmakuHit,
) {
    // 预读当前位置附近的点赞状态，跳播时也只请求新位置附近的条目。
    val windows = remember(items) {
        items.asSequence().filter(::isHighLikeScrollingDanmaku)
            .groupBy({ it.timeMs.coerceAtLeast(0L) / VOTE_WINDOW_MS }, { it.id })
    }
    val position = rememberUpdatedState(positionMs)
    LaunchedEffect(windows, cid, hit) {
        if (windows.isEmpty() || cid <= 0) return@LaunchedEffect
        val requested = mutableSetOf<Long>()
        var lastWindow = -1L
        while (true) {
            val window = position.value().coerceAtLeast(0L) / VOTE_WINDOW_MS
            val loadVotes = hit.loadVotes
            if (window != lastWindow && loadVotes != null) {
                lastWindow = window
                val nearby = (window - 1L..window + 1L).flatMap { windows[it].orEmpty() }
                nearby.filter(requested::add).chunked(20).forEach { loadVotes(it) }
            }
            delay(500L)
        }
    }
}
