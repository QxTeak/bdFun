package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.aritxonly.bdfun.core.model.user.UserProfile
import com.aritxonly.bdfun.feature.mine.application.*

@Composable
internal fun MineScreen(request: MineScreenRequest, navigation: MineNavigation, dependencies: MineDependencies) {
    val controller = remember(dependencies, request.profile?.mid, request.sessionRevision) { dependencies.createData(request.profile) }
    val state by controller.state.collectAsState()
    androidx.compose.runtime.DisposableEffect(controller) { onDispose { controller.close() } }
    androidx.compose.runtime.LaunchedEffect(controller) { controller.load() }
    MineGrid(request, state.profile ?: request.profile, navigation, dependencies)
}

@Composable
private fun MineGrid(request: MineScreenRequest, shown: UserProfile?, navigation: MineNavigation, dependencies: MineDependencies) {
    val cardShape = remember { ContinuousCornerShape(20.dp) }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(if (maxWidth >= 600.dp) 2 else 1),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 14.dp, top = 28.dp, end = 14.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (request.profile == null) {
                item(key = "login") { MineGuest(cardShape, navigation.onLogin) }
                if (request.showSentHistory) item(key = "sent-history") {
                    MineSentEntry(cardShape, navigation.onOpenSentHistory)
                }
                item(key = "settings-bar") { SettingsBar(onClick = navigation.onOpenSettings) }
                return@LazyVerticalGrid
            }
            if (shown != null) item(key = "header") { MineProfileCard(shown, cardShape, navigation) }
            item(key = "quick") { MineQuickActions(cardShape, navigation) }
            if (request.showSentHistory) item(key = "sent-history") { MineRecent(cardShape, dependencies.recent(), navigation) }
            item(key = "settings-bar") { SettingsBar(onClick = navigation.onOpenSettings) }
        }
    }
}

@Composable
private fun MineSentEntry(cardShape: androidx.compose.ui.graphics.Shape, onOpenSentHistory: () -> Unit) {
    val colors = MiuixTheme.colorScheme
    Column(modifier = Modifier.fillMaxWidth().clip(cardShape).background(colors.surfaceContainer)) {
        MineRow("我的评论与弹幕", onClick = onOpenSentHistory)
    }
}
