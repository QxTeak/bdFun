package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.core.model.video.VideoDetail
import com.aritxonly.bdfun.core.model.video.VideoPage
import com.aritxonly.teak.common.icons.AppGlyph
import com.aritxonly.teak.common.icons.LucideIcons
import com.aritxonly.teak.common.modal.MiuixBottomSheet
import com.aritxonly.teak.common.modal.SheetHeight
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 分 P 入口：卡片左上角「分P」，右上角 Miuix 右箭头；点开是整页分 P 列表。
 */
@Composable
internal fun VideoPagesSection(
    detail: VideoDetail,
    currentCid: Long,
    onPage: (VideoPage) -> Unit,
    cover: @Composable (String, Modifier) -> Unit,
    maxBodyHeight: androidx.compose.ui.unit.Dp,
) {
    var open by remember { mutableStateOf(false) }
    val colors = MiuixTheme.colorScheme
    // 必须自己套一层 Column：调用方把本组件放在 Box 里，Row / Spacer / VideoPageStrip
    // 如果直接作为 Box 的三个子项会全部叠在一起，横向分P条会把头部行盖住、点击也落在条上。
    Column(Modifier.fillMaxWidth()) {
        // 裸标题行：不加卡片底色、不留横向内边距，这样「分P」和下面的分P首卡左边缘对齐，
        // 右边箭头也正好压在右边缘。
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(interactionSource = null, indication = null) { open = true }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "分P",
                color = colors.onBackground,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${detail.pages.size} 个",
                color = colors.onBackgroundVariant,
                fontSize = 12.sp,
            )
            Spacer(Modifier.width(4.dp))
            AppGlyph(MiuixIcons.ChevronForward, colors.onBackgroundVariant, size = 17.dp)
        }
        // 原来的横向分 P 卡片保留；上面的行只负责打开完整列表。
        Spacer(Modifier.height(6.dp))
        VideoPageStrip(
            pages = detail.pages,
            currentCid = currentCid,
            onSelect = onPage,
        )
    }
    // 不能再用 if(open) 包住：那样关闭时整棵组合树被直接移除，sheet 自带的收起动画来不及播。
    VideoPagesSheet(
        show = open,
        detail = detail,
        currentCid = currentCid,
        onDismiss = { open = false },
        onPage = {
            open = false
            onPage(it)
        },
        cover = cover,
        maxBodyHeight = maxBodyHeight,
    )
}

@Composable
private fun VideoPagesSheet(
    show: Boolean,
    detail: VideoDetail,
    currentCid: Long,
    onDismiss: () -> Unit,
    onPage: (VideoPage) -> Unit,
    cover: @Composable (String, Modifier) -> Unit,
    maxBodyHeight: androidx.compose.ui.unit.Dp,
) {
    val colors = MiuixTheme.colorScheme
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val currentIndex = detail.pages.indexOfFirst { it.cid == currentCid }.coerceAtLeast(0)
    // 收藏夹 sheet 是按内容高度给的（最多到播放器下沿），分P 也照这个来：
    // 固定吃满 600dp 会在只有几 P 时留一大片空。
    val rows = detail.pages.size.coerceAtLeast(1)
    val listHeight = 79.dp * rows + 8.dp * (rows - 1)
    val contentHeight = 56.dp + listHeight + 32.dp
    MiuixBottomSheet(
        show = show,
        title = "分P",
        onDismissRequest = onDismiss,
        sheetHeight = SheetHeight.MEDIUM,
        maxBodyHeight = minOf(contentHeight, maxBodyHeight),
        endAction = {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                PageScrollButton(LucideIcons.ArrowUpToLine) { scope.launch { listState.animateScrollToItem(0) } }
                PageScrollButton(LucideIcons.ArrowDownToLine) {
                    scope.launch { listState.animateScrollToItem(detail.pages.lastIndex.coerceAtLeast(0)) }
                }
                PageScrollButton(LucideIcons.LocateFixedFilled) {
                    scope.launch { listState.animateScrollToItem(currentIndex) }
                }
            }
        },
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = padding,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(detail.pages, key = { _, page -> page.cid }) { index, page ->
                VideoPageRow(
                    page = page,
                    index = index,
                    // 每 P 用接口的 first_frame；没给才回退整稿封面（PiliPlus 同规则）。
                    cover = page.firstFrame.ifBlank { detail.cover },
                    publishDate = detail.pubdate,
                    active = page.cid == currentCid,
                    onClick = { onPage(page) },
                    coverSlot = cover,
                )
            }
        }
    }
}

@Composable
private fun PageScrollButton(icon: Int, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            // 和合集标题栏的「回顶/回底/定位」同尺寸（24dp 圆 + 14dp 图标）。
            .size(24.dp)
            .clip(CircleShape)
            .clickable(interactionSource = null, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        AppGlyph(icon, MiuixTheme.colorScheme.onBackgroundVariant, size = 14.dp)
    }
}

/** 与相关推荐同构：左边封面，右侧标题 + 发布日期。 */
@Composable
private fun VideoPageRow(
    page: VideoPage,
    index: Int,
    cover: String,
    publishDate: Long,
    active: Boolean,
    onClick: () -> Unit,
    coverSlot: @Composable (String, Modifier) -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // 列表本身不再一张张套卡片；只有当前选中的 P 才有一层圆角矩形。
            .then(
                if (active) {
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.primary.copy(alpha = 0.10f))
                } else Modifier,
            )
            .clickable(interactionSource = null, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        coverSlot(
            cover,
            Modifier
                .width(112.dp)
                .height(63.dp)
                .clip(RoundedCornerShape(8.dp)),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = page.part.ifBlank { "P${page.page}" },
                color = if (active) colors.primary else colors.onBackground,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "P${index + 1} · ${formatVideoDate(publishDate)} · ${formatPlayTime(page.duration * 1000L)}",
                color = colors.onBackgroundVariant,
                fontSize = 11.5.sp,
                maxLines = 1,
            )
        }
    }
}

private fun formatVideoDate(seconds: Long): String {
    if (seconds <= 0L) return "未知日期"
    return runCatching {
        java.time.Instant.ofEpochSecond(seconds)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate()
            .toString()
    }.getOrDefault("未知日期")
}
