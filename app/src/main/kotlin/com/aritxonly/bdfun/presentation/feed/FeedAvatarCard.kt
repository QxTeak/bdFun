package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.AppGraph
import androidx.compose.runtime.snapshotFlow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyListScope
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun FeedAvatarCard(
    state: FeedState,
    modifier: Modifier = Modifier,
) {
    val colors = MiuixTheme.colorScheme
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val haptics = com.aritxonly.bdfun.ui.rememberTeakHaptics()
    val authorScroll = rememberLazyListState()
    val profile = AppGraph.session.value
    LaunchedEffect(state) {
        snapshotFlow {
            val info = authorScroll.layoutInfo
            (info.visibleItemsInfo.lastOrNull()?.index ?: -1) to info.totalItemsCount
        }.collect { (last, count) ->
            if (count > 0 && last >= count - 3 && state.authorsError == null) state.loadMoreAuthors()
        }
    }
    Column(modifier.fillMaxWidth().clip(ContinuousCornerShape(18.dp)).background(colors.surfaceContainer)) {
        // 正在直播的 UP 提到最前面：点击照常筛选他的动态，长按直接进直播间。
        val liveUsers = state.liveUsers
        val liveMids = liveUsers.map { it.mid }.toSet()
        val entries: LazyListScope.() -> Unit = {
            items(liveUsers, key = { "live-${it.mid}" }) { user ->
                LiveAvatar(
                    name = user.name,
                    face = user.face,
                    onClick = {
                        haptics.tap()
                        scope.launch { state.listState.scrollToItem(0); state.filterTo(user.mid) }
                    },
                    onLongClick = {
                        haptics.longPress()
                        openFeedLive(context, user.roomId)
                    },
                )
            }
            item(key = "all-dynamics") {
                AllFeedAvatar(selected = state.type == "all") {
                    haptics.tap()
                    scope.launch { state.listState.scrollToItem(0); state.clearFilter() }
                }
            }
            profile?.takeIf { it.mid !in liveMids }?.let { me ->
                item(key = "my-dynamics") {
                    AuthorAvatar(name = "我", face = me.face, selected = state.filterMid == me.mid) {
                        haptics.tap()
                        scope.launch { state.listState.scrollToItem(0); state.filterTo(me.mid) }
                    }
                }
            }
            items(state.authors.filterNot { it.mid in liveMids }, key = { "up-${it.mid}" }) { author ->
                AuthorAvatar(name = author.name, face = author.face,
                    selected = state.filterMid == author.mid, hasUpdate = author.hasUpdate) {
                    haptics.tap()
                    scope.launch { state.listState.scrollToItem(0); state.filterTo(author.mid) }
                }
            }
            if (state.authorsLoading) item(key = "up-loading") { TeakLoading(size = 28.dp) }
            state.authorsError?.let { message ->
                item(key = "up-error") {
                    Text(message, color = colors.primary, fontSize = 12.sp,
                        modifier = Modifier.width(100.dp).clickable {
                            scope.launch { if (state.authorsHasMore) state.loadMoreAuthors() else state.refreshAuthors() }
                        }.padding(8.dp))
                }
            }
            if (state.authorsHasMore && !state.authorsLoading && state.authorsError == null) {
                item(key = "up-more") {
                    Text("更多 UP", color = colors.primary, modifier = Modifier.clickable {
                        scope.launch { state.loadMoreAuthors() }
                    }.padding(12.dp))
                }
            }
        }
        LazyRow(state = authorScroll, verticalAlignment = Alignment.CenterVertically,
            contentPadding = PaddingValues(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), content = entries)
    }
}
