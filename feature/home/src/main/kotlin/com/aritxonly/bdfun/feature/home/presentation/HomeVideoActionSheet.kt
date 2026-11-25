package com.aritxonly.bdfun.feature.home.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Rect
import com.aritxonly.teak.common.modal.TeakVideoActionMenu
import com.aritxonly.teak.common.modal.TeakVideoMenuItem
import com.aritxonly.teak.common.navigation.TeakMorphMenuState

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

internal data class HomeMenuItem(
    val label: String,
    val enabled: Boolean,
    val action: HomeVideoAction,
)

internal fun menuItems(state: HomeActionSheetUiState): List<HomeMenuItem> {
    val busy = state.busy
    val categoryLabel = state.category.ifBlank {
        if (state.categoryLoading) "分类加载中…" else "分类暂不可用"
    }
    return listOf(
        HomeMenuItem(if (busy) "正在处理…" else "添加至稍后再看", !busy, HomeVideoAction.WatchLater),
        HomeMenuItem("不感兴趣：当前视频", !busy, HomeVideoAction.HideVideo),
        HomeMenuItem("不感兴趣：UP 主", !busy && state.hasCreator, HomeVideoAction.HideCreator),
        HomeMenuItem("不感兴趣：$categoryLabel", !busy && state.category.isNotBlank(), HomeVideoAction.HideCategory),
    )
}

/**
 * 首页视频操作：长按仍是锚定在卡片上的矩形玻璃菜单；
 * 省略号改用 Miuix 玻璃形变菜单（和收藏页「更多」同款），图标参与展开/收起动画。
 */
@Composable
internal fun HomeVideoActionMenu(
    show: Boolean,
    anchor: Rect,
    state: HomeActionSheetUiState,
    onDismiss: () -> Unit,
    onAction: (HomeVideoAction) -> Unit,
) {
    val items = remember(state) { menuItems(state) }
    TeakVideoActionMenu(
        visible = show,
        anchorInWindow = anchor,
        onDismiss = onDismiss,
        items = items.map { item ->
            TeakVideoMenuItem(label = item.label, enabled = item.enabled) { onAction(item.action) }
        },
    )
}
