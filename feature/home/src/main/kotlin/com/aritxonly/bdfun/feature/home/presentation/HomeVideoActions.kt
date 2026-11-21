package com.aritxonly.bdfun.feature.home.presentation

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Rect
import com.aritxonly.bdfun.core.common.*
import com.aritxonly.bdfun.core.model.video.VideoItem
import kotlinx.coroutines.launch

@Composable
internal fun HomeVideoActions(
    video: VideoItem?,
    anchor: Rect,
    state: HomeFeedState,
    onDismiss: () -> Unit,
    onMessage: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val actions = state.actions
    val observed by actions.state.collectAsState()
    LaunchedEffect(video?.bvid) { actions.select(video) }
    HomeVideoActionMenu(
        show = video != null,
        anchor = anchor,
        state = HomeActionSheetUiState(
            title = video?.title.orEmpty(), cover = video?.cover.orEmpty(),
            creator = video?.ownerName.orEmpty(), category = observed.category,
            hasCreator = (video?.ownerMid ?: 0L) > 0,
            categoryLoading = observed.categoryLoading,
            localCategoryOnly = !observed.categoryLoading && observed.category.isNotBlank() && observed.categoryReason == null,
            busy = observed.busy,
        ),
        onDismiss = { if (!observed.busy) onDismiss() },
    ) { action ->
        scope.launch {
            when (val result = actions.perform(action)) {
                is Outcome.Success -> { onMessage(result.value); onDismiss() }
                is Outcome.Failure -> onMessage(result.error.messageOr("操作失败，请重试"))
                null -> Unit
            }
        }
    }
}
