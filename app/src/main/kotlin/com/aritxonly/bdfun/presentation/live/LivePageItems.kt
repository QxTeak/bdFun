package com.aritxonly.bdfun.presentation.live

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aritxonly.bdfun.core.model.feed.FeedLiveRecommendation
import com.aritxonly.bdfun.core.model.live.LiveArea
import com.aritxonly.bdfun.core.model.live.LiveCard
import com.aritxonly.bdfun.feature.live.application.LivePageStateHolder
import com.aritxonly.bdfun.presentation.common.discover.DiscoverErrorState
import com.aritxonly.bdfun.presentation.common.discover.DiscoverSectionTitle
import com.aritxonly.bdfun.navigateToLogin
import com.aritxonly.bdfun.ui.FeedCardShape
import com.aritxonly.bdfun.ui.FeedLiveCard
import com.aritxonly.bdfun.ui.TeakLoading
import com.aritxonly.bdfun.ui.rememberImageRequest
import com.aritxonly.teak.common.chrome.TeakGlass
import com.kyant.shapes.Capsule
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 第一行：关注的正在直播，直接复用动态页那个圆角直播卡片。 */
internal fun LazyGridScope.liveFollowSection(state: LivePageStateHolder) {
    if (state.following.isEmpty()) return
    item(key = "live-follow", span = { GridItemSpan(maxLineSpan) }) {
        Column {
            DiscoverSectionTitle("关注的直播")
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

/** 第二行：分区胶囊，收藏夹列表同款（选中没有高光描边）。 */
internal fun LazyGridScope.liveAreaSection(
    state: LivePageStateHolder,
    scope: kotlinx.coroutines.CoroutineScope,
) {
    if (state.areas.isEmpty()) return
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

/** 网格里的加载 / 空态 / 未登录提示。 */
internal fun LazyGridScope.livePageStateItems(
    state: LivePageStateHolder,
    scope: kotlinx.coroutines.CoroutineScope,
    loggedIn: Boolean,
) {
    if (state.cards.isEmpty() && state.loading) {
        item(key = "live-loading", span = { GridItemSpan(maxLineSpan) }) {
            Box(Modifier.fillMaxWidth().padding(vertical = 60.dp), contentAlignment = Alignment.Center) {
                TeakLoading(size = 40.dp)
            }
        }
    }
    state.error?.let { message ->
        if (state.cards.isNotEmpty()) return@let
        item(key = "live-error", span = { GridItemSpan(maxLineSpan) }) {
            DiscoverErrorState(message) {
                state.retry()
                scope.launch { state.refresh() }
            }
        }
    }
    if (!loggedIn && state.following.isEmpty()) {
        item(key = "live-login", span = { GridItemSpan(maxLineSpan) }) {
            val context = LocalContext.current
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
                Text(
                    "登录后在这里看到关注的直播",
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                    fontSize = 13.sp,
                )
            }
        }
    }
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
internal fun LiveGridCard(live: LiveCard, onClick: () -> Unit) {
    val colors = MiuixTheme.colorScheme
    Column(
        modifier = Modifier
            .clip(FeedCardShape)
            .background(colors.surfaceContainer)
            .clickable(interactionSource = null, indication = null, onClick = onClick),
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(colors.surfaceContainerHigh)) {
            AsyncImage(
                model = rememberImageRequest(live.cover),
                contentDescription = live.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                Modifier
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
        Column(Modifier.padding(start = 8.dp, end = 8.dp, top = 7.dp, bottom = 8.dp)) {
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
