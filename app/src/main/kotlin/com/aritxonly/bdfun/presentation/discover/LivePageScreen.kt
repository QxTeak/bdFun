package com.aritxonly.bdfun.presentation.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aritxonly.bdfun.core.model.feed.FeedLiveRecommendation
import com.aritxonly.bdfun.core.model.live.LiveArea
import com.aritxonly.bdfun.core.model.live.LiveCard
import com.aritxonly.bdfun.feature.discover.application.DiscoverGridStates
import com.aritxonly.bdfun.feature.discover.application.LivePageStateHolder
import com.aritxonly.bdfun.navigateToLiveRoom
import com.aritxonly.bdfun.navigateToLogin
import com.aritxonly.bdfun.ui.FeedCardShape
import com.aritxonly.bdfun.ui.FeedLiveCard
import com.aritxonly.bdfun.ui.HomePullIndicator
import com.aritxonly.bdfun.ui.HomeTopBarHeight
import com.aritxonly.bdfun.ui.TeakLoading
import com.aritxonly.bdfun.ui.rememberImageRequest
import com.aritxonly.bdfun.ui.showToast
import com.aritxonly.teak.common.chrome.TeakGlass
import com.kyant.shapes.Capsule
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 首页第一段「直播」。
 * 第一行：关注的正在直播（与动态页首个圆角矩形同构）；
 * 第二行：分区 chips（收藏夹列表同款，去掉高光描边）；
 * 下面：分区直播流卡片（看过人数 + 直播内容）。
 */
@Composable
internal fun LivePage(state: LivePageStateHolder, loggedIn: Boolean) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val freshGridState = rememberLazyGridState()
    val gridState = remember(freshGridState) {
        DiscoverGridStates.live ?: freshGridState.also { DiscoverGridStates.live = it }
    }
    var pullOffset by remember { mutableFloatStateOf(0f) }
    val thresholdPx = with(density) { 88.dp.toPx() }
    val pull = remember(state) {
        DiscoverPullSession(
            density = density,
            thresholdPx = thresholdPx,
            maxPullPx = with(density) { 260.dp.toPx() },
            currentOffset = { pullOffset },
            onPull = { pullOffset = it; state.pullFraction = (it / thresholdPx).coerceIn(0f, 1f) },
            onPullDp = { state.pullOffsetDp = it },
            canScrollBackward = { !gridState.canScrollBackward },
            isRefreshing = { state.refreshing },
            onRefresh = { scope.launch { state.refresh() } },
        )
    }
    val refreshConnection = remember(state) { pull.connection() }

    LaunchedEffect(state) { state.ensureLoaded() }
    // 刷新结束：先收小电视，再让内容平滑回顶（与推荐页同一条时序）。
    LaunchedEffect(state.refreshing) {
        if (!state.refreshing && pullOffset > 0f) {
            state.pullFraction = 0f
            kotlinx.coroutines.delay(160)
            pull.animatePullTo(0f, 260f)
        }
    }
    // 滚到底部预取下一页
    LaunchedEffect(state) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .collect { last ->
                val total = gridState.layoutInfo.totalItemsCount
                if (state.cards.isNotEmpty() && total > 0 && last >= total - 2) state.loadMore()
            }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            state = gridState,
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(0, pullOffset.toInt()) }
                .nestedScrollStrict(refreshConnection),
            contentPadding = PaddingValues(
                start = 12.dp,
                end = 12.dp,
                top = HomeTopBarHeight + statusBarTop + 6.dp,
                bottom = 104.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.following.isNotEmpty()) {
                item(key = "live-follow", span = { GridItemSpan(maxLineSpan) }) {
                    Column {
                        SectionTitle("关注的直播")
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 2.dp),
                        ) {
                            items(state.following, key = { it.roomId }) { live ->
                                Box(modifier = Modifier.width(250.dp)) {
                                    FeedLiveCard(live.toFeedLive())
                                }
                            }
                        }
                    }
                }
            }

            if (state.areas.isNotEmpty()) {
                item(key = "live-areas", span = { GridItemSpan(maxLineSpan) }) {
                    Column {
                        Spacer(Modifier.height(if (state.following.isEmpty()) 0.dp else 12.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 6.dp),
                        ) {
                            item(key = "area-all") {
                                LiveAreaChip(
                                    text = "推荐",
                                    selected = state.selectedKey == 0L,
                                    onClick = { scope.launch { state.selectArea(LiveArea(0L, "推荐")) } },
                                )
                            }
                            items(state.areas, key = { "${it.parentId}-${it.id}" }) { area ->
                                LiveAreaChip(
                                    text = area.name,
                                    selected = state.selectedKey == area.id,
                                    onClick = { scope.launch { state.selectArea(area) } },
                                )
                            }
                        }
                    }
                }
            }

            items(state.cards, key = { it.roomId }) { live ->
                LiveGridCard(live) { navigateToLiveRoom(context, live.roomId) }
            }

            if (state.cards.isEmpty() && state.loading) {
                item(key = "live-loading", span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 60.dp),
                        contentAlignment = Alignment.Center,
                    ) { TeakLoading(size = 40.dp) }
                }
            }

            state.error?.let { message ->
                if (state.cards.isEmpty()) {
                    item(key = "live-error", span = { GridItemSpan(maxLineSpan) }) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 60.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(message, color = MiuixTheme.colorScheme.onBackgroundVariant, fontSize = 14.sp)
                            Spacer(Modifier.height(10.dp))
                            TeakGlass(
                                modifier = Modifier.height(34.dp),
                                shape = Capsule(),
                                onClick = {
                                    state.retry()
                                    scope.launch { state.refresh() }
                                },
                            ) {
                                Text(
                                    text = "重试",
                                    color = MiuixTheme.colorScheme.primary,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(horizontal = 18.dp).align(Alignment.Center),
                                )
                            }
                        }
                    }
                }
            }

            if (!loggedIn && state.following.isEmpty()) {
                item(key = "live-login", span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(FeedCardShape)
                            .background(MiuixTheme.colorScheme.surfaceContainer)
                            .clickable(interactionSource = null, indication = null) {
                                navigateToLogin(context)
                            }
                            .padding(16.dp),
                    ) {
                        Text("登录后在这里看到关注的直播", color = MiuixTheme.colorScheme.onBackgroundVariant, fontSize = 13.sp)
                    }
                }
            }
        }

        HomePullIndicator(
            reveal = if (state.refreshing) 1f else ((state.pullOffsetDp - 58f) / 26f).coerceIn(0f, 1f),
            animated = state.refreshing,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = HomeTopBarHeight + 4.dp),
        )
    }
}

