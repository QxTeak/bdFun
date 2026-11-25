package com.aritxonly.bdfun.feature.home.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.common.messageOr
import com.aritxonly.bdfun.core.model.video.VideoItem
import com.aritxonly.teak.common.navigation.TeakMorphMenuHost
import com.aritxonly.teak.common.navigation.TeakMorphMenuState
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.glass.GlassPopupItem

/**
 * 省略号 / 长按共用的形变菜单宿主。挂在主壳玻璃采样树之外（MainHomeChrome），
 * 否则玻璃会采样到包含自己的那一帧，RenderThread 递归直接闪退。
 */
@Composable
fun HomeVideoMorphHost(state: HomeFeedState, onMessage: (String) -> Unit) {
    var activeState by remember { mutableStateOf<TeakMorphMenuState?>(null) }
    var lastVideo by remember { mutableStateOf<VideoItem?>(null) }
    LaunchedEffect(state.menuState, state.menuVideo) {
        if (state.menuState != null) activeState = state.menuState
        if (state.menuVideo != null) lastVideo = state.menuVideo
    }
    val menuState = activeState ?: return
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
