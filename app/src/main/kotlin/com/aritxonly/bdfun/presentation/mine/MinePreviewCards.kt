package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aritxonly.bdfun.feature.mine.application.*
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun MinePreviewCard(kind: MinePreviewKind, section: MinePreviewSection, navigation: MineNavigation, retry: () -> Unit) {
    val openAll = when (kind) {
        MinePreviewKind.History -> navigation.onOpenHistory
        MinePreviewKind.Favorites -> navigation.onOpenLibrary
        MinePreviewKind.WatchLater -> navigation.onOpenWatchLater
    }
    val title = when (kind) {
        MinePreviewKind.History -> "历史记录"
        MinePreviewKind.Favorites -> "我的收藏"
        MinePreviewKind.WatchLater -> "稍后再看"
    }
    Column(Modifier.fillMaxWidth().clip(ContinuousCornerShape(22.dp))
        .background(MiuixTheme.colorScheme.surface).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title, Modifier.weight(1f), fontSize = 15.sp, lineHeight = 18.sp, color = MiuixTheme.colorScheme.onBackground,
                minLines = 1, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (section.loaded && section.error != null) Text("重试",
                Modifier.clickable(onClick = retry).padding(horizontal = 6.dp, vertical = 6.dp), fontSize = 12.sp,
                lineHeight = 18.sp, minLines = 1, maxLines = 1, color = MiuixTheme.colorScheme.primary)
            Text("查看全部 ＞", Modifier.clickable(onClick = openAll).padding(vertical = 6.dp),
                fontSize = 12.sp, lineHeight = 18.sp, minLines = 1, maxLines = 1,
                overflow = TextOverflow.Ellipsis, color = MiuixTheme.colorScheme.onBackgroundVariant)
        }
        MinePreviewBody(kind, section, navigation, openAll, retry)
    }
}

@Composable
private fun MinePreviewBody(kind: MinePreviewKind, section: MinePreviewSection, navigation: MineNavigation,
    openAll: () -> Unit, retry: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val videoWidth = ((maxWidth - 12.dp).coerceAtLeast(0.dp) / 2).coerceAtMost(180.dp)
        val density = LocalDensity.current
        val textHeight = with(density) { 18.sp.toDp() * 2 + 16.sp.toDp() }
        val folderRow = maxOf(55.dp, with(density) { 20.sp.toDp() + 16.sp.toDp() } + 6.dp)
        val bodyHeight = if (kind == MinePreviewKind.Favorites) folderRow * MineFolderPreviewLimit + 14.dp * (MineFolderPreviewLimit - 1)
            else videoWidth / (16f / 9f) + textHeight + 12.dp
        // 固定的是与实际内容相同的预留高度；字体缩放和窗口宽度仍参与尺寸计算。
        Box(Modifier.fillMaxWidth().height(bodyHeight), contentAlignment = Alignment.Center) {
            when {
                !section.loaded && section.error != null -> Text("加载失败，点击重试",
                    Modifier.clickable(onClick = retry).padding(12.dp),
                    color = MiuixTheme.colorScheme.primary, fontSize = 13.sp)
                !section.loaded -> MinePreviewSkeleton(kind, videoWidth, folderRow)
                kind == MinePreviewKind.Favorites -> Column(Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)) { MineFolderPreviews(section, openAll) }
                section.entries.isEmpty() -> MinePreviewMessage(
                    if (kind == MinePreviewKind.History) "还没有观看记录" else "还没有待看视频")
                else -> MineVideoPreviews(section, kind == MinePreviewKind.History, navigation, videoWidth)
            }
        }
    }
}

