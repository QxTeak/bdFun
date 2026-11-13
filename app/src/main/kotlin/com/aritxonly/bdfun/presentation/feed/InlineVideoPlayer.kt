package com.aritxonly.bdfun.ui

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.aritxonly.bdfun.AppGraph
import com.aritxonly.bdfun.core.model.video.VideoItem

/** 动态预览仍先显示封面；地址失败后本次组合不重试，不增加额外加载 UI。 */
@Composable
fun InlineVideoPlayer(video: VideoItem, muted: Boolean = true, modifier: Modifier = Modifier) {
    val owner = AppGraph.session.value?.mid
    val controller = remember(owner, video.bvid) { AppGraph.inlineVideoStateHolder(video.bvid) }
    val snapshot by key(controller) { controller.state.collectAsState() }
    DisposableEffect(controller) { onDispose { controller.close() } }
    LaunchedEffect(controller, video.bvid) { controller.load() }
    val loaded = snapshot.info ?: return
    if (snapshot.failed) return
    InlineVideoContent(video.bvid, loaded, muted, modifier)
}
