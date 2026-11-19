package com.aritxonly.bdfun.ui

import com.aritxonly.teak.common.theme.TeakUiStyle

import com.aritxonly.bdfun.feature.message.application.visibleSessions
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aritxonly.teak.common.chrome.TeakLargeTitle
import com.aritxonly.teak.common.chrome.TeakPinnedLargeTitleHost

@OptIn(ExperimentalMaterial3Api::class)
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
    val horizontalInset = TeakUiStyle.design.contentInset(16.dp)
    val topInset = 8.dp + rememberStatusBarTop()
    // 大标题画在列表外：下拉（含回弹）只带动会话列表，标题保持底位。
    TeakPinnedLargeTitleHost(
        title = title,
        topInset = topInset,
        horizontalInset = horizontalInset,
        scrollOffsetPx = {
            if (listState.firstVisibleItemIndex > 0) 10_000f
            else listState.firstVisibleItemScrollOffset.toFloat()
        },
    ) {
        PullToRefreshBox(isRefreshing = snapshot.loading && snapshot.sessions.isNotEmpty(), onRefresh = onRefresh, modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize(),
                contentPadding = PaddingValues(
                    start = horizontalInset,
                    end = horizontalInset,
                    top = topInset,
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
                        var anchor by remember { mutableStateOf(Rect.Zero) }
                        Box(Modifier.padding(bottom = 4.dp)
                            .onGloballyPositioned { anchor = it.boundsInWindow() }
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
                                if (anchor != Rect.Zero && !snapshot.busy && !snapshot.loading) {
                                    menu.openSession(session, anchor)
                                }
                            })
                        }
                    }
                    item(key = "footer") {
                        MessagePageFooter(snapshot.loading, snapshot.error, snapshot.inboxCursor != null, visibleSessions.isEmpty(), onLoad = onLoad)
                    }
                }
            }
        }
    }
}
