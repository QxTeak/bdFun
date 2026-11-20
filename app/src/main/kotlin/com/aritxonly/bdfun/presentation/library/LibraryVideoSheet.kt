package com.aritxonly.bdfun.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aritxonly.bdfun.core.model.video.VideoItem
import com.aritxonly.teak.common.modal.MiuixBottomSheet
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.AddFolder
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Folder
import top.yukonga.miuix.kmp.squircle.squircleSurface
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 收藏 / 历史 / 稍后再看长按视频的操作面板。
 *
 * 视觉和首页长按视频的面板同一套（MiuixBottomSheet + squircle 分组），
 * 选项直接取批量管理里的功能：删除 / 移动到 / 复制到。
 */
@Composable
internal fun LibraryVideoSheet(
    show: Boolean,
    video: VideoItem?,
    allowTransfer: Boolean,
    onDismiss: () -> Unit,
    onAction: (String) -> Unit,
) {
    // 关闭动画期间保留最后一条内容，避免面板抽走时文字先空掉。
    val displayed = remember { mutableStateOf<VideoItem?>(null) }
    if (show && video != null) displayed.value = video
    val target = if (show) video else displayed.value
    MiuixBottomSheet(
        show = show && video != null,
        title = "视频操作",
        onDismissRequest = onDismiss,
        startAction = {
            IconButton(onClick = onDismiss) { Icon(MiuixIcons.Close, "关闭") }
        },
    ) { padding ->
        if (target == null) return@MiuixBottomSheet
        val direction = LocalLayoutDirection.current
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = 20.dp + padding.calculateStartPadding(direction),
                    end = 20.dp + padding.calculateEndPadding(direction),
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding() + 20.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SheetVideoSummary(target)
            Column(
                Modifier.fillMaxWidth().squircleSurface(
                    MiuixTheme.colorScheme.surface.copy(alpha = 0.65f), 24.dp,
                ),
            ) {
                SheetAction("删除", MiuixIcons.Delete) { onAction("delete") }
                if (allowTransfer) {
                    SheetAction("移动到收藏夹", MiuixIcons.Folder) { onAction("move") }
                    SheetAction("复制到收藏夹", MiuixIcons.AddFolder) { onAction("copy") }
                }
            }
        }
    }
}

@Composable
private fun SheetVideoSummary(video: VideoItem) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        AsyncImage(
            model = rememberImageRequest(video.cover),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .width(104.dp)
                .aspectRatio(16f / 10f)
                .clip(RoundedCornerShape(16.dp)),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                video.title,
                color = MiuixTheme.colorScheme.onSurfaceContainer,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                video.ownerName,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SheetAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 56.dp)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(icon, null, Modifier.size(22.dp), tint = MiuixTheme.colorScheme.onSurfaceContainer)
        Text(
            label,
            color = MiuixTheme.colorScheme.onSurfaceContainer,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f),
        )
    }
}
