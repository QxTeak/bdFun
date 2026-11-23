package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable

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

    com.aritxonly.bdfun.feature.library.presentation.FavoriteFolderSheet(
        show = ui.favSheet.value,
        state = com.aritxonly.bdfun.feature.library.presentation.FavoriteFolderSheetUiState(
            title = "收藏到", description = "可选择多个收藏夹，点选后立即保存",
            folders = interaction.folders.map { folder ->
                com.aritxonly.bdfun.feature.library.presentation.FavoriteFolderOption(
                    folder.id, folder.title, folder.count, folder.id in interaction.folderIds,
                )
            },
            loading = interaction.favoritesLoading, pendingFolderId = interaction.pendingFolder,
        ),
        onDismiss = { ui.favSheet.value = false },
        onSelect = actions::toggleFolder,
        maxBodyHeight = sheetMaxHeight,
    )
}
