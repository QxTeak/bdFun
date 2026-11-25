package com.aritxonly.bdfun.feature.home.presentation

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Rect
import com.aritxonly.bdfun.core.common.*
import com.aritxonly.bdfun.core.model.video.VideoItem
import com.aritxonly.teak.common.navigation.TeakMorphMenuState
import kotlinx.coroutines.launch

@Composable
internal fun HomeVideoActions(
    video: VideoItem?,
    anchor: Rect,
    menuState: TeakMorphMenuState,
    showRectMenu: Boolean,
    state: HomeFeedState,
    onDismiss: () -> Unit,
    onMessage: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val actions = state.actions
    val observed by actions.state.collectAsState()
    LaunchedEffect(video?.bvid) { actions.select(video) }
    val busy = observed.busy
    val uiState = HomeActionSheetUiState(
        title = video?.title.orEmpty(), cover = video?.cover.orEmpty(),
        creator = video?.ownerName.orEmpty(), category = observed.category,
        hasCreator = (video?.ownerMid ?: 0L) > 0,
        categoryLoading = observed.categoryLoading,
        localCategoryOnly = !observed.categoryLoading && observed.category.isNotBlank() && observed.categoryReason == null,
        busy = busy,
    )
    val dismissIfIdle: () -> Unit = { if (!busy) onDismiss() }
    val execute: (HomeVideoAction) -> Unit = { action ->
        scope.launch {
            when (val result = actions.perform(action)) {
                is Outcome.Success -> { onMessage(result.value); onDismiss() }
                is Outcome.Failure -> onMessage(result.error.messageOr("操作失败，请重试"))
                null -> Unit
            }
        }
    }
    // 形变菜单宿主常驻（关到 0 时不可见），确保收起动画完整；长按仍走矩形锚点菜单。
    HomeVideoMorphMenu(menuState, uiState, onDismiss = dismissIfIdle, onAction = execute)
    if (showRectMenu && video != null) {
        HomeVideoActionMenu(
            show = true,
            anchor = anchor,
            state = uiState,
            onDismiss = dismissIfIdle,
            onAction = execute,
        )
    }
}
