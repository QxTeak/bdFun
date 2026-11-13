package com.aritxonly.bdfun.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.AppGraph
import androidx.compose.runtime.snapshotFlow
import androidx.compose.foundation.lazy.rememberLazyListState
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun FeedAuthorSidebar(state: FeedState, modifier: Modifier = Modifier) {
    val colors = MiuixTheme.colorScheme
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val authorState = rememberLazyListState()
    LaunchedEffect(state, authorState) {
        snapshotFlow {
            authorState.layoutInfo.visibleItemsInfo.lastOrNull()?.index to authorState.layoutInfo.totalItemsCount
        }.collect { (last, count) ->
            if (count > 0 && last != null && last >= count - 3 && state.authorsError == null) state.loadMoreAuthors()
        }
    }
    LazyColumn(state = authorState, modifier = modifier, contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)) {
        item(key = "all") {
            FeedAuthorRow("全部动态", "", state.filterMid == null, false) {
                scope.launch { state.clearFilter() }
            }
        }
        if (state.liveCount > 0) {
            item(key = "live-toggle") {
                Text("正在直播 · ${state.liveCount}  ${if (state.showLive) "收起" else "展开"}",
                    fontSize = 12.sp, color = colors.onBackgroundVariant,
                    modifier = Modifier.fillMaxWidth().clickable { state.showLive = !state.showLive }.padding(10.dp))
            }
            if (state.showLive) items(state.liveUsers, key = { "live-${it.roomId}" }) { live ->
                FeedAuthorRow(live.name, live.face, false, false, live = true) { openFeedLive(context, live.roomId) }
            }
        }
        AppGraph.session.value?.let { me ->
            item(key = "me") {
                FeedAuthorRow("我", me.face, state.filterMid == me.mid, false) {
                    scope.launch { state.filterTo(me.mid) }
                }
            }
        }
        items(state.authors, key = { "author-${it.mid}" }) { author ->
            FeedAuthorRow(author.name, author.face, state.filterMid == author.mid, author.hasUpdate) {
                scope.launch { state.filterTo(author.mid) }
            }
        }
        if (state.authorsLoading) item { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { TeakLoading(size = 28.dp) } }
        if (state.authorsError != null || state.authorsHasMore) item {
            Text(if (state.authorsError != null) "加载失败，点击重试" else "更多 UP", color = colors.primary, fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth().clickable { scope.launch {
                    if (state.authorsHasMore) state.loadMoreAuthors() else state.refreshAuthors()
                } }.padding(12.dp))
        }
    }
}
