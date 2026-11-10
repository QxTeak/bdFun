package com.aritxonly.bdfun

import com.aritxonly.teak.common.chrome.TrackTeakPageScroll
import com.aritxonly.teak.common.chrome.TeakPageChrome
import com.aritxonly.bdfun.ui.UnfinishedNoticeOnce
import com.aritxonly.bdfun.ui.rememberImageRequest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aritxonly.teak.common.material.LocalTeakBackdrop
import com.aritxonly.bdfun.data.DynamicItem
import com.aritxonly.bdfun.data.SpaceInfo
import com.aritxonly.bdfun.data.VideoItem
import com.aritxonly.bdfun.ui.ContinuousCornerShape
import com.aritxonly.bdfun.ui.ResourceGlyph
import com.aritxonly.bdfun.ui.TeakActivityScaffold
import com.aritxonly.bdfun.ui.TeakIcon
import com.aritxonly.bdfun.ui.TeakLoading
import com.aritxonly.teak.common.navigation.TeakPagerTabs
import com.aritxonly.bdfun.ui.VideoGridCard
import com.aritxonly.bdfun.ui.formatCount
import com.aritxonly.bdfun.ui.showToast
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.icon.extended.Store
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.roundToInt

class SpaceActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppGraph.ensure(applicationContext)
        val mid = AppGraph.pendingSpaceMid.value
        if (mid == null || mid <= 0) {
            finish()
            return
        }
        setContent {
            val settings = AppGraph.settings
            var title by remember { mutableStateOf("个人主页") }
            TeakActivityScaffold(
                settings = settings,
                onBack = { finish() },
                title = title,
            ) {
                SpaceScreen(
                    mid = mid,
                    onTitleChange = { if (it.isNotBlank()) title = it },
                )
            }
            UnfinishedNoticeOnce()
        }
    }
}

private data class CachedSpace(
    val space: SpaceInfo,
    val dynamics: List<DynamicItem>,
    val followed: Boolean,
    val at: Long,
)

/** 个人主页资料 5 分钟进程内缓存：再次进入先出内容，再后台刷新。 */
private object SpaceCache {
    const val TTL = 5 * 60 * 1000L
    val map = mutableMapOf<Long, CachedSpace>()
}

