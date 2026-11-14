package com.aritxonly.bdfun.presentation.pgc

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
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
import com.aritxonly.bdfun.BrandPink
import com.aritxonly.bdfun.core.model.pgc.BangumiCard
import com.aritxonly.bdfun.feature.pgc.application.BangumiPageStateHolder
import com.aritxonly.bdfun.presentation.common.discover.DiscoverErrorState
import com.aritxonly.bdfun.presentation.common.discover.DiscoverSectionTitle
import com.aritxonly.bdfun.ui.FeedCardShape
import com.aritxonly.bdfun.ui.TeakLoading
import com.aritxonly.bdfun.ui.rememberImageRequest
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 第一项固定是「最近追番」：即使列表为空/还在加载也保留这一节，
 *  这样网格的第 0 项永远不变，进页面就锚在顶部，不会因为数据晚到被带到榜单上。 */
internal fun LazyGridScope.bangumiFollowSection(
    state: BangumiPageStateHolder,
    context: android.content.Context,
) {
    item(key = "pgc-follow", span = { GridItemSpan(maxLineSpan) }) {
        val openScope = rememberCoroutineScope()
        Column {
            BangumiSectionTitle("最近追番")
            if (state.following.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 2.dp),
                ) {
                    items(state.following, key = { it.seasonId }) { item ->
                        Box(Modifier.width(104.dp)) {
                            BangumiCardItem(item) { openBangumi(context, item.seasonId, openScope) }
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

internal fun LazyGridScope.bangumiPageStateItems(
    state: BangumiPageStateHolder,
    scope: kotlinx.coroutines.CoroutineScope,
) {
    if (state.cards.isEmpty() && state.loading) {
        item(key = "pgc-loading", span = { GridItemSpan(maxLineSpan) }) {
            Box(Modifier.fillMaxWidth().padding(vertical = 60.dp), contentAlignment = Alignment.Center) {
                TeakLoading(size = 40.dp)
            }
        }
    }
    state.error?.let { message ->
        if (state.cards.isNotEmpty()) return@let
        item(key = "pgc-error", span = { GridItemSpan(maxLineSpan) }) {
            DiscoverErrorState(message) {
                state.retry()
                scope.launch { state.refresh() }
            }
        }
    }
}

/** 番剧卡片：3:4 长封面，「xx万追番」在封面左下，UP 主位置换成「全xx话」。 */
@Composable
internal fun BangumiCardItem(item: BangumiCard, onClick: () -> Unit) {
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
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0x99000000)))),
            )
            if (item.followText.isNotBlank()) {
                Text(
                    text = item.followText,
                    color = Color.White,
                    fontSize = 10.sp,
                    lineHeight = 11.sp,
                    modifier = Modifier.align(Alignment.BottomStart).padding(start = 6.dp, bottom = 5.dp),
                )
            }
            if (item.vip) BangumiVipBadge()
        }
        Column(Modifier.padding(start = 6.dp, end = 6.dp, top = 6.dp, bottom = 6.dp)) {
            Text(
                text = item.title,
                color = colors.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontSize = 12.5.sp,
                lineHeight = 16.5.sp,
                modifier = Modifier.heightIn(min = 33.dp),
            )
            Spacer(Modifier.height(2.dp))
            // 固定高度：没有「全x话」的卡片也留出同样空间，保证一排卡片底边齐平。
            Box(Modifier.height(18.dp), contentAlignment = Alignment.CenterStart) {
                if (item.indexShow.isNotBlank()) {
                    Text(
                        text = item.indexShow,
                        color = colors.onBackgroundVariant,
                        fontSize = 10.5.sp,
                        lineHeight = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** 与个人主页完全同款：品牌粉胶囊 + 白字。 */
@Composable
private fun androidx.compose.foundation.layout.BoxScope.BangumiVipBadge() {
    Box(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(6.dp)
            .clip(CircleShape)
            .background(BrandPink)
            .padding(horizontal = 5.dp, vertical = 1.dp),
    ) {
        Text("大会员", color = Color.White, fontSize = 8.5.sp, lineHeight = 11.sp, maxLines = 1)
    }
}

/** 解析首集后进视频详情页（复用它的播放/评论/交互），失败才退回网页。 */
internal fun openBangumi(context: android.content.Context, seasonId: Long, scope: kotlinx.coroutines.CoroutineScope) {
    if (seasonId <= 0L) return
    scope.launch {
        val bvid = runCatching { com.aritxonly.bdfun.AppGraph.bangumiFirstEpisode(seasonId) }
            .getOrNull()?.bvid.orEmpty()
        if (bvid.isNotBlank()) {
            val opened = runCatching {
                context.startActivity(
                    android.content.Intent(context, com.aritxonly.bdfun.VideoActivity::class.java)
                        .putExtra("bvid", bvid),
                )
            }.isSuccess
            if (opened) return@launch
        }
        runCatching {
            context.startActivity(
                android.content.Intent(
                    android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse("https://www.bilibili.com/bangumi/play/ss$seasonId"),
                ),
            )
        }.onFailure {
            android.widget.Toast.makeText(context, "无法打开番剧", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}


/** 番剧打开动作：解析首集要发请求，所以绑定调用方的协程作用域。 */
@androidx.compose.runtime.Composable
internal fun rememberBangumiOpener(context: android.content.Context): (Long) -> Unit {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    return androidx.compose.runtime.remember(context, scope) {
        { seasonId: Long -> openBangumi(context, seasonId, scope) }
    }
}
