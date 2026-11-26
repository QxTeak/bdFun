package com.aritxonly.bdfun.ui

import com.aritxonly.teak.common.theme.TeakUiStyle

import com.aritxonly.bdfun.feature.message.application.visibleSessions
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.presentation.common.pullrefresh.HomePullRefreshBox
import com.aritxonly.teak.common.chrome.TeakChromeDefaults
import com.aritxonly.teak.common.chrome.TeakLargeTitle

@Composable
internal fun MessageSessionList(
    listState: androidx.compose.foundation.lazy.LazyListState,
    loggedIn: Boolean,
    snapshot: com.aritxonly.bdfun.feature.message.application.MessageSessionsUiState,
    ui: MessageSessionsPresentationStateHolder,
    menu: MessageSessionMenuStateHolder,
    events: MessageScreenEvents,
    onLoad: () -> Unit,
    title: String = "消息", pageType: Int = 1, onRefresh: () -> Unit = {},
) {
    val context = LocalContext.current
    val navigation = LocalMessageNavigation.current
    val openRoute = messageRouteAction()
    val visibleSessions = snapshot.visibleSessions(ui.tabIndex == 1)
    val classic = TeakUiStyle.isClassic
    // 小电视只属于用户手动下拉：自动加载/后台补全都走骨架或列表自身，不弹指示器。
    var manualRefresh by remember { mutableStateOf(false) }
    LaunchedEffect(snapshot.loading) { if (!snapshot.loading) manualRefresh = false }
    Box(Modifier.fillMaxSize()) {
    HomePullRefreshBox(
        refreshing = manualRefresh && snapshot.loading,
        topPadding = TeakChromeDefaults.TopBarHeight - 8.dp,
        canScrollBackward = { listState.canScrollBackward },
        isAtTop = { listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0 },
        scrollToTop = { listState.animateScrollToItem(0) },
        onRefresh = { manualRefresh = true; onRefresh() },
        modifier = Modifier.fillMaxSize(),
    ) { pullModifier ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .then(pullModifier),
            contentPadding = PaddingValues(
                start = TeakUiStyle.design.contentInset(16.dp),
                end = TeakUiStyle.design.contentInset(16.dp),
                top = 8.dp + rememberStatusBarTop(),
                bottom = 40.dp,
            ),
        ) {
            item(key = "header") {
                TeakLargeTitle(title = title)
            }

            if (pageType == 1) item(key = "shortcuts") { MessageShortcutGroup(snapshot.unread, events) }
            item(key = "menus") {
                MessageSessionTabs(ui)
            }
            if (!loggedIn) {
                item(key = "login") { MessageLoginPrompt(events.onNeedLogin) }
            } else {
                itemsIndexed(visibleSessions, key = { _, session -> session.id }) { _, session ->
                    val anchor = remember { CardBoundsHolder() }
                    Box(Modifier.padding(bottom = 4.dp)
                        .onGloballyPositioned { anchor.attach(it) }
                        .clip(
                            if (classic) androidx.compose.ui.graphics.RectangleShape
                            else com.aritxonly.teak.common.chrome.TeakContinuousCornerShape(18.dp),
                        )
                        .background(if (navigation?.selectedSessionId == session.id) com.aritxonly.bdfun.BrandPink.copy(alpha = .12f)
                            else androidx.compose.ui.graphics.Color.Transparent)) {
                        SessionRow(session, onClick = {
                            when {
                                session.preferences.pageType != null -> openRoute(com.aritxonly.bdfun.core.navigation.Route.MessageInbox(session.preferences.pageType!!, session.name))
                                session.isSystem -> navigation?.feed?.invoke(MessageFeedKind.System, 0) ?: com.aritxonly.bdfun.navigateToMessageFeed(context, "system")
                                session.talkerId > 0L -> openRoute(session.chatRoute())
                                else -> context.showToast("该类型会话暂不支持打开")
                            }
                        }, onLongClick = {
                            if (anchor.value != Rect.Zero && !snapshot.busy && !snapshot.loading) {
                                menu.openSession(session, anchor.value)
                            }
                        })
                    }
                }
                item(key = "footer") {
                    if (!(snapshot.loading && visibleSessions.isEmpty())) {
                        MessagePageFooter(snapshot.loading, snapshot.error, snapshot.inboxCursor != null, visibleSessions.isEmpty(), onLoad = onLoad)
                    }
                }
            }
        }
    }
    // 首屏加载固定在屏幕中间，不受大标题/快捷入口占位影响。
    if (loggedIn && snapshot.loading && visibleSessions.isEmpty()) {
        PageCenteredLoading()
    }
    }
}
