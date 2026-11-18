package com.aritxonly.bdfun.ui

import com.aritxonly.teak.common.theme.TeakUiStyle

import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.teak.common.chrome.TeakChromeDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
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
    val colors = MiuixTheme.colorScheme
    val itemsByTab = snapshot.itemsByTab
    val loadingByTab = snapshot.loadingByTab
    val errorByTab = snapshot.errorByTab
    val nextByTab = snapshot.nextByTab
    val followedOverride = snapshot.followedOverride
    val followBusy = snapshot.followBusy
    val tabs = messageFeedTabs(kind)
    val key = if (ui.tab == 1) 0 else ui.tab
    val allItems = itemsByTab[key].orEmpty()
    val pageItems = if (ui.tab == 1) allItems.filter { it.followed } else allItems
    PullToRefreshBox(isRefreshing = loadingByTab[key] == true && pageItems.isNotEmpty(),
        onRefresh = { if (!snapshot.busy) actions.load(ui.tab) },
        modifier = Modifier.fillMaxSize().padding(top = TeakChromeDefaults.TopOverlayHeight + rememberStatusBarTop()),
    ) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = TeakUiStyle.design.contentInset(16.dp), end = TeakUiStyle.design.contentInset(16.dp),
                bottom = 40.dp)) {
            if (tabs.isNotEmpty()) item(key = "tabs") {
                Box(Modifier.padding(bottom = 4.dp)) {
                    TeakPagerTabs(
                        options = tabs, selected = if (ui.tab == 2) 1 else 0,
                        onSelect = { ui.tab = if (it == 1) 2 else 0 },
                        itemWidth = 96.dp, backdrop = null, plain = false,
                    )
                }
            }
            itemsIndexed(pageItems, key = { _, item -> "message_${item.id}" }) { index, item ->
                var anchor by remember { mutableStateOf(Rect.Zero) }
                val onMenu = { if (!snapshot.busy && loadingByTab[key] != true && anchor != Rect.Zero) {
                    menu.open(key, item, anchor)
                } }
                val sectionStart = index == 0 || kind == MessageFeedKind.Like &&
                    item.presentation.latestLike != pageItems[index - 1].presentation.latestLike
                if (kind == MessageFeedKind.Like && sectionStart) {
                    Text(if (item.presentation.latestLike) "最新" else "累计",
                        color = colors.onBackgroundVariant, fontSize = 13.sp,
                        modifier = Modifier.padding(start = 12.dp, top = if (index == 0) 2.dp else 10.dp, bottom = 6.dp))
                }
                Box(Modifier.padding(bottom = TeakUiStyle.design.contentInset(8.dp))
                    .onGloballyPositioned { anchor = it.boundsInWindow() }) {
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
