package com.aritxonly.bdfun.feature.home.presentation

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Rect
import com.aritxonly.bdfun.core.common.*
import com.aritxonly.bdfun.core.model.video.VideoItem
import com.aritxonly.teak.common.navigation.TeakMorphMenuHost
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.glass.GlassPopupItem

@Composable
internal fun HomeVideoActions(
    video: VideoItem?,
    anchor: Rect,
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
    // 长按仍走矩形锚点菜单；省略号由主壳里的 HomeVideoMorphHost 负责。
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

/**
 * 省略号形变菜单宿主：必须挂在主壳玻璃采样树之外（MainHomeChrome），
 * 否则玻璃会采样到包含自己的那一帧，RenderThread 递归直接闪退。
 */
@Composable
fun HomeVideoMorphHost(state: HomeFeedState, onMessage: (String) -> Unit) {
    // 收起动画期间宿主还要活着，因此保留最后一次的锚点状态和视频。
    var activeState by remember { mutableStateOf<com.aritxonly.teak.common.navigation.TeakMorphMenuState?>(null) }
    var lastVideo by remember { mutableStateOf<VideoItem?>(null) }
    LaunchedEffect(state.menuState, state.menuVideo) {
        if (state.menuState != null) activeState = state.menuState
        if (state.menuVideo != null) lastVideo = state.menuVideo
    }
    val menuState = state.menuState ?: activeState ?: return
    val video = state.menuVideo ?: lastVideo
    val scope = rememberCoroutineScope()
    val actions = state.actions
    val observed by actions.state.collectAsState()
    val uiState = HomeActionSheetUiState(
        title = video?.title.orEmpty(), cover = video?.cover.orEmpty(),
        creator = video?.ownerName.orEmpty(), category = observed.category,
        hasCreator = (video?.ownerMid ?: 0L) > 0,
        categoryLoading = observed.categoryLoading,
        localCategoryOnly = !observed.categoryLoading && observed.category.isNotBlank() && observed.categoryReason == null,
        busy = observed.busy,
    )
    val items = remember(uiState) { menuItems(uiState) }
    val dismiss: () -> Unit = {
        if (!observed.busy) {
            state.menuVideo = null
            state.menuState = null
        }
    }
    TeakMorphMenuHost(menuState, onDismiss = dismiss) { close ->
        items.forEach { item ->
            GlassPopupItem(
                text = item.label,
                enabled = item.enabled,
                onClick = {
                    if (!item.enabled) return@GlassPopupItem
                    close {
                        scope.launch {
                            when (val result = actions.perform(item.action)) {
                                is Outcome.Success -> { onMessage(result.value); dismiss() }
                                is Outcome.Failure -> onMessage(result.error.messageOr("操作失败，请重试"))
                                null -> Unit
                            }
                        }
                    }
                },
            )
        }
    }
}
