package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import com.aritxonly.teak.common.chrome.TeakGlass

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aritxonly.bdfun.BrandPink
import com.aritxonly.bdfun.TeakNavigator
import com.aritxonly.bdfun.data.VideoItem
import com.kyant.shapes.Capsule
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 卡片圆角统一走这里（图二风格：大圆角 squircle）。 */
internal val FeedCardShape = ContinuousCornerShape(16.dp)

/** 下拉超过这个距离、并且**松手**之后才触发刷新。 */
private val RefreshThreshold = 88.dp

/** 下拉位移上限，避免拉飞。 */
private val MaxPull = 260.dp

/** 标题固定两行的高度，保证单行/双行卡片的作者行对齐。 */
private val TitleBlockHeight = 36.dp

@Composable
fun HomeScreen(
    state: HomeFeedState,
    onOpenVideo: (VideoItem) -> Unit,
    /** 0 = 双排小卡，1 = 大卡（动态页视频卡片同款）。 */
    cardStyle: Int = 0,
) {
    val largeCards = cardStyle == 1
    val colors = MiuixTheme.colorScheme
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val context = LocalContext.current
    val thresholdPx = with(density) { RefreshThreshold.toPx() }
    val maxPullPx = with(density) { MaxPull.toPx() }
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    // 下拉/刷新时的内容位移（px）。这里刻意不用 Animatable：
    // 1) 每次滚动事件都 launch{snapTo} 会排队竞态；2) spring 回弹会冲过 0 点再弹回来，
    //    表现就是「只拉一点点松手后卡片抽动一下」。改成单调缓动手动补间，回弹只进不退。
    var pullOffset by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(state) { state.ensureLoaded() }

    fun applyPullRaw(px: Float) {
        val next = px.coerceIn(0f, maxPullPx)
        pullOffset = next
        state.pullOffsetDp = with(density) { next.toDp().value }
        state.pullFraction = (next / thresholdPx).coerceIn(0f, 1f)
    }

    suspend fun animatePullTo(
        target: Float,
        durationMs: Float = 220f,
        easing: androidx.compose.animation.core.Easing =
            androidx.compose.animation.core.FastOutSlowInEasing,
    ) {
        val from = pullOffset
        val to = target.coerceIn(0f, maxPullPx)
        if (from == to) return
        if (durationMs <= 0f) {
            applyPullRaw(to)
            return
        }
        val startNanos = System.nanoTime()
        while (true) {
            val p = ((System.nanoTime() - startNanos) / 1_000_000f / durationMs)
                .coerceIn(0f, 1f)
            applyPullRaw(from + (to - from) * easing.transform(p))
            if (p >= 1f) break
            kotlinx.coroutines.delay(8)
        }
        applyPullRaw(to)
    }

    // 刷新结束 → 先把小电视收掉，再让内容平滑回顶（避免回弹时撞上小电视）
    LaunchedEffect(state.refreshing) {
        if (!state.refreshing && pullOffset > 0f) {
            state.pullFraction = 0f
            kotlinx.coroutines.delay(160)
            animatePullTo(0f, 260f)
        }
    }

    // 外部请求刷新（再次点击底栏当前页）。
    // 用"已处理到第几号请求"做幂等：页面被销毁重建（如从视频页返回）时不会重放刷新。
    var handledRequest by remember { mutableIntStateOf(state.refreshRequest) }
    LaunchedEffect(state.refreshRequest) {
        if (state.refreshRequest == handledRequest) return@LaunchedEffect
        handledRequest = state.refreshRequest
        if (state.refreshing || state.loading) return@LaunchedEffect
        val atTop = state.gridState.firstVisibleItemIndex == 0 &&
            state.gridState.firstVisibleItemScrollOffset == 0
        if (atTop) {
            animatePullTo(thresholdPx, 200f)
        } else {
            // 回顶与下拉并行，衔接处不停顿
            kotlinx.coroutines.coroutineScope {
                launch { state.gridState.animateScrollToItem(0) }
                launch { animatePullTo(thresholdPx, 260f) }
            }
        }
        state.pullFraction = 1f
        state.refresh()
    }

    // 滚到接近底部就预取下一页
    LaunchedEffect(state) {
        snapshotFlow { state.gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            // 进入页面/从视频页返回时首次布局会重发一次当前索引，不能把它当成用户滑到底。
            .drop(1)
            .collect { last ->
                if (state.videos.isNotEmpty() && last >= state.videos.size - 2) state.loadMore()
            }
    }

    fun applyPull(value: Float) {
        applyPullRaw(value)
    }

    val refreshConnection = remember(state) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput) return Offset.Zero
                if (available.y < 0f && pullOffset > 0f) {
                    val next = (pullOffset + available.y).coerceAtLeast(0f)
                    val consumed = next - pullOffset
                    applyPull(next)
                    return Offset(0f, consumed)
                }
                if (available.y > 0f && !state.gridState.canScrollBackward) {
                    val next = (pullOffset + available.y * 0.6f).coerceAtMost(maxPullPx)
                    val consumed = next - pullOffset
                    applyPull(next)
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                val shouldRefresh = pullOffset >= thresholdPx &&
                    !state.refreshing && !state.loading
                if (shouldRefresh) {
                    animatePullTo(thresholdPx, 140f)
                    state.pullFraction = 1f
                    state.refresh()
                    // 回落交给 LaunchedEffect(state.refreshing)，这里保持阈值位置。
                } else {
                    animatePullTo(0f, 220f)
                    state.pullFraction = 0f
                }
                return Velocity.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity = Velocity.Zero
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = if (maxWidth >= 600.dp) GridCells.Adaptive(if (largeCards) 320.dp else 240.dp) else GridCells.Fixed(if (largeCards) 1 else 2),
            state = state.gridState,
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(0, pullOffset.roundToInt()) }
                .nestedScroll(refreshConnection),
            contentPadding = PaddingValues(
                start = 12.dp,
                top = HomeTopBarHeight + statusBarTop + 6.dp,
                end = 12.dp,
                // 让最后一排卡片能滚出悬浮底栏
                bottom = 104.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items = state.videos, key = { it.bvid }) { video ->
                val oneTake = rememberOneTakeAnchor()
                val radiusPx = with(density) { (if (largeCards) 14.dp else 16.dp).toPx() }
                val bgArgb = colors.background.toArgb()
                val openCard = {
                    oneTake.open(
                        context = context,
                        video = video,
                        radiusPx = radiusPx,
                        backgroundColor = bgArgb,
                    ) { onOpenVideo(video) }
                }
                if (largeCards) {
                    VideoLargeCard(video = video, modifier = oneTake.modifier, onClick = openCard)
                } else {
                    VideoGridCard(video = video, modifier = oneTake.modifier, onClick = openCard)
                }
            }

            if (state.videos.isNotEmpty()) {
                item(key = "footer", span = { GridItemSpan(maxLineSpan) }) {
                    val footerError = state.loadMoreError
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp)
                            .then(
                                if (footerError != null) {
                                    Modifier.clickable { scope.launch { state.loadMore() } }
                                } else {
                                    Modifier
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = when {
                                state.loadingMore -> "加载中…"
                                footerError != null -> "加载失败，点此重试"
                                state.endReached -> "没有更多了"
                                else -> "上滑加载更多"
                            },
                            color = colors.onBackgroundVariant,
                            fontSize = 12.sp,
                        )
                    }
                }
            }
        }

        if (state.loading && state.videos.isEmpty()) {
            Box(modifier = Modifier.align(Alignment.Center)) {
                TeakLoading(color = colors.primary, size = 44.dp)
            }
        }

        state.error?.let { message ->
            if (state.videos.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(text = message, color = colors.onBackgroundVariant, fontSize = 14.sp)
                    Spacer(Modifier.height(10.dp))
                    TeakGlass(
                        modifier = Modifier.height(34.dp),
                        shape = Capsule(),
                        onClick = { scope.launch { state.refresh() } },
                    ) {
                        Text(
                            text = "重试",
                            color = colors.primary,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 18.dp).align(Alignment.Center),
                        )
                    }
                }
            }
        }
    }
}