@Composable
private fun SpaceScreen(
    mid: Long,
    onTitleChange: (String) -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cached = remember(mid) { SpaceCache.map[mid] }

    var space by remember(mid) { mutableStateOf(cached?.space) }
    var dynamics by remember(mid) { mutableStateOf(cached?.dynamics.orEmpty()) }
    var loading by remember(mid) { mutableStateOf(cached == null) }
    var followed by remember(mid) {
        mutableStateOf(cached?.followed ?: cached?.space?.isFollowed ?: false)
    }
    var followBusy by remember { mutableStateOf(false) }

    val profile = space?.profile
    val isSelf = AppGraph.session.value?.mid == mid

    LaunchedEffect(mid, isSelf) {
        if (cached == null) loading = true
        val fetched = runCatching { AppGraph.api.space(mid) }.getOrNull()
        if (fetched != null) space = fetched
        val fetchedDynamics = runCatching { AppGraph.api.userDynamics(mid) }.getOrNull()
        if (fetchedDynamics != null) dynamics = fetchedDynamics
        val attribute = if (!isSelf && AppGraph.api.isLoggedIn) {
            runCatching { AppGraph.api.relation(mid) }.getOrNull()
        } else {
            null
        }
        followed = when {
            isSelf -> false
            attribute != null -> attribute == 2 || attribute == 6
            fetched != null -> fetched.isFollowed
            cached != null -> cached.followed
            else -> false
        }
        loading = false
        val resolved = fetched ?: space
        if (resolved != null) {
            SpaceCache.map[mid] = CachedSpace(
                space = resolved,
                dynamics = fetchedDynamics ?: dynamics,
                followed = followed,
                at = System.currentTimeMillis(),
            )
        }
    }

    LaunchedEffect(profile?.name) {
        onTitleChange(profile?.name.orEmpty().ifBlank { "个人主页" })
    }

    val verticalScroll = rememberScrollState()
    val horizontalScroll = rememberScrollState()
    TrackTeakPageScroll(verticalScroll)
    val collapse = com.aritxonly.teak.common.chrome.LocalTeakPageScrollState.current.collapse

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val pageWidth: Dp = maxWidth
        val pageWidthPx = with(LocalDensity.current) { pageWidth.toPx() }

        LaunchedEffect(horizontalScroll.isScrollInProgress, pageWidthPx) {
            if (!horizontalScroll.isScrollInProgress && pageWidthPx > 0f) {
                val target = (horizontalScroll.value / pageWidthPx)
                    .roundToInt()
                    .coerceIn(0, 4)
                runCatching {
                    horizontalScroll.animateScrollTo(
                        (target.toFloat() * pageWidthPx).roundToInt(),
                    )
                }
            }
        }
        val currentPage by remember(pageWidthPx) {
            derivedStateOf {
                if (pageWidthPx <= 0f) {
                    0
                } else {
                    (horizontalScroll.value / pageWidthPx).roundToInt().coerceIn(0, 4)
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(verticalScroll)
                .statusBarsPadding(),
        ) {
            // 大标题：和设置 / 收藏等老页面同一位置（顶部 8 + 52，正文左 16 + 10）
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = 1f - (collapse / 0.65f).coerceIn(0f, 1f)
                    }
                    .blur((12f * (collapse / 0.65f).coerceIn(0f, 1f)).dp)
                    .padding(horizontal = 16.dp),
            ) {
                Spacer(Modifier.height(60.dp))
                Text(
                    text = profile?.name.orEmpty().ifBlank { "个人主页" },
                    color = colors.onBackground,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 10.dp),
                )
                Spacer(Modifier.height(14.dp))
            }

            // 头图：全宽，不再做成卡片
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(178.dp)
                    .background(colors.surfaceContainerHigh),
            ) {
                val banner = profile?.banner.orEmpty()
                if (banner.isNotBlank()) {
                    AsyncImage(
                        model = rememberImageRequest(banner, crossfade = false),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFF2A2D36), Color(0xFF15161B)),
                                ),
                            ),
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.30f)),
                            ),
                        ),
                )
                // 右上角：搜索 / 更多
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 10.dp, end = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    BannerCircleButton(MiuixIcons.Search, "搜索") {
                        context.showToast("搜索暂未开放")
                    }
                    BannerCircleButton(MiuixIcons.More, "更多") {
                        context.showToast("更多暂未开放")
                    }
                }
                BannerCircleButton(
                    MiuixIcons.Store,
                    "装扮",
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 14.dp, bottom = 74.dp),
                ) {
                    context.showToast("装扮暂未开放")
                }
            }

            // 头像压住头图下沿；右侧是粉丝 / 关注 / 获赞 + 关注按钮
            // Box 只占 44dp，Row 向上探出 44dp，形成头像压在头图上的效果
            Box(modifier = Modifier.fillMaxWidth().height(44.dp)) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(y = (-44).dp)
                        // Box 只有 44dp 高，这里允许 Row 按内容真实高度测量，
                        // 否则粉丝/关注下方的关注按钮会被 44dp 的约束裁掉
                        .wrapContentHeight(unbounded = true)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box {
                        AsyncImage(
                            model = rememberImageRequest(profile?.face.orEmpty()),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape)
                                .background(colors.surfaceContainerHigh)
                                .border(3.dp, colors.surface, CircleShape),
                        )
                        if (profile?.vip == true) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .offset(x = 2.dp, y = 2.dp)
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(colors.primary)
                                    .border(2.dp, colors.surface, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "大",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        // 无分割线：三项等宽居中
                        Row(modifier = Modifier.fillMaxWidth()) {
                            SpaceStat("粉丝", profile?.follower ?: 0, Modifier.weight(1f))
                            SpaceStat("关注", profile?.following ?: 0, Modifier.weight(1f))
                            SpaceStat("获赞", profile?.likes ?: 0, Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(10.dp))
                        FollowButton(
                            isSelf = isSelf,
                            followed = followed,
                            busy = followBusy,
                            onClick = {
                                if (isSelf) {
                                    context.showToast("编辑资料暂未接入")
                                    return@FollowButton
                                }
                                if (!AppGraph.api.isLoggedIn) {
                                    context.showToast("请先登录")
                                    return@FollowButton
                                }
                                if (followBusy) return@FollowButton
                                val next = !followed
                                followed = next
                                followBusy = true
                                scope.launch {
                                    val result = runCatching {
                                        AppGraph.api.followUser(mid, next)
                                    }
                                    followBusy = false
                                    if (result.isSuccess) {
                                        SpaceCache.map[mid]?.let {
                                            SpaceCache.map[mid] = it.copy(
                                                followed = next,
                                                at = System.currentTimeMillis(),
                                            )
                                        }
                                        context.showToast(
                                            if (next) "关注成功" else "已取消关注",
                                        )
                                    } else {
                                        followed = !next
                                        context.showToast(
                                            result.exceptionOrNull()?.message ?: "操作失败",
                                        )
                                    }
                                }
                            },
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))

            // 昵称 + 大会员 + 等级
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = profile?.name.orEmpty(),
                    color = colors.primary,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (profile?.vip == true) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(colors.primary)
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = profile?.vipLabel.orEmpty().ifBlank { "大会员" },
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Normal,
                            maxLines = 1,
                        )
                    }
                }
                if (profile?.officialTitle.orEmpty().isNotBlank()) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(colors.primary.copy(alpha = 0.12f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = profile?.officialTitle.orEmpty(),
                            color = colors.primary,
                            fontSize = 9.sp,
                            maxLines = 1,
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // 等级 / 经验条
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(
                        id = when ((profile?.level ?: 0).coerceIn(1, 6)) {
                            1 -> R.drawable.bili_lv1
                            2 -> R.drawable.bili_lv2
                            3 -> R.drawable.bili_lv3
                            4 -> R.drawable.bili_lv4
                            5 -> R.drawable.bili_lv5
                            else -> R.drawable.bili_lv6
                        },
                    ),
                    contentDescription = "LV${profile?.level ?: 0}",
                    modifier = Modifier.size(width = 24.dp, height = 11.dp),
                )
                val nextExp = profile?.nextLevelExp ?: 0L
                val expText = if (nextExp > 0) {
                    "${profile?.levelExp ?: 0L}/$nextExp"
                } else {
                    "LV${profile?.level ?: 0}"
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = expText,
                    color = colors.onBackgroundVariant,
                    fontSize = 10.5.sp,
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(colors.surfaceContainerHigh),
                ) {
                    val fraction = if (nextExp > 0) {
                        ((profile?.levelExp ?: 0L).toFloat() / nextExp).coerceIn(0f, 1f)
                    } else {
                        ((profile?.level ?: 0) / 6f).coerceIn(0f, 1f)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(colors.primary),
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // 签名
            Text(
                text = profile?.sign.orEmpty().ifBlank { "这个人很神秘，什么都没有写" },
                color = colors.onBackgroundVariant,
                fontSize = 12.5.sp,
                lineHeight = 18.sp,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            // IP 属地
            if (profile?.ipLocation.orEmpty().isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(colors.onBackground.copy(alpha = 0.06f))
                            .padding(horizontal = 4.dp, vertical = 1.dp),
                    ) {
                        Text(
                            text = "IP",
                            color = colors.onBackgroundVariant,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = "属地：${profile?.ipLocation.orEmpty()}",
                        color = colors.onBackgroundVariant,
                        fontSize = 11.sp,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // 充电
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surface)
                    .padding(horizontal = 16.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "⚡", fontSize = 15.sp)
                Spacer(Modifier.width(8.dp))
                Text(text = "充电", color = colors.onBackground, fontSize = 13.sp)
                Spacer(Modifier.weight(1f))
                Text(
                    text = "${formatCount(((profile?.likes ?: 0) / 4000).coerceAtLeast(0))} 人充电",
                    color = colors.onBackgroundVariant,
                    fontSize = 11.sp,
                )
            }

            Spacer(Modifier.height(14.dp))

            // 自制的 tab 栏：完全复用视频页 TeakPagerTabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                CompositionLocalProvider(LocalTeakBackdrop provides null) {
                    TeakPagerTabs(
                        options = listOf("主页", "动态", "投稿", "收藏", "追番"),
                        selected = currentPage,
                        onSelect = { index ->
                            scope.launch {
                                horizontalScroll.animateScrollTo(
                                    (index.toFloat() * pageWidthPx).roundToInt(),
                                )
                            }
                        },
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .horizontalScroll(horizontalScroll)
                    .width(pageWidth * 5f),
            ) {
                SpacePage(pageWidth) {
                    VideoGridContent(
                        videos = space?.videos.orEmpty(),
                        total = space?.videoCount ?: space?.videos?.size ?: 0,
                        loading = loading,
                        onOpenVideo = { TeakNavigator.openVideo(context, it) },
                    )
                }
                SpacePage(pageWidth) {
                    DynamicListContent(
                        dynamics = dynamics,
                        loading = loading,
                        onOpenDynamic = { TeakNavigator.openDynamic(context, it) },
                    )
                }
                SpacePage(pageWidth) {
                    VideoGridContent(
                        videos = space?.videos.orEmpty(),
                        total = space?.videoCount ?: space?.videos?.size ?: 0,
                        loading = loading,
                        onOpenVideo = { TeakNavigator.openVideo(context, it) },
                    )
                }
                SpacePage(pageWidth) {
                    SpaceEmpty("收藏夹暂未接入")
                }
                SpacePage(pageWidth) {
                    SpaceEmpty("追番暂未接入")
                }
            }

            Spacer(Modifier.height(90.dp))
        }

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                TeakLoading(size = 44.dp)
            }
        }
    }
}

