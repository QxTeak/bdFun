package com.aritxonly.bdfun.ui

import com.aritxonly.teak.common.theme.TeakUiStyle

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.AppGraph
import com.aritxonly.bdfun.BuildConfig
import com.aritxonly.bdfun.data.LogCollector
import com.aritxonly.bdfun.core.model.feed.FeedLiveUser
import com.aritxonly.bdfun.core.model.user.UserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun FeedAvatarCard(
    state: FeedState,
    ring: FeedAvatarRingState,
    modifier: Modifier = Modifier,
) {
    val colors = MiuixTheme.colorScheme
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val haptics = rememberTeakHaptics()
    val authorScroll = rememberLazyListState()
    val profile = AppGraph.session.value
    // 没有筛选任何 UP 时，环停在「全部动态」；筛选中则跟着当前 UP 走。
    val selectedKey: Any = state.filterMid ?: FeedAllAvatarKey
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
    // 切换 UP：先把目标滚到「完整露出」（已经在视口里就不动），再放环的动画。
    // 不能边滚边绕：目标半露时环会被卡片裁掉一截，看起来像缩小。
    val gapPx = with(androidx.compose.ui.platform.LocalDensity.current) { 8.dp.toPx() }
    LaunchedEffect(selectedKey) {
        // 滚动会改变锚点，先把环现在的落点冻结下来，滚完仍从原来的位置出发。
        val source = ring.currentRect()
        // 源头像此刻还有没有实时测量点：没有就说明它本来就在屏幕外，
        // 滚动方向不能反过来覆盖已经记住的那一侧。
        val sourceLive = ring.hasLiveAnchor()
        val scrollBefore = authorScroll.positionToken()
        val scrolled = if (state.filterMid == null) {
            authorScroll.animateScrollToItem(0)
            true
        } else {
            val index = feedAvatarIndex(state, profile?.mid, selectedKey)
            index >= 0 && authorScroll.ensureFullyVisible(index, gapPx)
        }
        // 只有真的滚了才等一帧；目标本来就完整露出时直接开动画，少 1 帧延迟。
        if (scrolled) withFrameNanos { }
        val scrollDelta = if (sourceLive) authorScroll.scrollDirection(scrollBefore) else 0
        ring.moveTo(selectedKey, source, scrollDelta)
    }

    val liveUsers = state.liveUsers
    // 头像行每次重组都会重建这些过滤结果；作者列表/直播列表没变时复用。
    val liveMids = remember(liveUsers) { liveUsers.map { it.mid }.toSet() }
    val authors = remember(state.authors, liveMids) {
        state.authors.filterNot { it.mid in liveMids }
    }

    Box(
        modifier.fillMaxWidth()
            .clip(TeakUiStyle.design.cardShape(ContinuousCornerShape(18.dp)))
            .background(colors.surfaceContainer),
    ) {
        LazyRow(
            state = authorScroll,
            verticalAlignment = Alignment.CenterVertically,
            contentPadding = PaddingValues(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            dynamicAvatarItems(
                state, liveUsers, authors, liveMids, profile, haptics, scope, context, ring,
            )
        }
        FeedAvatarRingOverlay(
            state = ring,
            color = colors.primary,
            modifier = Modifier.matchParentSize(),
        )
    }
}

/** 头像行内容单独拆出，避免主 Composable 超过 80 行的尺寸门槛。 */
private fun LazyListScope.dynamicAvatarItems(
    state: FeedState,
    liveUsers: List<FeedLiveUser>,
    authors: List<com.aritxonly.bdfun.core.model.feed.FeedAuthor>,
    liveMids: Set<Long>,
    profile: UserProfile?,
    haptics: TeakHaptics,
    scope: CoroutineScope,
    context: android.content.Context,
    ring: FeedAvatarRingState,
) {
    items(liveUsers, key = { "live-${it.mid}" }) { user ->
        LiveAvatar(
            name = user.name,
            face = user.face,
            avatarModifier = rememberAvatarAnchor(ring, user.mid),
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
        AllFeedAvatar(avatarModifier = rememberAvatarAnchor(ring, FeedAllAvatarKey)) {
            haptics.tap()
            scope.launch { state.listState.scrollToItem(0); state.clearFilter() }
        }
    }
    profile?.takeIf { it.mid !in liveMids }?.let { me ->
        item(key = "my-dynamics") {
            AuthorAvatar(
                name = "我",
                face = me.face,
                avatarModifier = rememberAvatarAnchor(ring, me.mid),
            ) {
                haptics.tap()
                scope.launch { state.listState.scrollToItem(0); state.filterTo(me.mid) }
            }
        }
    }
    items(authors, key = { "up-${it.mid}" }) { author ->
        AuthorAvatar(
            name = author.name,
            face = author.face,
            hasUpdate = author.hasUpdate,
            avatarModifier = rememberAvatarAnchor(ring, author.mid),
        ) {
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

/**
 * 给头像圆登记测量点。LazyRow 会回收滚出视口的项，离开组合时必须清掉锚点，
 * 否则环会停在一个已经不存在的位置。
 */
@Composable
private fun rememberAvatarAnchor(ring: FeedAvatarRingState, key: Any): Modifier {
    DisposableEffect(ring, key) { onDispose { ring.clearAnchor(key) } }
    // 复用同一个 Modifier：列表分页/作者刷新导致重组时，不必重新下发布局节点配置。
    return remember(ring, key) { Modifier.feedAvatarAnchor(ring, key) }
}

/** 把 [index] 滚到"完整露出"：已经在视口里就不动，否则居中；两端夹住时只补差额。 */
private suspend fun LazyListState.ensureFullyVisible(index: Int, gapPx: Float): Boolean {
    val info = layoutInfo
    val item = info.visibleItemsInfo.firstOrNull { it.index == index }
    val fullyVisible = item != null &&
        item.offset >= info.viewportStartOffset &&
        item.offset + item.size <= info.viewportEndOffset
    if (BuildConfig.DEBUG) {
        LogCollector.record(
            "FEED_RING",
            "ensureVisible index=$index item=${item?.let { "${it.offset}+${it.size}" } ?: "null"} " +
                "viewport=${info.viewportStartOffset}..${info.viewportEndOffset} fully=$fullyVisible",
        )
    }
    if (fullyVisible) return false
    val visibleCount = info.visibleItemsInfo.size.coerceAtLeast(1)
    val first = info.visibleItemsInfo.firstOrNull()?.index ?: 0
    val centered = (index - visibleCount / 2).coerceAtLeast(0)
    if (item == null || centered != first) {
        animateScrollToItem(centered)
        return true
    }
    // 已经贴着起始位置但只露了一半：只补差的这点，避免整页跳。
    if (item.offset < info.viewportStartOffset) {
        animateScrollBy(item.offset - info.viewportStartOffset - gapPx)
    } else if (item.offset + item.size > info.viewportEndOffset) {
        animateScrollBy(item.offset + item.size - info.viewportEndOffset + gapPx)
    } else {
        return false
    }
    return true
}

/** 滚动位置快照：只用于比较切换前后头像栏往哪边滚了。 */
private fun LazyListState.positionToken(): Pair<Int, Int> =
    firstVisibleItemIndex to firstVisibleItemScrollOffset

/** 正数=内容左移（列表往后滚），负数=内容右移；没动返回 0。 */
private fun LazyListState.scrollDirection(before: Pair<Int, Int>): Int {
    val after = positionToken()
    return when {
        after.first > before.first -> 1
        after.first < before.first -> -1
        after.second > before.second -> 1
        after.second < before.second -> -1
        else -> 0
    }
}