/**
 * 两列信息流卡片：圆角矩形背景，上半部分与封面完全重合，下半部分包住标题与作者行。
 * 封面底部一条信息带：左边播放 / 弹幕（小图标 + 数字），右边时长。
 */
@Composable
internal fun VideoGridCard(
    video: VideoItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** 已看进度 0..1（历史记录用），默认不显示 */
    progress: Float = 0f,
    /** 批量管理：显示右上角勾选圈，点击仍交给 onClick 处理 */
    selectionEnabled: Boolean = false,
    selected: Boolean = false,
    /** 历史页：统计行先占位，避免补数据时整行突然出现 */
    reserveMeta: Boolean = false,
) {
    val colors = MiuixTheme.colorScheme
    val context = LocalContext.current

    Column(
        modifier = modifier
            .clip(FeedCardShape)
            .background(colors.surface)
            .clickable(interactionSource = null, indication = null, onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(colors.surfaceContainerHigh),
        ) {
            AsyncImage(
                model = rememberImageRequest(video.cover),
                contentDescription = video.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(42.dp)
                    .background(
                        Brush.verticalGradient(listOf(Color.Transparent, Color(0x4D000000))),
                    ),
            )

            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(start = 6.dp, end = 6.dp, bottom = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 历史记录等没有播放/弹幕数据的来源就不显示这两个 0
                if (reserveMeta || video.play > 0 || video.danmaku > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        MetricGlyph(play = true)
                        Spacer(Modifier.width(3.dp))
                        Text(
                            text = if (reserveMeta && video.play <= 0) "—" else formatCount(video.play),
                            color = Color.White,
                            fontSize = 10.sp,
                            lineHeight = 11.sp,
                            fontWeight = FontWeight.Normal,
                        )
                        Spacer(Modifier.width(6.dp))
                        MetricGlyph(play = false)
                        Spacer(Modifier.width(3.dp))
                        Text(
                            text = if (reserveMeta && video.danmaku <= 0) "—" else formatCount(video.danmaku),
                            color = Color.White,
                            fontSize = 10.sp,
                            lineHeight = 11.sp,
                            fontWeight = FontWeight.Normal,
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                if (video.duration > 0) {
                    Text(
                        text = formatDuration(video.duration),
                        color = Color.White,
                        fontSize = 10.sp,
                        lineHeight = 11.sp,
                    )
                }
            }

            // 历史记录：封面底部一条已看进度
            if (progress > 0f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .height(2.dp)
                        .background(MiuixTheme.colorScheme.primary),
                )
            }

            // 批量管理：右上角勾选圈
            if (selectionEnabled) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(
                            if (selected) MiuixTheme.colorScheme.primary else Color(0x66000000),
                        )
                        .border(1.5.dp, Color.White.copy(alpha = 0.9f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (selected) {
                        Text(
                            text = "✓",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }

        Column(modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 7.dp, bottom = 8.dp)) {
            Text(
                text = video.title,
                color = colors.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    fontSize = 13.5.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Normal,
                ),
                modifier = Modifier.heightIn(min = TitleBlockHeight),
            )

            Spacer(Modifier.height(5.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // UP 名称可点：进 UP 空间页（点击被这里消费，不会触发整卡进视频）
                Row(
                    modifier = Modifier
                        .clip(Capsule())
                        .clickable(interactionSource = null, indication = null) {
                            TeakNavigator.openSpace(
                                context,
                                video.ownerMid,
                            )
                        }
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    UpBadge()
                    if (video.isFollowed) {
                        Spacer(Modifier.width(4.dp))
                        FollowedBadge()
                    }
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = video.ownerName,
                        color = colors.onBackgroundVariant,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/**
 * 首页大卡：和动态页视频卡片同一套排版 —— 整行 16:9 封面，底部一条渐变信息带，
 * 标题和作者放在封面下方。双排模式完全不会用到它。
 */
@Composable
internal fun VideoLargeCard(
    video: VideoItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    progress: Float = 0f,
) {
    val colors = MiuixTheme.colorScheme
    val context = LocalContext.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(ContinuousCornerShape(14.dp))
            .background(colors.surface)
            .clickable(interactionSource = null, indication = null, onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(colors.surfaceContainerHigh),
        ) {
            AsyncImage(
                model = rememberImageRequest(video.cover),
                contentDescription = video.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(
                        Brush.verticalGradient(listOf(Color.Transparent, Color(0x66000000))),
                    ),
            )
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 10.dp, bottom = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (video.duration > 0) {
                    Text(
                        text = formatDuration(video.duration),
                        color = Color.White,
                        fontSize = 11.sp,
                        lineHeight = 12.sp,
                    )
                    Spacer(Modifier.width(9.dp))
                }
                if (video.play > 0 || video.danmaku > 0) {
                    MetricGlyph(play = true, size = 13.dp)
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = formatCount(video.play),
                        color = Color.White,
                        fontSize = 10.sp,
                    )
                    Spacer(Modifier.width(9.dp))
                    MetricGlyph(play = false, size = 13.dp)
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = formatCount(video.danmaku),
                        color = Color.White,
                        fontSize = 10.sp,
                    )
                }
            }
            if (progress > 0f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .height(2.dp)
                        .background(colors.primary),
                )
            }
        }
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
            Text(
                text = video.title,
                color = colors.onBackground,
                fontSize = 14.sp,
                lineHeight = 19.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(5.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                UpBadge()
                if (video.isFollowed) {
                    Spacer(Modifier.width(4.dp))
                    FollowedBadge()
                }
                Spacer(Modifier.width(5.dp))
                Text(
                    text = video.ownerName,
                    color = colors.onBackgroundVariant,
                    fontSize = 11.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(interactionSource = null, indication = null) {
                            com.aritxonly.bdfun.TeakNavigator.openSpace(context, video.ownerMid)
                        },
                )
            }
        }
    }
}

/** 播放量 / 弹幕数图标：用用户提供的「播放键方框」「列表方框」两张图，颜色随文字。 */
@Composable
internal fun MetricGlyph(
    play: Boolean,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    size: androidx.compose.ui.unit.Dp = 13.dp,
) {
    Image(
        painter = androidx.compose.ui.res.painterResource(
            if (play) TeakIcon.MetricPlay else TeakIcon.MetricDanmaku,
        ),
        contentDescription = null,
        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(color),
        modifier = modifier.size(size),
    )
}