private fun Modifier.nestedScrollStrict(connection: androidx.compose.ui.input.nestedscroll.NestedScrollConnection): Modifier =
    this.nestedScroll(connection)

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = MiuixTheme.colorScheme.onBackground,
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(start = 2.dp, bottom = 8.dp),
    )
}

@Composable
private fun LiveAreaChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val colors = MiuixTheme.colorScheme
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) colors.primary else colors.surfaceContainer)
            .clickable(interactionSource = null, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else colors.onBackground,
            fontSize = 12.sp,
            maxLines = 1,
        )
    }
}

/** 直播卡片：结构同首页视频卡片，统计行换成「xx人看过」+ 直播内容。 */
@Composable
private fun LiveGridCard(live: LiveCard, onClick: () -> Unit) {
    val colors = MiuixTheme.colorScheme
    Column(
        modifier = Modifier
            .clip(FeedCardShape)
            .background(colors.surfaceContainer)
            .clickable(interactionSource = null, indication = null, onClick = onClick),
    ) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(colors.surfaceContainerHigh)) {
            AsyncImage(
                model = rememberImageRequest(live.cover),
                contentDescription = live.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(42.dp)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0x4D000000)))),
            )
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(start = 6.dp, end = 6.dp, bottom = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (live.watchedText.isNotBlank()) {
                    Text(live.watchedText, color = Color.White, fontSize = 10.sp, lineHeight = 11.sp)
                }
                Spacer(Modifier.weight(1f))
                if (live.areaName.isNotBlank()) {
                    Text(live.areaName, color = Color.White, fontSize = 10.sp, lineHeight = 11.sp)
                }
            }
        }
        Column(modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 7.dp, bottom = 8.dp)) {
            Text(
                text = live.title,
                color = colors.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontSize = 13.5.sp,
                lineHeight = 18.sp,
                modifier = Modifier.heightIn(min = 36.dp),
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = live.uname,
                color = colors.onBackgroundVariant,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun LiveCard.toFeedLive() = FeedLiveRecommendation(
    roomId = roomId,
    title = title,
    cover = cover,
    live = true,
    areaName = areaName,
    watched = watchedText,
)