@Composable
private fun BannerCircleButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    desc: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.30f))
            .clickable(interactionSource = null, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = desc,
            tint = Color.White,
            modifier = Modifier.size(17.dp),
        )
    }
}

@Composable
private fun FollowButton(
    isSelf: Boolean,
    followed: Boolean,
    busy: Boolean,
    onClick: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val filled = !isSelf && !followed
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (filled) colors.primary else colors.surface)
            .border(
                1.dp,
                if (filled) Color.Transparent else colors.primary.copy(alpha = 0.45f),
                RoundedCornerShape(10.dp),
            )
            .clickable(interactionSource = null, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = when {
                isSelf -> "编辑资料"
                busy -> "处理中…"
                followed -> "已关注"
                else -> "+ 关注"
            },
            color = if (filled) Color.White else colors.primary,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Normal,
        )
    }
}

@Composable
private fun SpaceStat(label: String, value: Long, modifier: Modifier = Modifier) {
    val colors = MiuixTheme.colorScheme
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = formatCount(value),
            color = colors.onBackground,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
        Spacer(Modifier.height(2.dp))
        Text(text = label, color = colors.onBackgroundVariant, fontSize = 10.5.sp)
    }
}

@Composable
private fun SpaceVideoCard(video: VideoItem, onClick: () -> Unit) {
    val colors = MiuixTheme.colorScheme
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ContinuousCornerShape(14.dp))
            .background(colors.surface)
            .clickable(interactionSource = null, indication = null, onClick = onClick)
            .padding(6.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(ContinuousCornerShape(10.dp))
                .background(colors.surfaceContainerHigh),
        ) {
            AsyncImage(
                model = rememberImageRequest(video.cover, crossfade = false),
                contentDescription = video.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(36.dp)
                    .background(
                        Brush.verticalGradient(listOf(Color.Transparent, Color(0x55000000))),
                    ),
            )
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (video.duration > 0) {
                    Text(
                        text = com.aritxonly.bdfun.ui.formatDuration(video.duration),
                        color = Color.White,
                        fontSize = 9.5.sp,
                    )
                    Spacer(Modifier.width(6.dp))
                }
                ResourceGlyph(res = TeakIcon.MetricPlay, tint = Color.White, size = 11.dp)
                Spacer(Modifier.width(2.dp))
                Text(
                    text = formatCount(video.play),
                    color = Color.White,
                    fontSize = 9.5.sp,
                )
                Spacer(Modifier.width(6.dp))
                ResourceGlyph(res = TeakIcon.MetricDanmaku, tint = Color.White, size = 11.dp)
                Spacer(Modifier.width(2.dp))
                Text(
                    text = formatCount(video.danmaku.toLong()),
                    color = Color.White,
                    fontSize = 9.5.sp,
                )
            }
        }
        Text(
            text = video.title,
            color = colors.onBackground,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun SpacePage(width: Dp, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.width(width), content = content)
}

