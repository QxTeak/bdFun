package com.aritxonly.bdfun.ui

import androidx.compose.runtime.getValue

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.aritxonly.bdfun.AppGraph
import com.aritxonly.teak.common.chrome.TrackTeakPageScroll

@Composable
fun MessageScreen(
    loggedIn: Boolean,
    onNeedLogin: () -> Unit,
    onBack: () -> Unit,
    onOpenReplies: () -> Unit = {},
    onOpenLikes: () -> Unit = {},
    onOpenFans: () -> Unit = {},
    pageType: Int = 1, title: String = "消息",
) {
    val chrome = LocalPageChromeState.current
    val context = LocalContext.current
    val owner = AppGraph.session.identity
    val controller = remember(owner, loggedIn, pageType) { AppGraph.messageSessionsStateHolder(pageType) }
    val scope = rememberMessageTaskScope(controller) { controller.close() }
    val snapshot by androidx.compose.runtime.key(controller) { controller.state.collectAsState() }
    val listState = rememberLazyListState()
    val ui = remember { MessageSessionsPresentationStateHolder(chrome.readAllRequest, chrome.moreRequest) }
    val menu = remember(controller) { MessageSessionMenuStateHolder() }
    val events = MessageScreenEvents(onNeedLogin, onOpenReplies, onOpenLikes, onOpenFans)
    val actions = MessageSessionActions(controller, scope, context, loggedIn, menu)
    RegisterScrollTop(listState, "message")
    MessageSessionsEffects(controller, loggedIn, ui, menu, scope)
    BackHandler(enabled = LocalMessageNavigation.current == null, onBack = onBack)
    TrackTeakPageScroll(listState)
    MessageSessionList(listState, loggedIn, snapshot, ui, menu, events, actions::loadFooter, title, pageType, actions::load)
    MessageSessionSheets(snapshot, menu, onRefresh = { actions.load() },
        onReadAll = actions::readAll, onClearList = actions::clearList, onMutate = actions::mutate)
}
