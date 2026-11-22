package com.aritxonly.bdfun.ui

import com.aritxonly.teak.common.chrome.rememberManualProgress

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.core.model.video.VideoDetail
import com.aritxonly.bdfun.core.model.video.VideoItem
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

data class VideoMetadataContent(
    val detail: VideoDetail?, val video: VideoItem, val metaReady: Boolean,
    val upFollowers: Long, val upVideos: Long, val onlineCount: Long, val onlineText: String,
)
class VideoMetadataActions(
    val toggleFollow: () -> Unit, val toggleLike: () -> Unit, val toggleDislike: () -> Unit,
    val giveCoin: () -> Unit, val toggleFavorite: () -> Unit, val shareVideo: () -> Unit,
    val tripleAction: () -> Unit, val onOpenSpace: () -> Unit,
)
class VideoDescriptionStateHolder {
    var expanded by mutableStateOf(false)
    var expandable by mutableStateOf(false)
    fun reset() { expanded = false; expandable = false }
}

@Composable
fun VideoMetadataSection(
    content: VideoMetadataContent,
    interaction: com.aritxonly.bdfun.feature.video.application.VideoInteractionState,
    description: VideoDescriptionStateHolder, actions: VideoMetadataActions,
    appearance: VideoMetadataAppearance, slots: VideoMetadataSlots,
) {
    // 加载中：图标放在「播放器下方」靠上的位置，而不是剩余区域的几何中点（那样会显得偏低）。
    if (!content.metaReady) {
        val window = androidx.compose.ui.platform.LocalWindowInfo.current.containerSize
        val density = androidx.compose.ui.platform.LocalDensity.current
        val screenHeight = with(density) { window.height.toDp() }
        val screenWidth = with(density) { window.width.toDp() }
        val statusTop = androidx.compose.foundation.layout.WindowInsets.statusBars
            .asPaddingValues().calculateTopPadding()
        // 竖屏播放器按 16:9 占满宽度，播放器底边以下的可见区域就是剩余高度。
        val playerBottom = statusTop + screenWidth * 9f / 16f
        // 目标：图标落在约 45% 屏高处，既在播放器下方，也不至于低到接近评论条。
        val areaHeight = (screenHeight * 0.9f - playerBottom * 2f)
            .coerceAtLeast(160.dp)
        androidx.compose.foundation.layout.Box(
            Modifier.fillMaxWidth().height(areaHeight),
            contentAlignment = androidx.compose.ui.Alignment.Center,
        ) {
            com.aritxonly.teak.common.chrome.TeakLoadingIndicator()
        }
        return
    }
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
        VideoMetadataAuthor(content, interaction, actions, appearance, slots)
        Spacer(Modifier.height(10.dp))
        VideoMetadataTitle(content)
        Spacer(Modifier.height(4.dp))
        VideoMetadataStatistics(content, slots)
        val descText = content.detail?.desc?.takeIf { it.isNotBlank() }
        val descProgress = rememberManualProgress(
            if (description.expanded) 1f else 0f,
            durationMs = 420f,
        )
        Spacer(Modifier.height(4.dp))
        VideoMetadataIdentifier(content, description, descText, descProgress)
        VideoMetadataDescription(descText, description, slots)
        VideoMetadataCategory(content)
        Spacer(Modifier.height(14.dp))
        VideoMetadataInteractions(interaction, actions, appearance, slots)
    }
}
