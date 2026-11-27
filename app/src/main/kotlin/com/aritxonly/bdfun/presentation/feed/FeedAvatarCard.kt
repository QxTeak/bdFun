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
import androidx.compose.ui.geometry.Rect
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
    /** UP 之间切换：由页面统一跑「旧页滑出、新页滑入」，不要在卡片里直接 filterTo。 */
    onSelectUp: (Long) -> Unit,
    /** 点「全部动态」退出 UP 模式：页面自己决定淡出，卡片不直接 clearFilter。 */
    onExitUp: () -> Unit,
    /**
     * 环跟随的选中项。UP 模式下它比 [FeedState.filterMid] 更早变化：
     * pager 一开始翻页环就走，数据等停稳再换。
     */
    selectedMid: Long? = state.filterMid,
) {
    val colors = MiuixTheme.colorScheme
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val haptics = rememberTeakHaptics()
    val authorScroll = rememberLazyListState()
    val profile = AppGraph.session.value
    // 头像行此刻往哪边滚：头像被回收时用它兜底判断是从左边还是右边滑出去的。
    val rowScroll = rememberAvatarRowScroll(authorScroll)
    // 没有筛选任何 UP 时，环停在「全部动态」；筛选中则跟着当前 UP 走。
    val selectedKey: Any = selectedMid ?: FeedAllAvatarKey
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
    FeedAvatarRingEffect(state, profile, ring, authorScroll, selectedKey)

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
                rowScroll, onSelectUp, onExitUp,
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
    rowScroll: AvatarRowScrollState,
    onSelectUp: (Long) -> Unit,
    onExitUp: () -> Unit,
) {
    items(liveUsers, key = { "live-${it.mid}" }) { user ->
        LiveAvatar(
            name = user.name,
            face = user.face,
            avatarModifier = rememberAvatarAnchor(ring, user.mid, rowScroll),
            onClick = {
                haptics.tap()
                onSelectUp(user.mid)
            },
            onLongClick = {
                haptics.longPress()
                openFeedLive(context, user.roomId)
            },
        )
    }
    item(key = "all-dynamics") {
        AllFeedAvatar(avatarModifier = rememberAvatarAnchor(ring, FeedAllAvatarKey, rowScroll)) {
            haptics.tap()
            onExitUp()
        }
    }
    profile?.takeIf { it.mid !in liveMids }?.let { me ->
        item(key = "my-dynamics") {
            AuthorAvatar(
                name = "我",
                face = me.face,
                avatarModifier = rememberAvatarAnchor(ring, me.mid, rowScroll),
            ) {
                haptics.tap()
                onSelectUp(me.mid)
            }
        }
    }
    items(authors, key = { "up-${it.mid}" }) { author ->
        AuthorAvatar(
            name = author.name,
            face = author.face,
            hasUpdate = author.hasUpdate,
            avatarModifier = rememberAvatarAnchor(ring, author.mid, rowScroll),
        ) {
            haptics.tap()
            onSelectUp(author.mid)
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
 * 否则环会停在一个已经不存在的位置。回收时把头像行此刻的滚动方向一起带上，
 * 让环能区分「横向滑出」和「整张卡片纵向滚走」。
 */
@Composable
private fun rememberAvatarAnchor(
    ring: FeedAvatarRingState,
    key: Any,
    rowScroll: AvatarRowScrollState,
): Modifier {
    DisposableEffect(ring, key) {
        onDispose { ring.clearAnchor(key, rowScroll.direction) }
    }
    // 复用同一个 Modifier：列表分页/作者刷新导致重组时，不必重新下发布局节点配置。
    return remember(ring, key) { Modifier.feedAvatarAnchor(ring, key) }
}

/** 头像行的滚动方向：+1 内容左移（往后滚），-1 内容右移，0 没在滚。 */
private class AvatarRowScrollState {
    var direction = 0
}

private data class AvatarRowPosition(val index: Int, val offset: Int, val scrolling: Boolean)

/** 跟踪头像行往哪边滚。只在其实滚动时记录，滚停后清零，避免拿很久以前的方向乱猜。 */
@Composable
private fun rememberAvatarRowScroll(authorScroll: LazyListState): AvatarRowScrollState {
    val rowScroll = remember { AvatarRowScrollState() }
    LaunchedEffect(authorScroll) {
        var last = authorScroll.firstVisibleItemIndex to authorScroll.firstVisibleItemScrollOffset
        snapshotFlow {
            AvatarRowPosition(
                index = authorScroll.firstVisibleItemIndex,
                offset = authorScroll.firstVisibleItemScrollOffset,
                scrolling = authorScroll.isScrollInProgress,
            )
        }.collect { position ->
            if (!position.scrolling) {
                rowScroll.direction = 0
            } else {
                rowScroll.direction = when {
                    position.index > last.first -> 1
                    position.index < last.first -> -1
                    position.offset > last.second -> 1
                    position.offset < last.second -> -1
                    else -> rowScroll.direction
                }
            }
            last = position.index to position.offset
        }
    }
    return rowScroll
}

/**
 * 切换 UP 时把目标头像滚进视口，再让环从源头像出发。
 * 单独拆出来，主 Composable 才能守住 80 行的尺寸门槛。
 */
@Composable
private fun FeedAvatarRingEffect(
    state: FeedState,
    profile: UserProfile?,
    ring: FeedAvatarRingState,
    authorScroll: LazyListState,
    selectedKey: Any,
) {
    val gapPx = with(androidx.compose.ui.platform.LocalDensity.current) { 8.dp.toPx() }
    LaunchedEffect(selectedKey) {
        // 滚动会改变锚点，先把环现在的落点冻结下来，作为锚点被回收时的兜底。
        val source = ring.currentRect()
        // 源头像此刻还有没有实时测量点：没有就说明它本来就在屏幕外，
        // 滚动方向不能反过来覆盖已经记住的那一侧。
        val sourceLive = ring.hasLiveAnchor()
        val index = feedAvatarIndex(state, profile?.mid, selectedKey)
        // 目标可能还在屏幕外：先用行内间距估一个位置，环就不用等头像行滚完。
        val estimate = if (index >= 0) authorScroll.estimateAvatarRect(index, ring, gapPx) else null
        val sourceIndex = feedAvatarIndex(state, profile?.mid, ring.selectedKey)
        // 目标在源头像右边 = 头像行往后滚（内容左移），源头像从左边出；反之亦然。
        val expectedDelta = if (sourceLive && sourceIndex >= 0 && index >= 0) {
            when {
                index > sourceIndex -> 1
                index < sourceIndex -> -1
                else -> 0
            }
        } else {
            0
        }
        // 头像行滚动和环同时开始：不再"滚完再绕"。
        kotlinx.coroutines.coroutineScope {
            val scroll = launch {
                if (state.filterMid == null) {
                    authorScroll.animateScrollToItem(0)
                } else if (index >= 0) {
                    authorScroll.ensureFullyVisible(index, gapPx)
                }
            }
            ring.moveTo(selectedKey, source, expectedDelta, estimate)
            scroll.join()
        }
    }
}

/**
 * 目标头像还在屏幕外时的估算位置：头像行定宽（56dp + 12dp 间距），
 * 用可见项的平均步距外推即可；纵向/尺寸沿用任意一个已测量头像。
 */
private fun LazyListState.estimateAvatarRect(
    index: Int,
    ring: FeedAvatarRingState,
    gapPx: Float,
): Rect? {
    val info = layoutInfo
    val visible = info.visibleItemsInfo
    if (index < 0 || visible.isEmpty()) return null
    val reference = ring.referenceAvatarRect() ?: return null
    val first = visible.first()
    val last = visible.last()
    val pitch = if (last.index > first.index) {
        (last.offset - first.offset).toFloat() / (last.index - first.index)
    } else {
        first.size + gapPx
    }
    val itemX = -info.viewportStartOffset + first.offset + pitch * (index - first.index)
    val avatarX = itemX + (first.size - reference.width) / 2f
    return Rect(avatarX, reference.top, avatarX + reference.width, reference.bottom)
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
