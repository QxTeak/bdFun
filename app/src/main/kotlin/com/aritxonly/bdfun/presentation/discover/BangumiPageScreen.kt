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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
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
import com.aritxonly.bdfun.BrandPink
import com.aritxonly.bdfun.core.model.pgc.BangumiCard
import com.aritxonly.bdfun.feature.discover.application.BangumiPageStateHolder
import com.aritxonly.bdfun.feature.discover.application.DiscoverGridStates
import com.aritxonly.bdfun.ui.FeedCardShape
import com.aritxonly.bdfun.ui.HomePullIndicator
import com.aritxonly.bdfun.ui.HomeTopBarHeight
import com.aritxonly.bdfun.ui.TeakLoading
import com.aritxonly.bdfun.ui.rememberImageRequest
import com.aritxonly.teak.common.chrome.TeakGlass
import com.kyant.shapes.Capsule
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 首页第三段「番剧」。
 * 第一行：最近追番（横向长封面卡片）；
 * 下面：推荐（两列长封面卡片）。封面 3:4，统计行是「xx万追番」，
 * UP 主位置换成「全xx话」，需要大会员的右上角挂与个人主页同款角标。
 */
@Composable
internal fun BangumiPage(state: BangumiPageStateHolder) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val freshGridState = rememberLazyGridState()
    val gridState = remember(freshGridState) {
        DiscoverGridStates.bangumi ?: freshGridState.also { DiscoverGridStates.bangumi = it }
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
    LaunchedEffect(state.refreshing) {
        if (!state.refreshing && pullOffset > 0f) {
            state.pullFraction = 0f
            kotlinx.coroutines.delay(160)
            pull.animatePullTo(0f, 260f)
        }
    }
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
                .nestedScroll(refreshConnection),
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
                item(key = "pgc-follow", span = { GridItemSpan(maxLineSpan) }) {
                    Column {
                        Text(
                            text = "最近追番",
                            color = MiuixTheme.colorScheme.onBackground,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(start = 2.dp, bottom = 8.dp),
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 2.dp),
                        ) {
                            items(state.following, key = { it.seasonId }) { item ->
                                Box(modifier = Modifier.width(116.dp)) {
                                    BangumiCardItem(item) { openBangumi(context, item.seasonId) }
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }

            items(state.cards, key = { it.seasonId }) { item ->
                BangumiCardItem(item) { openBangumi(context, item.seasonId) }
            }

            if (state.cards.isEmpty() && state.loading) {
                item(key = "pgc-loading", span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 60.dp),
                        contentAlignment = Alignment.Center,
                    ) { TeakLoading(size = 40.dp) }
                }
            }

            state.error?.let { message ->
                if (state.cards.isEmpty()) {
                    item(key = "pgc-error", span = { GridItemSpan(maxLineSpan) }) {
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

@Composable
private fun BangumiCardItem(item: BangumiCard, onClick: () -> Unit) {
    val colors = MiuixTheme.colorScheme
    Column(
        modifier = Modifier
            .clip(FeedCardShape)
            .background(colors.surfaceContainer)
            .clickable(interactionSource = null, indication = null, onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .background(colors.surfaceContainerHigh),
        ) {
            AsyncImage(
                model = rememberImageRequest(item.cover),
                contentDescription = item.title,
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
            if (item.followText.isNotBlank()) {
                Text(
                    text = item.followText,
                    color = Color.White,
                    fontSize = 10.sp,
                    lineHeight = 11.sp,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 6.dp, bottom = 5.dp),
                )
            }
            if (item.vip) {
                // 与个人主页「大会员」完全同款：品牌粉胶囊 + 白字。
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(CircleShape)
                        .background(BrandPink)
                        .padding(horizontal = 5.dp, vertical = 1.dp),
                ) {
                    Text(
                        text = "大会员",
                        color = Color.White,
                        fontSize = 8.5.sp,
                        lineHeight = 11.sp,
                        maxLines = 1,
                    )
                }
            }
        }
        Column(modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 7.dp, bottom = 8.dp)) {
            Text(
                text = item.title,
                color = colors.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontSize = 13.5.sp,
                lineHeight = 18.sp,
                modifier = Modifier.heightIn(min = 36.dp),
            )
            if (item.indexShow.isNotBlank()) {
                Spacer(Modifier.height(5.dp))
                Text(
                    text = item.indexShow,
                    color = colors.onBackgroundVariant,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun openBangumi(context: android.content.Context, seasonId: Long) {
    if (seasonId <= 0L) return
    runCatching {
        context.startActivity(
            android.content.Intent(
                android.content.Intent.ACTION_VIEW,
                android.net.Uri.parse("https://www.bilibili.com/bangumi/play/ss$seasonId"),
            ),
        )
    }.onFailure { android.widget.Toast.makeText(context, "无法打开番剧", android.widget.Toast.LENGTH_SHORT).show() }
}
