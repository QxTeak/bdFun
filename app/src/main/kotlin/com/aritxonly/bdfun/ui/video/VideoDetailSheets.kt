package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/** pendingFolderId 只用于显示“正在保存…”；面板内提交时没有具体单夹，用这个占位。 */
private const val pendingFolderMarker = -1L

@Composable
internal fun VideoDetailSheets(
    ui: VideoDetailUiStateHolder,
    interaction: com.aritxonly.bdfun.feature.video.application.VideoInteractionState,
    actions: VideoDetailViewActions,
) {
    // 投币用 PiliPlus 的整页投币界面（硬币箱 / 角色 / 余额 / 同时点赞）。
    val page = LocalVideoPageState.current
    val session = page.dependencies.comments.session
    // 收藏夹 sheet 同样从播放器下方开始，半屏视频不被遮住。
    val sheetMaxHeight = rememberVideoSheetMaxHeight(
        playerBottomPx = if (ui.fullscreen.value) 0f else page.playerBottomPx,
        fullscreen = ui.fullscreen.value,
    )
    CoinPayOverlay(
        visible = ui.coinSheet.value,
        balance = session.value?.coins,
        alreadyCoined = interaction.coined,
        allowTwoCoins = actions.allowTwoCoins,
        onDismiss = { ui.coinSheet.value = false },
        onConfirm = { count, alsoLike -> actions.doCoin(count, alsoLike) },
    )

    // 收藏夹勾选只在面板内暂存：点「完成」才一次提交，取消/关闭不写服务器。
    var stagedFolders by remember { mutableStateOf<Set<Long>>(emptySet()) }
    LaunchedEffect(ui.favSheet.value) {
        if (ui.favSheet.value) stagedFolders = interaction.folderIds
    }
    com.aritxonly.bdfun.feature.library.presentation.FavoriteFolderSheet(
        show = ui.favSheet.value,
        state = com.aritxonly.bdfun.feature.library.presentation.FavoriteFolderSheetUiState(
            title = "收藏到", description = "勾选收藏夹，点「完成」保存",
            folders = interaction.folders.map { folder ->
                com.aritxonly.bdfun.feature.library.presentation.FavoriteFolderOption(
                    folder.id, folder.title, folder.count, folder.id in stagedFolders,
                )
            },
            loading = interaction.favoritesLoading,
            pendingFolderId = if (interaction.busy && ui.favSheet.value) pendingFolderMarker else null,
        ),
        onDismiss = { if (!interaction.busy) ui.favSheet.value = false },
        onSelect = { id ->
            if (!interaction.busy) {
                stagedFolders = if (id in stagedFolders) stagedFolders - id else stagedFolders + id
            }
        },
        onConfirm = { actions.applyFolders(stagedFolders) },
        maxBodyHeight = sheetMaxHeight,
    )
}
