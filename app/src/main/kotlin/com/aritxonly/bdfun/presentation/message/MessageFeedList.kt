package com.aritxonly.bdfun.ui

import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.teak.common.chrome.TeakLargeTitle
import com.aritxonly.teak.common.navigation.TeakPagerTabs
import top.yukonga.miuix.kmp.theme.MiuixTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MessageFeedList(
    kind: MessageFeedKind,
    listState: androidx.compose.foundation.lazy.LazyListState,
    snapshot: com.aritxonly.bdfun.feature.message.application.MessageFeedUiState,
    ui: MessageFeedPresentationStateHolder,
    menu: MessageFeedMenuStateHolder,
    actions: MessageFeedActions,
) {
    val navigate = messageRouteAction()
    val colors = MiuixTheme.colorScheme
    val itemsByTab = snapshot.itemsByTab
    val loadingByTab = snapshot.loadingByTab
    val errorByTab = snapshot.errorByTab
    val nextByTab = snapshot.nextByTab
    val followedOverride = snapshot.followedOverride
    val followBusy = snapshot.followBusy
    val busy = snapshot.busy
    val tabs = messageFeedTabs(kind)
    val title = messageFeedTitle(kind)
    val key = if (ui.tab == 1) 0 else ui.tab
    val allItems = itemsByTab[key].orEmpty()
    val pageItems = if (ui.tab == 1) allItems.filter { it.followed } else allItems
    val classic = com.aritxonly.teak.common.theme.TeakUiStyle.isClassic
    PullToRefreshBox(isRefreshing = loadingByTab[key] == true && pageItems.isNotEmpty(),
        onRefresh = { actions.load(ui.tab) }, modifier = Modifier.fillMaxSize()) {
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = if (classic) 0.dp else 16.dp, end = if (classic) 0.dp else 16.dp,
            top = 8.dp + rememberStatusBarTop(), bottom = 40.dp)) {
        item(key = "header") { TeakLargeTitle(title = title) }
        item(key = "tabs") {
            MessageFeedTabsRow(
                kind = kind, tabs = tabs, selectedTab = ui.tab, classic = classic,
                onSelectTab = { ui.tab = if (it == 1) 2 else 0 },
                onOpenSettings = {
                    navigate(com.aritxonly.bdfun.core.navigation.Route.MessageTools("settings",
                        type = if (kind == MessageFeedKind.Like) 19 else if (ui.tab == 2) 18 else 17))
                },
                onRefresh = { actions.load(ui.tab) },
                refreshEnabled = loadingByTab[key] != true && !busy,
            )
        }
        itemsIndexed(pageItems, key = { _, item -> "message_${item.id}" }) { index, item ->
            val onMenu = { menu.selected = key to item; menu.confirmDelete = false; menu.confirmMute = false }
            val sectionStart = index == 0 || kind == MessageFeedKind.Like &&
                item.presentation.latestLike != pageItems[index - 1].presentation.latestLike
            if (kind == MessageFeedKind.Like && sectionStart) {
                Text(if (item.presentation.latestLike) "最新" else "累计",
                    color = colors.onBackgroundVariant, fontSize = 13.sp,
                    modifier = Modifier.padding(start = 12.dp, top = 14.dp, bottom = 8.dp))
            }
            Box(Modifier.padding(bottom = if (classic) 0.dp else 12.dp)) {
                MessageRowSurface(true, true) {
                    when (kind) {
                        MessageFeedKind.Reply -> ReplyRow(item, onClick = { actions.open(item) }, onMore = onMenu)
                        MessageFeedKind.Like -> LikeRow(item, onClick = { actions.open(item) }, onMore = onMenu)
                        MessageFeedKind.Fans -> FanRow(item, followedOverride[item.userMid] ?: item.followed,
                            item.userMid in followBusy, onFollow = { actions.toggleFollow(item) })
                        MessageFeedKind.System -> SystemNotificationRow(item, onMore = onMenu)
                    }
                }
            }
        }
        item(key = "footer") {
            MessagePageFooter(loadingByTab[key] == true, errorByTab[key], nextByTab[key] != null, pageItems.isEmpty(),
                onLoad = { actions.loadFooter(ui.tab) })
        }
    }
}
}

/** 「回复/赞/粉丝/系统」顶部的页签行；经典样式下要先限宽，右侧设置/刷新才不会被挤掉。 */
@Composable
private fun MessageFeedTabsRow(
    kind: MessageFeedKind, tabs: List<String>, selectedTab: Int, classic: Boolean,
    onSelectTab: (Int) -> Unit, onOpenSettings: () -> Unit, onRefresh: () -> Unit,
    refreshEnabled: Boolean,
) {
    val colors = MiuixTheme.colorScheme
    Row(Modifier.fillMaxWidth().padding(
        top = 16.dp, bottom = 10.dp,
        start = if (classic) 16.dp else 0.dp,
        end = if (classic) 4.dp else 0.dp,
    ), verticalAlignment = Alignment.CenterVertically) {
        if (tabs.isNotEmpty()) {
            val tabsContent: @Composable () -> Unit = {
                TeakPagerTabs(
                    options = tabs, selected = if (selectedTab == 2) 1 else 0,
                    onSelect = onSelectTab,
                    itemWidth = 96.dp, backdrop = null, plain = false,
                )
            }
            if (classic) {
                androidx.compose.foundation.layout.Box(Modifier.weight(1f)) { tabsContent() }
            } else {
                tabsContent()
            }
        }
        if (!classic) Spacer(Modifier.weight(1f))
        if (kind == MessageFeedKind.Reply || kind == MessageFeedKind.Like) {
            Text("设置", color = colors.primary, fontSize = 13.sp,
                modifier = Modifier.clickable(onClick = onOpenSettings).padding(12.dp))
        }
        Text("刷新", color = colors.primary, fontSize = 13.sp,
            modifier = Modifier.clickable(enabled = refreshEnabled, onClick = onRefresh).padding(12.dp))
    }
}