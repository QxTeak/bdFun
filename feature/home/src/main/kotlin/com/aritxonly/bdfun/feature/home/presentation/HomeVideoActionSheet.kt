package com.aritxonly.bdfun.feature.home.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import coil.compose.AsyncImage
import com.aritxonly.teak.common.modal.MiuixBottomSheet
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*
import top.yukonga.miuix.kmp.squircle.squircleSurface
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Immutable
internal data class HomeActionSheetUiState(
    val title: String,
    val cover: String,
    val creator: String,
    val category: String,
    val hasCreator: Boolean,
    val categoryLoading: Boolean,
    val localCategoryOnly: Boolean,
    val busy: Boolean,
)

internal typealias HomeVideoAction = com.aritxonly.bdfun.feature.home.application.HomeVideoAction

/** 关闭动画期间保留摘要，防止上层清空选中视频造成内容闪动。 */
@Stable
private class HomeSheetStateHolder(initial: HomeActionSheetUiState) {
    var displayed by mutableStateOf(initial)
        private set
    fun update(show: Boolean, state: HomeActionSheetUiState) { if (show) displayed = state }
}

@Composable
internal fun HomeVideoActionSheet(
    show: Boolean,
    state: HomeActionSheetUiState,
    onDismiss: () -> Unit,
    onAction: (HomeVideoAction) -> Unit,
) {
    val holder = remember { HomeSheetStateHolder(state) }
    SideEffect { holder.update(show, state) }
    val displayed = if (show) state else holder.displayed
    MiuixBottomSheet(
        show = show, title = "视频操作", onDismissRequest = onDismiss,
        allowDismiss = !state.busy,
        startAction = {
            IconButton(onClick = onDismiss, enabled = !state.busy) {
                Icon(MiuixIcons.Close, "关闭")
            }
        },
    ) { padding ->
        val direction = LocalLayoutDirection.current
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp + padding.calculateStartPadding(direction),
                end = 20.dp + padding.calculateEndPadding(direction),
                top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item("video") { VideoSummary(displayed) }
            item("watch-later") {
                SheetGroup {
                    HomeSheetAction(if (displayed.busy) "正在处理…" else "添加至稍后再看",
                        MiuixIcons.SearchDevice, show && !state.busy) { onAction(HomeVideoAction.WatchLater) }
                }
            }
            item("feedback") {
                Column {
                    Text("减少此类推荐", color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        fontSize = 13.sp, modifier = Modifier.padding(start = 16.dp, bottom = 8.dp))
                    SheetGroup {
                        HomeSheetAction("不感兴趣：当前视频", MiuixIcons.Close, show && !state.busy) {
                            onAction(HomeVideoAction.HideVideo)
                        }
                        HomeSheetAction("不感兴趣：UP", MiuixIcons.Contacts,
                            show && !state.busy && displayed.hasCreator) { onAction(HomeVideoAction.HideCreator) }
                        HomeSheetAction("不感兴趣：" + displayed.category.ifBlank {
                            if (displayed.categoryLoading) "分类加载中…" else "分类暂不可用"
                        }, MiuixIcons.Album, show && !state.busy && displayed.category.isNotBlank()) {
                            onAction(HomeVideoAction.HideCategory)
                        }
                    }
                    if (displayed.localCategoryOnly) Text("此推荐源的分类过滤仅在本次浏览生效",
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary, fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp))
                }
            }
        }
    }
}

@Composable
private fun VideoSummary(state: HomeActionSheetUiState) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        AsyncImage(model = state.cover, contentDescription = null, contentScale = ContentScale.Crop,
            modifier = Modifier.width(104.dp).aspectRatio(16f / 10f).clip(RoundedCornerShape(16.dp)))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(state.title, color = MiuixTheme.colorScheme.onSurfaceContainer, fontWeight = FontWeight.Medium,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(state.creator, color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun SheetGroup(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().squircleSurface(
        MiuixTheme.colorScheme.surface.copy(alpha = 0.65f), 24.dp), content = content)
}

@Composable
private fun HomeSheetAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val color = MiuixTheme.colorScheme.onSurfaceContainer.copy(alpha = if (enabled) 1f else 0.45f)
    Row(Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick)
        .heightIn(min = 64.dp).padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Icon(icon, null, Modifier.size(24.dp), tint = color)
        Text(label, color = color, fontSize = 16.sp, modifier = Modifier.weight(1f))
    }
}
