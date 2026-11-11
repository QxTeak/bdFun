package com.aritxonly.bdfun.feature.library.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.teak.common.modal.MiuixBottomSheet
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Folder
import top.yukonga.miuix.kmp.squircle.squircleSurface
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Immutable
internal data class FavoriteFolderOption(
    val id: Long,
    val title: String,
    val count: Int,
    val selected: Boolean? = null,
)

@Immutable
internal data class FavoriteFolderSheetUiState(
    val title: String,
    val description: String,
    val folders: List<FavoriteFolderOption>,
    val loading: Boolean = false,
    val pendingFolderId: Long? = null,
    val emptyMessage: String = "没有可用的收藏夹",
)

/** 上层结束批量选择时仍保留退场画面，避免标题、数量在关闭途中跳变。 */
@Stable
private class FavoriteSheetStateHolder(initial: FavoriteFolderSheetUiState) {
    var displayed by mutableStateOf(initial)
        private set
    fun update(show: Boolean, state: FavoriteFolderSheetUiState) { if (show) displayed = state }
}

@Composable
internal fun FavoriteFolderSheet(
    show: Boolean,
    state: FavoriteFolderSheetUiState,
    onDismiss: () -> Unit,
    onSelect: (Long) -> Unit,
) {
    val holder = remember { FavoriteSheetStateHolder(state) }
    SideEffect { holder.update(show, state) }
    val displayed = if (show) state else holder.displayed
    val busy = displayed.pendingFolderId != null
    MiuixBottomSheet(
        show = show, title = displayed.title, onDismissRequest = onDismiss, allowDismiss = !busy,
        startAction = {
            IconButton(onClick = onDismiss, enabled = !busy) { Icon(MiuixIcons.Close, "关闭") }
        },
        bottomBar = {
            Button(onClick = onDismiss, enabled = !busy,
                colors = ButtonDefaults.buttonColorsPrimary(), cornerRadius = 20.dp,
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp)) {
                Text(if (busy) "正在保存…" else "完成", color = MiuixTheme.colorScheme.onPrimary)
            }
        },
    ) { padding ->
        val direction = LocalLayoutDirection.current
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp + padding.calculateStartPadding(direction),
                end = 20.dp + padding.calculateEndPadding(direction),
                top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item("hint") {
                Text(displayed.description, color = MiuixTheme.colorScheme.onSurfaceVariantSummary, fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 8.dp))
            }
            if (displayed.loading && displayed.folders.isEmpty()) {
                item("loading") { FolderPlaceholder("正在读取收藏夹…") }
            } else if (displayed.folders.isEmpty()) {
                item("empty") { FolderPlaceholder(displayed.emptyMessage) }
            } else {
                items(displayed.folders, key = { it.id }) { folder ->
                    FolderRow(folder, pending = displayed.pendingFolderId == folder.id,
                        enabled = show && !busy && !displayed.loading, onClick = { onSelect(folder.id) })
                }
            }
        }
    }
}

@Composable
private fun FolderRow(folder: FavoriteFolderOption, pending: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val colors = MiuixTheme.colorScheme
    val selected = folder.selected == true
    val input = if (folder.selected != null) Modifier.toggleable(
        value = selected, enabled = enabled, role = Role.Checkbox, onValueChange = { onClick() },
    ) else Modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick)
    Row(Modifier.fillMaxWidth().squircleSurface(colors.surfaceContainer.copy(alpha = 0.65f), 24.dp)
        .then(input).heightIn(min = 80.dp).padding(18.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Icon(MiuixIcons.Folder, null, Modifier.size(28.dp), tint = if (selected) colors.primary else colors.onSurfaceVariantSummary)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(folder.title, color = colors.onSurface, fontWeight = FontWeight.Medium,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(if (pending) "正在保存…" else "${folder.count} 个内容", fontSize = 12.sp, color = colors.onSurfaceVariantSummary)
        }
        if (folder.selected != null) Checkbox(
            state = if (selected) ToggleableState.On else ToggleableState.Off,
            onClick = null, enabled = enabled, modifier = Modifier.clearAndSetSemantics {},
        )
    }
}

@Composable
private fun FolderPlaceholder(message: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Icon(MiuixIcons.Folder, null, Modifier.size(40.dp), tint = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        Text(message, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
    }
}