@Composable
private fun MinePreviewSkeleton(kind: MinePreviewKind, videoWidth: Dp, folderRow: Dp) {
    val fill = MiuixTheme.colorScheme.surfaceContainerHigh
    if (kind == MinePreviewKind.Favorites) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            repeat(MineFolderPreviewLimit) {
                Row(Modifier.fillMaxWidth().height(folderRow), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(88.dp, 55.dp).clip(RoundedCornerShape(12.dp)).background(fill))
                    Box(Modifier.fillMaxWidth(0.55f).height(14.dp).clip(RoundedCornerShape(4.dp)).background(fill))
                }
            }
        }
    } else {
        val density = LocalDensity.current
        LazyRow(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp), userScrollEnabled = false) {
            items(MineVideoPreviewLimit) {
                Column(Modifier.width(videoWidth), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(12.dp)).background(fill))
                    Box(Modifier.fillMaxWidth().height(with(density) { 18.sp.toDp() * 2 })) {
                        Box(Modifier.fillMaxWidth(0.85f).height(12.dp).clip(RoundedCornerShape(4.dp)).background(fill))
                    }
                    Box(Modifier.fillMaxWidth(0.5f).height(with(density) { 16.sp.toDp() })
                        .clip(RoundedCornerShape(4.dp)).background(fill))
                }
            }
        }
    }
}

@Composable
private fun MineFolderPreviews(section: MinePreviewSection, openAll: () -> Unit) {
    if (section.folders.isEmpty()) MinePreviewMessage("还没有收藏夹")
    section.folders.forEach { folder ->
        Row(Modifier.fillMaxWidth().clickable(onClick = openAll), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AsyncImage(rememberImageRequest(folder.cover), null, contentScale = ContentScale.Crop,
                modifier = Modifier.size(88.dp, 55.dp).clip(RoundedCornerShape(12.dp))
                    .background(MiuixTheme.colorScheme.surfaceContainerHigh))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(folder.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 14.sp, lineHeight = 20.sp,
                    color = MiuixTheme.colorScheme.onBackground)
                Text("共 ${folder.count} 视频", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp, lineHeight = 16.sp, color = MiuixTheme.colorScheme.onBackgroundVariant)
            }
        }
    }
}

@Composable
private fun MineVideoPreviews(section: MinePreviewSection, history: Boolean, navigation: MineNavigation, width: Dp) {
    LazyRow(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        itemsIndexed(section.entries, key = { index, entry -> "${entry.bvid}-$index" }) { _, entry ->
            Column(Modifier.width(width).clickable { navigation.onOpenVideo(entry.mineVideo(history)) },
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(12.dp))
                    .background(MiuixTheme.colorScheme.surfaceContainerHigh)) {
                    AsyncImage(rememberImageRequest(entry.cover), null, contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize())
                    if (entry.duration > 0) Text(formatDuration(entry.duration), color = Color.White,
                        fontSize = 10.sp, modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp)
                            .clip(RoundedCornerShape(4.dp)).background(Color.Black.copy(alpha = 0.55f))
                            .padding(horizontal = 4.dp, vertical = 2.dp))
                    if (history && entry.duration > 0 && entry.progress > 0) Box(Modifier.align(Alignment.BottomStart)
                        .fillMaxWidth((entry.progress.toFloat() / entry.duration).coerceIn(0f, 1f))
                        .height(3.dp).background(MiuixTheme.colorScheme.primary))
                }
                Text(entry.title, minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 12.sp, lineHeight = 18.sp,
                    color = MiuixTheme.colorScheme.onBackground)
                Text(if (history && entry.progress < 0) "已看完" else if (history && entry.progress > 0)
                    "看到 ${entry.progress / 60}:${(entry.progress % 60).toString().padStart(2, '0')}" else entry.author,
                    minLines = 1, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 11.sp, lineHeight = 16.sp,
                    color = MiuixTheme.colorScheme.onBackgroundVariant)
            }
        }
    }
}

@Composable
private fun MinePreviewMessage(message: String) {
    Text(message, Modifier.padding(vertical = 14.dp), fontSize = 13.sp,
        color = MiuixTheme.colorScheme.onBackgroundVariant)
}
