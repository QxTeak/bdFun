package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aritxonly.bdfun.core.model.message.MessageItem
import com.aritxonly.teak.common.chrome.TeakContinuousCornerShape
import com.aritxonly.teak.common.icons.AppGlyph
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Play
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 封面只占卡片头部右上角，回复与原评论仍可使用下方完整宽度。 */
@Composable
internal fun MessageCardHeader(item: MessageItem, aggregate: Boolean = false) {
    val colors = MiuixTheme.colorScheme
    val cover = item.presentation.originalVideoCover.ifBlank { item.subjectCover }
    // 只有确实指向视频（或本来就有封面）的消息才占这一格；
    // 认不出视频来源时用占位图标，避免卡片头部忽高忽低。
    val videoRelated = cover.isNotBlank() || item.bvid.isNotBlank() || item.videoSubject
    Row(Modifier.fillMaxWidth()) {
        Box(Modifier.weight(1f)) { MessageActorHeadline(item, aggregate) }
        if (videoRelated) {
            Spacer(Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .size(width = 80.dp, height = 50.dp)
                    .clip(TeakContinuousCornerShape(10.dp))
                    .background(colors.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                if (cover.isNotBlank()) {
                    AsyncImage(
                        model = rememberImageRequest(cover),
                        contentDescription = if (item.presentation.originalVideoCover.isNotBlank()) {
                            "原视频封面"
                        } else {
                            "原内容封面"
                        },
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    AppGlyph(
                        icon = MiuixIcons.Play,
                        tint = colors.onBackgroundVariant.copy(alpha = 0.4f),
                        size = 20.dp,
                        contentDescription = "暂无封面",
                    )
                }
            }
        }
    }
}
