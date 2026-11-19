package com.aritxonly.bdfun.ui

import com.aritxonly.teak.common.theme.TeakUiStyle

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.ui.*
import com.aritxonly.teak.common.chrome.*
import com.aritxonly.teak.common.navigation.TeakPagerTabs
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.theme.MiuixTheme

import com.aritxonly.bdfun.feature.relations.application.*

@Composable
internal fun RelationFeedList(runtime: RelationScreenRuntime) {
    val context = LocalContext.current
    val colors = MiuixTheme.colorScheme
    val controller = runtime.controller
    val state = runtime.state
    val listState = runtime.listState
    val scope = runtime.scope
    val title = runtime.title
    val fans = runtime.fans
    val order = runtime.order
    val frequent = order.frequent
    val currentMid = runtime.currentMid
    val onOpenSpace = runtime.onOpenSpace
    val classic = TeakUiStyle.isClassic
    val horizontalInset = TeakUiStyle.design.contentInset(12.dp)
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 8.dp
    // 大标题画在列表外：顶部下拉回弹只带动列表内容，标题保持底位。
    TeakPinnedLargeTitleHost(
        title = title,
        topInset = topInset,
        horizontalInset = horizontalInset,
        scrollOffsetPx = {
            if (listState.firstVisibleItemIndex > 0) 10_000f
            else listState.firstVisibleItemScrollOffset.toFloat()
        },
    ) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = horizontalInset, end = horizontalInset,
                top = topInset,
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp),
        ) {
            item("title") { TeakLargeTitle(title = title); Spacer(Modifier.height(16.dp)) }
            if (!fans) item("order") {
                TeakPagerTabs(options = listOf("最近关注", "最常访问"), selected = if (frequent) 1 else 0,
                    onSelect = { order.frequent = it == 1 }, itemWidth = 94.dp,
                    plain = true, backdrop = null,
                    modifier = Modifier.padding(start = if (classic) 12.dp else 0.dp, bottom = 12.dp))
            }
            if (state.total > 0) item("total") {
                Text("共 ${state.total} 人", color = colors.onBackgroundVariant, fontSize = 13.sp,
                    modifier = Modifier.padding(start = 12.dp, bottom = 12.dp))
            }
            itemsIndexed(state.items, key = { _, user -> user.mid }) { index, user ->
                MessageRowSurface(first = index == 0, last = index == state.items.lastIndex) {
                    RelationUserRow(user, user.mid in state.busyUsers, showAction = user.mid != currentMid,
                        onOpen = {
                            onOpenSpace(user.mid)
                        },
                        onFollow = {
                            scope.launch { controller.changeFollow(user)?.let(context::showToast) }
                        })
                }
            }
            item("footer") { RelationFooter(controller, state, fans, scope) }
        }
    }
}
