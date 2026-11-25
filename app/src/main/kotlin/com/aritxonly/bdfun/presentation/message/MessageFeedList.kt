package com.aritxonly.bdfun.ui

import com.aritxonly.teak.common.theme.TeakUiStyle

import androidx.compose.foundation.lazy.itemsIndexed
import com.aritxonly.bdfun.core.model.message.MessageItem
import com.aritxonly.bdfun.feature.message.application.MessageFeedUiState
import com.aritxonly.bdfun.presentation.common.pullrefresh.HomePullRefreshBox
import com.aritxonly.teak.common.chrome.isWideFoldTopBar
import top.yukonga.miuix.kmp.basic.TopAppBarDefaults
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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

@Composable
internal fun MessageFeedList(
    kind: MessageFeedKind,
    listState: androidx.compose.foundation.lazy.LazyListState,
    snapshot: MessageFeedUiState,
    ui: MessageFeedPresentationStateHolder,
    menu: MessageFeedMenuStateHolder,
    actions: MessageFeedActions,
) {
    val colors = MiuixTheme.colorScheme
    val itemsByTab = snapshot.itemsByTab
    val loadingByTab = snapshot.loadingByTab
    val errorByTab = snapshot.errorByTab
    val nextByTab = snapshot.nextByTab
    val tabs = messageFeedTabs(kind)
    val key = if (ui.tab == 1) 0 else ui.tab
    val allItems = itemsByTab[key].orEmpty()
    // 过滤只在列表或 tab 变化时重算，避免每次组合都重新分配一份列表。
    val pageItems = remember(allItems, ui.tab) {
        if (ui.tab == 1) allItems.filter { it.followed } else allItems
    }
    val barHeight = if (isWideFoldTopBar()) TopAppBarDefaults.CollapsedHeight else TeakChromeDefaults.TopOverlayHeight
    val topInset = rememberStatusBarTop() + barHeight
    // 小电视只在手动下拉时出现；tab 切换/返回本页的自动加载不弹。
    var manualRefresh by remember { mutableStateOf(false) }
    LaunchedEffect(loadingByTab[key]) { if (loadingByTab[key] != true) manualRefresh = false }
    val refreshing = manualRefresh && loadingByTab[key] == true
    Box(Modifier.fillMaxSize()) {
    HomePullRefreshBox(
        refreshing = refreshing,
        topPadding = barHeight - 8.dp,
        canScrollBackward = { listState.canScrollBackward },
        isAtTop = { listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0 },
        scrollToTop = { listState.animateScrollToItem(0) },
        onRefresh = { if (!snapshot.busy) { manualRefresh = true; actions.load(ui.tab) } },
        modifier = Modifier.fillMaxSize(),
    ) { pullModifier ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().then(pullModifier),
            contentPadding = PaddingValues(
                start = TeakUiStyle.design.contentInset(16.dp),
                end = TeakUiStyle.design.contentInset(16.dp),
                top = topInset,
                bottom = 40.dp,
            ),
        ) {
            if (tabs.isNotEmpty()) item(key = "tabs") {
                // 内容与 tab 之间拉开 5dp，避免紧贴。
                Box(Modifier.padding(bottom = 9.dp)) {
                    TeakPagerTabs(
                        options = tabs, selected = if (ui.tab == 2) 1 else 0,
                        onSelect = { ui.tab = if (it == 1) 2 else 0 },
                        itemWidth = 96.dp, backdrop = null, plain = false,
                    )
                }
            }
            itemsIndexed(pageItems, key = { _, item -> "message_${item.id}" }) { index, item ->
                val sectionStart = index == 0 || kind == MessageFeedKind.Like &&
                    item.presentation.latestLike != pageItems[index - 1].presentation.latestLike
                if (kind == MessageFeedKind.Like && sectionStart && item.presentation.latestLike) {
                    Text("最新",
                        color = colors.onBackgroundVariant, fontSize = 13.sp,
                        modifier = Modifier.padding(start = 12.dp, top = if (index == 0) 2.dp else 10.dp, bottom = 6.dp))
                }
                MessageFeedCard(kind, item, snapshot, actions) { anchor ->
                    if (!snapshot.busy && loadingByTab[key] != true) menu.open(key, item, anchor)
                }
            }
            // 首次加载由整屏居中图标负责，footer 只在已有内容或失败时出现。
            if (!(loadingByTab[key] == true && pageItems.isEmpty())) {
                item(key = if (pageItems.isEmpty()) "initial-footer" else "footer") {
                    MessagePageFooter(
                        loading = loadingByTab[key] == true && pageItems.isNotEmpty(),
                        error = errorByTab[key],
                        hasMore = nextByTab[key] != null,
                        empty = pageItems.isEmpty(),
                        onLoad = { actions.loadFooter(ui.tab) },
                    )
                }
            }
        }
    }
    if (loadingByTab[key] == true && pageItems.isEmpty() && errorByTab[key] == null) {
        PageCenteredLoading()
    }
    }
}


@Composable
private fun MessageFeedCard(
    kind: MessageFeedKind,
    item: MessageItem,
    snapshot: MessageFeedUiState,
    actions: MessageFeedActions,
    openMenu: (Rect) -> Unit,
) {
    val anchor = remember { CardBoundsHolder() }
    val onMenu = { if (anchor.value != Rect.Zero) openMenu(anchor.value) }
    Box(Modifier.padding(bottom = TeakUiStyle.design.contentInset(8.dp))
        .onGloballyPositioned { anchor.value = it.boundsInWindow() }) {
        MessageRowSurface(true, true) {
            when (kind) {
                MessageFeedKind.Reply -> ReplyRow(item, onClick = { actions.open(item) }, onMore = onMenu)
                MessageFeedKind.Like -> LikeRow(item, onClick = { actions.open(item) }, onMore = onMenu)
                MessageFeedKind.Fans -> FanRow(
                    item, snapshot.followedOverride[item.userMid] ?: item.followed,
                    item.userMid in snapshot.followBusy, onFollow = { actions.toggleFollow(item) },
                )
                MessageFeedKind.System -> SystemNotificationRow(item, onMore = onMenu)
            }
        }
    }
}
