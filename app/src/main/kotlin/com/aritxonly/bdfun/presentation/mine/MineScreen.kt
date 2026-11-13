package com.aritxonly.bdfun.ui

import androidx.compose.material3.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.aritxonly.bdfun.core.model.settings.MineLayout
import com.aritxonly.bdfun.feature.mine.application.*
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch

@Composable
internal fun MineScreen(request: MineScreenRequest, navigation: MineNavigation, dependencies: MineDependencies,
    listState: androidx.compose.foundation.lazy.LazyListState) {
    val controller = remember(dependencies, request.profile?.mid, request.sessionRevision) { dependencies.createData(request.profile) }
    val preview = remember(controller) { dependencies.createPreview(request.profile) }
    val state by controller.state.collectAsState()
    val previews by preview.state.collectAsState()
    val layout by dependencies.layout.state.collectAsState()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(controller, preview) { onDispose { controller.close(); preview.close() } }
    LaunchedEffect(controller) { controller.load() }
    LaunchedEffect(preview, layout, lifecycle) {
        if (request.profile != null) lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            preview.load(layout)
            awaitCancellation()
        }
    }
    val scope = rememberCoroutineScope()
    MinePage(request, state.profile ?: request.profile, navigation, dependencies,
        layout, previews, { kind -> scope.launch { preview.retry(kind) } }, listState)
}

@Composable
private fun MinePage(
    request: MineScreenRequest, shown: com.aritxonly.bdfun.core.model.user.UserProfile?,
    navigation: MineNavigation, dependencies: MineDependencies, layout: MineLayout,
    previews: MinePreviewUiState, retry: (MinePreviewKind) -> Unit, listState: androidx.compose.foundation.lazy.LazyListState,
) {
    val shape = remember { ContinuousCornerShape(22.dp) }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 600.dp
        val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        // 避让放进可滚动内容，视口本身铺满窗口，才能滚入状态栏下的渐进模糊层。
        LazyColumn(Modifier.fillMaxSize(), state = listState,
            contentPadding = if (wide) PaddingValues(start = 22.dp, top = statusBarTop + 25.dp, end = 22.dp, bottom = 110.dp)
                else PaddingValues(bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (request.profile == null) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        MineHero(null, navigation, wide)
                        if (wide) MineSettingsCard(navigation.onOpenSettings)
                    }
                }
                if (request.showSentHistory) item { MineContentInset { MineRecent(shape, emptyList(), navigation) } }

            } else if (shown != null) {
                if (wide) item {
                    Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                        Column(Modifier.weight(0.45f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            MineHero(shown, navigation, wide = true)
                            MineSettingsCard(navigation.onOpenSettings)
                        }
                        Column(Modifier.weight(0.55f).padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            MinePageContent(request, navigation, dependencies, layout, previews, retry)
                        }
                    }
                } else {
                    item { MineHero(shown, navigation) }
                    item { MineContentInset { MineShortcuts(navigation) } }
                    if (layout.history) item { MineContentInset { MinePreviewCard(MinePreviewKind.History, previews[MinePreviewKind.History], navigation) { retry(MinePreviewKind.History) } } }
                    if (layout.favorites) item { MineContentInset { MinePreviewCard(MinePreviewKind.Favorites, previews[MinePreviewKind.Favorites], navigation) { retry(MinePreviewKind.Favorites) } } }
                    if (layout.watchLater) item { MineContentInset { MinePreviewCard(MinePreviewKind.WatchLater, previews[MinePreviewKind.WatchLater], navigation) { retry(MinePreviewKind.WatchLater) } } }
                    if (request.showSentHistory) item { MineContentInset { MineRecent(shape, dependencies.recent(), navigation) } }

                }
            }
        }
    }
}

@Composable
private fun MinePageContent(
    request: MineScreenRequest, navigation: MineNavigation, dependencies: MineDependencies,
    layout: MineLayout, previews: MinePreviewUiState, retry: (MinePreviewKind) -> Unit,
) {
    val shape = remember { ContinuousCornerShape(22.dp) }
    MineShortcuts(navigation)
    val enabled = listOf(MinePreviewKind.History to layout.history,
        MinePreviewKind.Favorites to layout.favorites, MinePreviewKind.WatchLater to layout.watchLater)
    enabled.filter { it.second }.forEach { (kind, _) ->
        MinePreviewCard(kind, previews[kind], navigation) { retry(kind) }
    }
    if (request.showSentHistory) MineRecent(shape, dependencies.recent(), navigation)
}

@Composable
private fun MineContentInset(content: @Composable () -> Unit) {
    Box(Modifier.padding(horizontal = 14.dp)) { content() }
}

@Composable
private fun MineShortcuts(navigation: MineNavigation) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("常用功能", Modifier.padding(horizontal = 7.dp), fontSize = 12.sp,
            color = MiuixTheme.colorScheme.onBackgroundVariant)
        MineQuickActions(ContinuousCornerShape(22.dp), navigation)
    }
}
