package com.aritxonly.bdfun.presentation.pgc

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aritxonly.bdfun.BrandPink
import com.aritxonly.bdfun.core.model.pgc.BangumiRankEntry
import com.aritxonly.bdfun.feature.pgc.application.BangumiRankSection
import com.aritxonly.bdfun.feature.pgc.application.BangumiRankStateHolder
import com.aritxonly.bdfun.ui.FeedCardShape
import com.aritxonly.bdfun.ui.TeakLoading
import com.aritxonly.bdfun.ui.rememberImageRequest
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.ArrowRight
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 番剧页小节标题：与榜单分节标题同一套字号/字重。 */
@Composable
internal fun BangumiSectionTitle(text: String) {
    Text(
        text = text,
        color = MiuixTheme.colorScheme.onBackground,
        fontSize = 20.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 2.dp, bottom = 8.dp),
    )
}

/** 排行榜：每榜一条整行 item，滚到才请求；「查看更多」进独立页面。 */
internal fun LazyGridScope.bangumiRankSections(
    sections: List<BangumiRankSection>,
    holder: BangumiRankStateHolder,
    onOpenSeason: (Long) -> Unit,
    onOpenBoard: (BangumiRankSection) -> Unit,
) {
    sections.forEachIndexed { index, section ->
        item(key = "pgc-rank-${section.key}", span = { GridItemSpan(maxLineSpan) }) {
            RankSectionBlock(holder, index, section, onOpenSeason, onOpenBoard)
        }
    }
}

@Composable
private fun RankSectionBlock(
    holder: BangumiRankStateHolder,
    index: Int,
    fallback: BangumiRankSection,
    onOpenSeason: (Long) -> Unit,
    onOpenBoard: (BangumiRankSection) -> Unit,
) {
    // 必须在这里 collect：直接读 state.value 只拿到一次性快照，加载完成不会重组。
    val ui by holder.state.collectAsState()
    val section = ui.sections.getOrNull(index) ?: fallback
    LaunchedEffect(index) { holder.ensureSection(index) }
    Column(Modifier.fillMaxWidth()) {
        RankSectionHeader(section.title) { onOpenBoard(section) }
        Spacer(Modifier.height(8.dp))
        when {
            section.entries.isEmpty() && section.loading -> Box(
                Modifier.fillMaxWidth().height(120.dp),
                contentAlignment = Alignment.Center,
            ) { TeakLoading(size = 28.dp) }

            section.entries.isEmpty() -> RankErrorText(section.error ?: "暂无内容") {
                holder.retrySection(index)
            }

            else -> RankCardRow(section.entries.take(PREVIEW_COUNT), onOpenSeason)
        }
        Spacer(Modifier.height(6.dp))
    }
}

@Composable
private fun RankSectionHeader(title: String, onMore: () -> Unit) {
    val colors = MiuixTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .width(3.dp)
                .height(16.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(rankAccent(title)),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = title,
            color = colors.onBackground,
            fontSize = 20.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 220.dp),
        )
        Spacer(Modifier.weight(1f))
        // 网格左右各有 10dp padding，往右偏 10dp 才能贴到屏幕右边。
        Row(
            modifier = Modifier
                .offset(x = 10.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onMore)
                .padding(start = 8.dp, end = 10.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("查看更多", color = colors.onBackgroundVariant, fontSize = 13.sp)
            Spacer(Modifier.width(2.dp))
            androidx.compose.material3.Icon(
                imageVector = MiuixIcons.Basic.ArrowRight,
                contentDescription = null,
                tint = colors.onBackgroundVariant,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

/** 榜单横滑到两端时把剩余横向 delta 吃掉，别传给外层翻页手势。 */
private object HorizontalOverscrollConsumer : NestedScrollConnection {
    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset =
        if (available.x == 0f) Offset.Zero else Offset(available.x, 0f)
}

@Composable
private fun RankCardRow(entries: List<BangumiRankEntry>, onOpenSeason: (Long) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val cardWidth: Dp = (maxWidth - 16.dp) / 3.35f
        LazyRow(
            modifier = Modifier.nestedScroll(HorizontalOverscrollConsumer),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(entries, key = { it.seasonId }) { entry ->
                RankCard(entry, cardWidth) { onOpenSeason(entry.seasonId) }
            }
        }
    }
}

@Composable
private fun RankCard(entry: BangumiRankEntry, width: Dp, onClick: () -> Unit) {
    val colors = MiuixTheme.colorScheme
    Column(
        modifier = Modifier
            .width(width)
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
                model = rememberImageRequest(entry.cover),
                contentDescription = entry.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            // 底部渐变：压住浅色海报，保证「全X话」白字可读。
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0x99000000)))),
            )
            RankBadge(entry.rank, entry.cover, Modifier.align(Alignment.TopStart).padding(6.dp))
            if (entry.vip) RankVipBadge(Modifier.align(Alignment.TopEnd).padding(6.dp))
            if (entry.indexShow.isNotBlank()) {
                Text(
                    text = entry.indexShow,
                    color = Color.White,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(start = 6.dp, end = 6.dp, bottom = 6.dp),
                )
            }
        }
        Column(Modifier.padding(start = 6.dp, end = 6.dp, top = 5.dp, bottom = 6.dp)) {
            Text(
                text = entry.title,
                color = colors.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 13.sp,
                lineHeight = 17.sp,
            )
            // 高度要容得下一整行（含中文字形下伸部分），否则底部会被裁。
            Box(Modifier.height(18.dp), contentAlignment = Alignment.CenterStart) {
                if (entry.subtitle.isNotBlank()) {
                    Text(
                        text = entry.subtitle,
                        color = colors.onBackgroundVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                    )
                }
            }
        }
    }
}

