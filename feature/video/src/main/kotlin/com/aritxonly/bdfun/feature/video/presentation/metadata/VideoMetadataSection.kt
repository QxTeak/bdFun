package com.aritxonly.bdfun.ui

import com.aritxonly.teak.common.chrome.rememberManualProgress

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
    // 加载中不再让内容从下方浮出：先给骨架（灰条 + 白色扫光），数据到了直接替换。
    if (!content.metaReady) {
        VideoMetadataSkeleton()
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
