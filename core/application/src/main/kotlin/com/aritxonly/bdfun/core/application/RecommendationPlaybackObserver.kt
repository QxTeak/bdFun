package com.aritxonly.bdfun.core.application

import com.aritxonly.bdfun.core.domain.RecommendationWatchPolicy
import kotlinx.coroutines.*

/** 只学习从首页点开的播放；轮询共享控制器，不创建播放器或改变播放行为。 */
fun observeRecommendationPlayback(
    optimizer: RecommendationOptimizer, playback: PlaybackSessionStateHolder,
    scope: CoroutineScope, monotonicTime: () -> Long,
): Job = scope.launch {
    var key: Pair<String, RecommendationTicket>? = null
    var watch = RecommendationWatchPolicy()
    var lastPublished = 0.0
    while (isActive) {
        delay(1000)
        val state = playback.state.value
        val video = state.video
        val retained = key?.takeIf { it.first == video?.bvid && optimizer.valid(it.second) && optimizer.state.value.enabled }
        val next = retained ?: video?.let { optimizer.playbackTicket(it.bvid)?.let { ticket -> it.bvid to ticket } }
        if (next != key) {
            key = next
            watch = RecommendationWatchPolicy()
            lastPublished = 0.0
        }
        if (next == null || video == null) continue
        val snapshot = state.playback
        watch.sample(snapshot.positionMs, monotonicTime(), snapshot.playing && snapshot.ready && snapshot.error == null, snapshot.speed)
        val factor = if (state.detached) 0.5 else 1.0
        val satisfaction = watch.satisfaction(snapshot.durationMs) * factor
        if (satisfaction >= lastPublished + 0.05) {
            optimizer.watch(video, next.second, satisfaction)
            lastPublished = satisfaction
        }
    }
}
