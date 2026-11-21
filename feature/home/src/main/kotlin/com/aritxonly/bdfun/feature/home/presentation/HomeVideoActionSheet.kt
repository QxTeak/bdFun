package com.aritxonly.bdfun.feature.home.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Rect
import com.aritxonly.teak.common.modal.TeakVideoActionMenu
import com.aritxonly.teak.common.modal.TeakVideoMenuItem

@Immutable
internal data class HomeActionSheetUiState(
    val title: String = "",
    val cover: String = "",
    val creator: String = "",
    val category: String = "",
    val hasCreator: Boolean = false,
    val categoryLoading: Boolean = false,
    val localCategoryOnly: Boolean = false,
    val busy: Boolean = false,
)

internal typealias HomeVideoAction = com.aritxonly.bdfun.feature.home.application.HomeVideoAction

/**
 * 首页视频操作：与评论区一样是锚定在卡片上的 Miuix 玻璃下拉菜单，
 * 不再用底部 sheet。长按与右下角省略号打开同一份菜单。
 */
@Composable
internal fun HomeVideoActionMenu(
    show: Boolean,
    anchor: Rect,
    state: HomeActionSheetUiState,
    onDismiss: () -> Unit,
    onAction: (HomeVideoAction) -> Unit,
) {
    val busy = state.busy
    val categoryLabel = state.category.ifBlank {
        if (state.categoryLoading) "分类加载中…" else "分类暂不可用"
    }
    TeakVideoActionMenu(
        visible = show,
        anchorInWindow = anchor,
        onDismiss = onDismiss,
        items = listOf(
            TeakVideoMenuItem(
                label = if (busy) "正在处理…" else "添加至稍后再看",
                enabled = !busy,
            ) { onAction(HomeVideoAction.WatchLater) },
            TeakVideoMenuItem(
                label = "不感兴趣：当前视频",
                enabled = !busy,
            ) { onAction(HomeVideoAction.HideVideo) },
            TeakVideoMenuItem(
                label = "不感兴趣：UP 主",
                enabled = !busy && state.hasCreator,
            ) { onAction(HomeVideoAction.HideCreator) },
            TeakVideoMenuItem(
                label = "不感兴趣：$categoryLabel",
                enabled = !busy && state.category.isNotBlank(),
            ) { onAction(HomeVideoAction.HideCategory) },
        ),
    )
}