/** 大会员角标：与「最近追番」卡片同款。 */
@Composable
private fun RankVipBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(BrandPink)
            .padding(horizontal = 5.dp, vertical = 1.dp),
    ) {
        Text("大会员", color = Color.White, fontSize = 8.5.sp, lineHeight = 11.sp, maxLines = 1)
    }
}

/** 名次角标：只标前三名。大号斜体数字，50% 往下渐隐到透明，不画底色；颜色随封面深浅自动反色。 */
@Composable
private fun RankBadge(rank: Int, coverUrl: String, modifier: Modifier = Modifier) {
    if (rank !in 1..3) return
    val ink = rememberRankInk(coverUrl)
    Text(
        text = rank.toString(),
        modifier = modifier.graphicsLayer { rotationZ = 2.5f },
        style = androidx.compose.ui.text.TextStyle(
            fontSize = 30.sp,
            lineHeight = 30.sp,
            fontWeight = FontWeight.Black,
            fontStyle = FontStyle.Italic,
            brush = Brush.verticalGradient(0f to ink, 0.5f to ink, 1f to ink.copy(alpha = 0f)),
            shadow = androidx.compose.ui.graphics.Shadow(
                color = if (ink.luminance() > 0.6f) Color(0x22FFFFFF) else Color(0x66000000),
                offset = Offset(1f, 1f),
                blurRadius = 4f,
            ),
        ),
        maxLines = 1,
    )
}

/** 取封面左上角亮度决定字色：浅底深灰、深底白灰。48px 缩略图采样，命中 Coil 缓存。 */
@Composable
private fun rememberRankInk(coverUrl: String): Color {
    val context = LocalContext.current
    var lightCover by remember(coverUrl) { mutableStateOf(false) }
    LaunchedEffect(coverUrl) {
        if (coverUrl.isBlank()) return@LaunchedEffect
        val request = coil.request.ImageRequest.Builder(context)
            .data(coverUrl)
            .size(48)
            .allowHardware(false)
            .crossfade(false)
            .build()
        val result = runCatching { coil.Coil.imageLoader(context).execute(request) }.getOrNull()
        val bitmap = ((result as? coil.request.SuccessResult)?.drawable
            as? android.graphics.drawable.BitmapDrawable)?.bitmap ?: return@LaunchedEffect
        lightCover = topLeftIsLight(bitmap)
    }
    return if (lightCover) Color(0xFF3C3C3C) else Color(0xFFF5F5F5)
}

private fun topLeftIsLight(bitmap: android.graphics.Bitmap): Boolean = runCatching {
    val readable = if (bitmap.config == android.graphics.Bitmap.Config.HARDWARE) {
        bitmap.copy(android.graphics.Bitmap.Config.ARGB_8888, false) ?: return false
    } else {
        bitmap
    }
    val w = (readable.width / 3).coerceAtLeast(1)
    val h = (readable.height / 3).coerceAtLeast(1)
    val pixels = IntArray(w * h)
    readable.getPixels(pixels, 0, w, 0, 0, w, h)
    var sum = 0.0
    pixels.forEach { pixel ->
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF
        sum += 0.299 * r + 0.587 * g + 0.114 * b
    }
    if (readable !== bitmap) readable.recycle()
    (sum / pixels.size.coerceAtLeast(1)) > 165.0
}.getOrDefault(false)

@Composable
private fun RankErrorText(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(message, color = MiuixTheme.colorScheme.onBackgroundVariant, fontSize = 13.sp)
        Text(
            text = "点击重试",
            color = MiuixTheme.colorScheme.primary,
            fontSize = 13.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onRetry)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

/** 每个榜单先露出的卡片数。 */
private const val PREVIEW_COUNT = 6

/** 榜单主题色：按榜名匹配，改榜名也不会掉色。 */
internal fun rankAccent(title: String): Color = when {
    title.contains("热门动画") -> Color(0xFFB3261E)
    title.contains("搞笑") -> Color(0xFFFFC107)
    title.contains("热血") -> Color(0xFFFF3B30)
    title.contains("奇幻") -> Color(0xFF34C759)
    title.contains("励志") -> Color(0xFFD4A017)
    title.contains("萌系") -> Color(0xFFFF8FAB)
    title.contains("运动") -> Color(0xFF2F80ED)
    title.contains("机战") -> Color(0xFF8E44AD)
    title.contains("小说改编") -> Color(0xFF8D6E63)
    title.contains("漫画改编") -> Color(0xFF26A69A)
    title.contains("原创") -> Color(0xFFFF8A00)
    title.contains("恋爱") -> Color(0xFFFF4F9A)
    title.contains("战斗") -> Color(0xFFE64A19)
    title.contains("泡面") -> Color(0xFFF2C94C)
    title.contains("动态漫") -> Color(0xFF4DB6AC)
    else -> Color(0xFF9E9E9E)
}
