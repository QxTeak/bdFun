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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.AppGraph
import androidx.compose.runtime.snapshotFlow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyListScope
import com.aritxonly.bdfun.core.model.feed.FeedLiveUser
import com.aritxonly.bdfun.core.model.user.UserProfile
import kotlinx.coroutines.CoroutineScope
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
    val haptics = rememberTeakHaptics()
    val authorScroll = rememberLazyListState()
    val profile = AppGraph.session.value
    // 直播 UP 比动态列表晚到：它们插到最前面时 LazyRow 会按原来的锚点停在「全部动态」，
    // 看起来就像直播头像没排到最前。第一次从无到有时主动回到行首。
    var hadLiveUsers by remember { mutableStateOf(false) }
    LaunchedEffect(state.liveUsers.isNotEmpty()) {
        val hasLive = state.liveUsers.isNotEmpty()
        if (hasLive && !hadLiveUsers) authorScroll.scrollToItem(0)
        hadLiveUsers = hasLive
    }
    LaunchedEffect(state) {
        snapshotFlow {
            val info = authorScroll.layoutInfo
            (info.visibleItemsInfo.lastOrNull()?.index ?: -1) to info.totalItemsCount
        }.collect { (last, count) ->
            if (count > 0 && last >= count - 3 && state.authorsError == null) state.loadMoreAuthors()
        }
    }
    val classic = com.aritxonly.teak.common.theme.TeakUiStyle.isClassic
    Column(
        modifier.fillMaxWidth()
            .clip(if (classic) androidx.compose.ui.graphics.RectangleShape else ContinuousCornerShape(18.dp))
            .background(colors.surfaceContainer),
    ) {
        val liveUsers = state.liveUsers
        val liveMids = liveUsers.map { it.mid }.toSet()
        LazyRow(
            state = authorScroll,
            verticalAlignment = Alignment.CenterVertically,
            contentPadding = PaddingValues(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            dynamicAvatarItems(state, liveUsers, liveMids, profile, haptics, scope, context)
        }
    }
}

/** 头像行内容单独拆出，避免主 Composable 超过 80 行的尺寸门槛。 */
private fun LazyListScope.dynamicAvatarItems(
    state: FeedState,
    liveUsers: List<FeedLiveUser>,
    liveMids: Set<Long>,
    profile: UserProfile?,
    haptics: TeakHaptics,
    scope: CoroutineScope,
    context: android.content.Context,
) {
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
            Text(message, color = MiuixTheme.colorScheme.primary, fontSize = 12.sp,
                modifier = Modifier.width(100.dp).clickable {
                    scope.launch { if (state.authorsHasMore) state.loadMoreAuthors() else state.refreshAuthors() }
                }.padding(8.dp))
        }
    }
    if (state.authorsHasMore && !state.authorsLoading && state.authorsError == null) {
        item(key = "up-more") {
            Text("更多 UP", color = MiuixTheme.colorScheme.primary, modifier = Modifier.clickable {
                scope.launch { state.loadMoreAuthors() }
            }.padding(12.dp))
        }
    }
}
