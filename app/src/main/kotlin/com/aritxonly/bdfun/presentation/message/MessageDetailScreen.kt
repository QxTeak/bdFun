package com.aritxonly.bdfun.ui

import androidx.compose.runtime.getValue

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.aritxonly.bdfun.AppGraph
import com.aritxonly.bdfun.core.navigation.Route
import com.aritxonly.teak.common.chrome.TeakTopGlassButton
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.aritxonly.teak.common.chrome.TrackTeakPageScroll

@Composable
internal fun MessageDetailScreen(kind: MessageFeedKind, onBack: () -> Unit, initialTab: Int = 0) {
    val context = LocalContext.current
    val owner = AppGraph.session.identity
    val controller = remember(owner, kind) { AppGraph.messageFeedStateHolder(kind) }
    val scope = rememberMessageTaskScope(controller) { controller.close() }
    val snapshot by androidx.compose.runtime.key(controller) { controller.state.collectAsState() }
    val listState = rememberLazyListState()
    val ui = remember(kind, initialTab) { MessageFeedPresentationStateHolder().apply { tab = initialTab } }
    val menu = remember(controller) { MessageFeedMenuStateHolder() }
    val navigate = messageRouteAction()
    val actions = MessageFeedActions(controller, scope, context, menu, navigate)
    TeakActivityScaffold(
        settings = AppGraph.settings,
        onBack = onBack,
        options = ActivityPageOptions(
            title = messageFeedTitle(kind),
            titleAlwaysVisible = true,
            backRevealed = true,
            limitContentWidth = LocalMessageNavigation.current == null,
        ),
        slots = ActivityPageSlots(topBarActions = {
            if (kind == MessageFeedKind.Reply || kind == MessageFeedKind.Like) {
                MessageTopButton("设置") {
                    val type = when {
                        kind == MessageFeedKind.Like -> 19
                        ui.tab == 2 -> 18
                        else -> 17
                    }
                    navigate(Route.MessageTools("settings", type = type))
                }
            }
        }),
    ) {
        RegisterScrollTop(listState, "message_detail")
        TrackTeakPageScroll(listState)
        MessageFeedEffects(controller, ui, scope)
        MessageFeedList(kind, listState, snapshot, ui, menu, actions)
        MessageFeedSheet(kind, snapshot, menu, actions::mutate)
    }
}

@Composable
internal fun MessageTopButton(label: String, onClick: () -> Unit) {
    TeakTopGlassButton(
        onClick = onClick,
        modifier = Modifier.semantics { contentDescription = label },
    ) { Text(label, color = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.onBackground,
        fontSize = if (label.length > 2) 11.sp else 13.sp) }
}
