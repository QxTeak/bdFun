package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable

@Composable
internal fun VideoDetailSheets(
    ui: VideoDetailUiStateHolder,
    interaction: com.aritxonly.bdfun.feature.video.application.VideoInteractionState,
    actions: VideoDetailViewActions,
) {
    // 投币不再是底部 sheet：中间浮出两枚硬币卡，点卡片直接投币。
    CoinChoicePopup(
        visible = ui.coinSheet.value,
        onDismiss = { ui.coinSheet.value = false },
        onPick = actions::doCoin,
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
    )
}
