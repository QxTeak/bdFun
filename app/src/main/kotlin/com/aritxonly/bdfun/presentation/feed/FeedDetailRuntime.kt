package com.aritxonly.bdfun

import com.aritxonly.bdfun.navigateToVideo
import com.aritxonly.bdfun.navigateToFeed
import android.content.Context
import androidx.compose.runtime.*
import com.aritxonly.bdfun.core.model.feed.FeedItem
import com.aritxonly.bdfun.core.model.video.VideoItem
import com.aritxonly.bdfun.feature.feed.application.FeedDetailStateHolder
import com.aritxonly.bdfun.ui.showToast
import kotlinx.coroutines.*

internal data class FeedDetailRuntime(val controller: FeedDetailStateHolder, val scope: CoroutineScope)

@Composable
internal fun rememberFeedDetailRuntime(dynamic: FeedItem): FeedDetailRuntime {
    val owner = AppGraph.session.value?.mid
    val parent = rememberCoroutineScope()
    val controller = remember(owner, dynamic.id) { AppGraph.dynamicDetailStateHolder(dynamic) }
    val scope = remember(controller) { CoroutineScope(parent.coroutineContext + SupervisorJob(parent.coroutineContext[Job])) }
    DisposableEffect(controller) { onDispose { scope.cancel(); controller.close() } }
    return FeedDetailRuntime(controller, scope)
}

internal class FeedDetailActions(
    private val controller: FeedDetailStateHolder,
    private val scope: CoroutineScope,
    private val context: Context,
) {
    fun like() {
        val next = controller.beginLike() ?: return
        scope.launch { controller.commitLike(next)?.let { context.showToast(it) } }
    }
    fun openVideo(video: VideoItem) { navigateToVideo(context, video) }
    fun openForwarded(item: FeedItem) { navigateToFeed(context, item) }
}