@Composable
private fun VideoGridContent(
    videos: List<VideoItem>,
    total: Int,
    loading: Boolean,
    onOpenVideo: (VideoItem) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "视频",
                color = MiuixTheme.colorScheme.onBackground,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = formatCount(total.toLong()),
                color = MiuixTheme.colorScheme.onBackgroundVariant,
                fontSize = 11.sp,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "查看更多 ＞",
                color = MiuixTheme.colorScheme.onBackgroundVariant,
                fontSize = 11.sp,
            )
        }
        videos.chunked(2).forEach { rowVideos ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                rowVideos.forEach { video ->
                    Box(modifier = Modifier.weight(1f)) {
                        SpaceVideoCard(
                            video = video,
                            onClick = { onOpenVideo(video) },
                        )
                    }
                }
                if (rowVideos.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        if (videos.isEmpty() && !loading) {
            SpaceEmpty("还没有投稿视频")
        }
    }
}

@Composable
private fun DynamicListContent(
    dynamics: List<DynamicItem>,
    loading: Boolean,
    onOpenDynamic: (DynamicItem) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "动态",
                color = MiuixTheme.colorScheme.onBackground,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "查看更多 ＞",
                color = MiuixTheme.colorScheme.onBackgroundVariant,
                fontSize = 11.sp,
            )
        }
        dynamics.forEach { item ->
            SpaceDynamicRow(item, onOpenDynamic)
        }
        if (dynamics.isEmpty() && !loading) {
            SpaceEmpty("还没有发布过动态")
        }
    }
}

@Composable
private fun SpaceDynamicRow(item: DynamicItem, onOpen: (DynamicItem) -> Unit) {
    val colors = MiuixTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(ContinuousCornerShape(14.dp))
            .background(colors.surface)
            .clickable(interactionSource = null, indication = null) { onOpen(item) }
            .padding(12.dp),
    ) {
        Text(text = item.time, color = colors.onBackgroundVariant, fontSize = 10.5.sp)
        if (item.title.isNotBlank()) {
            Spacer(Modifier.height(5.dp))
            Text(
                text = item.title,
                color = colors.onBackground,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (item.text.isNotBlank()) {
            Spacer(Modifier.height(5.dp))
            Text(
                text = item.text,
                color = colors.onBackground,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
        }
        item.video?.let { video ->
            Spacer(Modifier.height(6.dp))
            Text(
                text = "▶ ${video.title}",
                color = colors.primary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SpaceEmpty(text: String) {
    val colors = MiuixTheme.colorScheme
    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
        Text(text = text, color = colors.onBackgroundVariant, fontSize = 12.5.sp)
    }
}
