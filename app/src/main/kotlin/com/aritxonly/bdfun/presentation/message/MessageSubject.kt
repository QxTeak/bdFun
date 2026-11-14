package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aritxonly.bdfun.core.model.message.MessageItem
import com.aritxonly.teak.common.chrome.TeakContinuousCornerShape
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 封面只占卡片头部右上角，回复与原评论仍可使用下方完整宽度。 */
@Composable
internal fun MessageCardHeader(item: MessageItem, aggregate: Boolean = false) {
    val cover = item.presentation.originalVideoCover.ifBlank { item.subjectCover }
    Row(Modifier.fillMaxWidth()) {
        Box(Modifier.weight(1f)) { MessageActorHeadline(item, aggregate) }
        if (cover.isNotBlank()) {
            Spacer(Modifier.width(12.dp))
            AsyncImage(
                model = rememberImageRequest(cover),
                contentDescription = if (item.presentation.originalVideoCover.isNotBlank()) "原视频封面" else "原内容封面",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(width = 80.dp, height = 50.dp)
                    .clip(TeakContinuousCornerShape(10.dp))
                    .background(MiuixTheme.colorScheme.surfaceContainerHigh),
            )
        }
    }
}
