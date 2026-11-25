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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aritxonly.bdfun.core.model.live.LiveArea
import com.aritxonly.bdfun.core.model.live.LiveCard
import com.aritxonly.bdfun.presentation.live.LivePageStateHolder
import com.aritxonly.bdfun.presentation.common.discover.DiscoverErrorState
import com.aritxonly.bdfun.presentation.common.discover.DiscoverSectionTitle
import com.aritxonly.bdfun.navigateToLogin
import com.aritxonly.bdfun.ui.AnchorBadge
import com.aritxonly.bdfun.ui.ContinuousCornerShape
import com.aritxonly.bdfun.ui.FeedCardShape
import com.aritxonly.bdfun.ui.LiveAvatar
import com.aritxonly.bdfun.ui.TeakLoading
import com.aritxonly.bdfun.ui.rememberImageRequest
import com.aritxonly.teak.common.navigation.TeakChip
import com.aritxonly.teak.common.navigation.TeakSlidingChipRow
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 第一行：我的关注里正在直播的 UP。
 * 小标题在卡片外面（和番剧页「最近追番」同款大小/位置），圆角卡片只包住头像行。
 */
internal fun LazyGridScope.liveFollowSection(state: LivePageStateHolder, onOpenRoom: (Long) -> Unit) {
    if (state.followers.isEmpty()) return
    item(key = "live-follow", span = { GridItemSpan(maxLineSpan) }) {
        Column(Modifier.fillMaxWidth()) {
            // 和番剧页小节标题同一份实现（DiscoverSectionTitle：20sp Bold）。
            DiscoverSectionTitle("正在直播")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ContinuousCornerShape(18.dp))
                    .background(MiuixTheme.colorScheme.surfaceContainer),
            ) {
                LazyRow(
                    // 和动态页顶部圆角卡内一致：14dp 内边距 + 12dp 头像间距。
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(14.dp),
                ) {
                    items(state.followers, key = { it.roomId }) { user ->
                        LiveAvatar(name = user.name, face = user.face, onClick = { onOpenRoom(user.roomId) })
                    }
                }
            }
        }
    }
}

/** 第二行：分区胶囊，形状/尺寸和顶栏分段指示器一致。 */
internal fun LazyGridScope.liveAreaSection(
    state: LivePageStateHolder,
    scope: kotlinx.coroutines.CoroutineScope,
) {
    if (state.areas.isEmpty()) return
    item(key = "live-areas", span = { GridItemSpan(maxLineSpan) }) {
        Column {
            Spacer(Modifier.height(if (state.followers.isEmpty()) 0.dp else 4.dp))
            LiveAreaChips(state, scope)
        }
    }
}

/**
 * 分区标签：选中底色由 TeakUI 的分段同款弹簧在胶囊之间滑动，
 * 不再是「点谁谁瞬间变粉、上一个瞬间变白」的两段硬切。
 */
@Composable
private fun LiveAreaChips(state: LivePageStateHolder, scope: kotlinx.coroutines.CoroutineScope) {
    val areas = state.areas
    val chips = remember(areas) {
        buildList {
            add(TeakChip(RecommendedAreaKey, "推荐"))
            areas.forEach { add(TeakChip(it.id, it.name)) }
        }
    }
    val areasById = remember(areas) { areas.associateBy { it.id } }
    TeakSlidingChipRow(
        chips = chips,
        // LiveArea.id 已按「area_v2_id 为 0 时退回 parent id」保证唯一，直接当选中键用。
        selectedKey = state.selectedKey,
        onSelect = { chip ->
            scope.launch {
                val from = chips.indexOfFirst { it.key == state.selectedKey }
                val to = chips.indexOfFirst { it.key == chip.key }
                state.selectArea(areasById[chip.key] ?: LiveArea(RecommendedAreaKey, "推荐"), to - from)
            }
        },
        // 和原来 FullBleedRow 一样：抵消网格的 12dp 内容边距，能滚到屏幕边缘再被裁切。
        bleed = 12.dp,
        verticalPadding = 4.dp,
    )
}

private const val RecommendedAreaKey = 0L

/** 网格里的加载 / 空态 / 未登录提示。 */
internal fun LazyGridScope.livePageStateItems(
    state: LivePageStateHolder,
    scope: kotlinx.coroutines.CoroutineScope,
    loggedIn: Boolean,
) {
    if (state.cards.isEmpty() && state.loading) {
        item(key = "live-loading", span = { GridItemSpan(maxLineSpan) }) {
            com.aritxonly.teak.common.chrome.TeakLoadingArea()
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
    if (!loggedIn && state.followers.isEmpty()) {
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

/** 直播卡片：结构同首页视频卡片，统计行换成「xx人看过」+ 直播内容。 */
@Composable
internal fun LiveGridCard(live: LiveCard, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = MiuixTheme.colorScheme
    Column(
        modifier = modifier
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
            // 主播名前缀「主播」：和首页卡片的「UP」标签同一个组件，只换文字。
            Row(verticalAlignment = Alignment.CenterVertically) {
                AnchorBadge()
                Spacer(Modifier.width(4.dp))
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
}
