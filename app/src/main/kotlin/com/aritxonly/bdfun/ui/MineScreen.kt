package com.aritxonly.bdfun.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aritxonly.bdfun.AppGraph
import com.aritxonly.bdfun.data.UserProfile
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val MineBlue = Color(0xFF3C9BD9)

/** 「我的」页资料的进程内缓存：切换 tab 回来不再重新请求。 */
private object MineProfileCache {
    const val TTL = 5 * 60 * 1000L
    var mid: Long = 0L
    var data: UserProfile? = null
    var at: Long = 0L
}

@Composable
fun MineScreen(
    profile: UserProfile?,
    onLogin: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenHistory: () -> Unit = {},
    onOpenWatchLater: () -> Unit = {},
    onOpenCache: () -> Unit = {},
    onOpenMessages: () -> Unit,
    onLogout: () -> Unit,
    showSentHistory: Boolean = true,
    onOpenSentHistory: () -> Unit = {},
) {
    val colors = MiuixTheme.colorScheme
    val ctx = LocalContext.current
    val cardShape = remember { ContinuousCornerShape(20.dp) }

    var liveProfile by remember(profile?.mid) { mutableStateOf(profile) }
    LaunchedEffect(profile?.mid) {
        val p = profile ?: return@LaunchedEffect
        // 5 分钟内再次进入「我的」直接用缓存，不再重载数据
        val now = System.currentTimeMillis()
        val cached = MineProfileCache.data
        if (cached != null && MineProfileCache.mid == p.mid &&
            now - MineProfileCache.at < MineProfileCache.TTL
        ) {
            liveProfile = cached
            return@LaunchedEffect
        }
        val nav = runCatching { AppGraph.api.nav() }.getOrNull()
        val rel = runCatching { AppGraph.api.relationStat(p.mid) }.getOrNull()
        val likes = runCatching { AppGraph.api.userLikes(p.mid) }.getOrDefault(p.likes)
        val merged = (nav ?: p).copy(
            following = rel?.first ?: p.following,
            follower = rel?.second ?: p.follower,
            likes = likes,
        )
        liveProfile = merged
        MineProfileCache.mid = p.mid
        MineProfileCache.data = merged
        MineProfileCache.at = now
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(if (maxWidth >= 600.dp) 2 else 1),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 14.dp,
            top = 28.dp,
            end = 14.dp,
            bottom = 110.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (profile == null) {
            item(key = "login") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(cardShape)
                        .background(colors.surface)
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "尚未登录",
                        color = colors.onBackground,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Normal,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "登录后可同步关注、收藏与历史记录",
                        color = colors.onBackgroundVariant,
                        fontSize = 12.sp,
                    )
                    Spacer(Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(colors.primary)
                            .clickable { onLogin() }
                            .padding(horizontal = 28.dp, vertical = 11.dp),
                    ) {
                        Text(
                            text = "扫码登录",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                        )
                    }
                }
            }
            if (showSentHistory) {
                item(key = "sent-history") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(cardShape)
                            .background(colors.surface),
                    ) {
                        MineRow("我的评论与弹幕", onClick = onOpenSentHistory)
                    }
                }
            }
            item(key = "settings-bar") { SettingsBar(onClick = onOpenSettings) }
            return@LazyVerticalGrid
        }

        val shown = liveProfile ?: profile

        item(key = "header") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(cardShape)
                    .background(colors.surface)
                    .padding(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = rememberImageRequest(shown.face),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .background(colors.surfaceContainerHigh),
                    )
                    Spacer(Modifier.width(12.dp))
                    // 三行整体高度压在头像（58dp）以内：ID / 大会员 / 硬币，首字左对齐
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = shown.name,
                                color = colors.onBackground,
                                fontSize = 17.sp,
                                lineHeight = 20.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            Spacer(Modifier.width(4.dp))
                            Image(
                                painter = painterResource(
                                    id = when (shown.level.coerceIn(1, 6)) {
                                        1 -> com.aritxonly.bdfun.R.drawable.bili_lv1
                                        2 -> com.aritxonly.bdfun.R.drawable.bili_lv2
                                        3 -> com.aritxonly.bdfun.R.drawable.bili_lv3
                                        4 -> com.aritxonly.bdfun.R.drawable.bili_lv4
                                        5 -> com.aritxonly.bdfun.R.drawable.bili_lv5
                                        else -> com.aritxonly.bdfun.R.drawable.bili_lv6
                                    },
                                ),
                                contentDescription = "LV${shown.level}",
                                modifier = Modifier.size(width = 22.dp, height = 10.dp),
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        if (shown.vip) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(com.aritxonly.bdfun.BrandPink)
                                    .padding(horizontal = 5.dp, vertical = 1.dp),
                            ) {
                                Text(
                                    text = shown.vipLabel.ifBlank { "大会员" },
                                    color = Color.White,
                                    fontSize = 8.5.sp,
                                    lineHeight = 11.sp,
                                    fontWeight = FontWeight.Normal,
                                    maxLines = 1,
                                )
                            }
                        } else {
                            Spacer(Modifier.height(14.dp))
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "硬币 ${"%.1f".format(shown.coins)}",
                            color = colors.onBackgroundVariant,
                            fontSize = 11.sp,
                            lineHeight = 13.sp,
                        )
                    }
                    Text(
                        text = "空间 ＞",
                        color = colors.onBackgroundVariant,
                        fontSize = 12.sp,
                        modifier = Modifier.clickable(interactionSource = null, indication = null) {
                            com.aritxonly.bdfun.TeakNavigator.openSpace(
                                ctx,
                                shown.mid,
                            )
                        },
                    )
                }

                Spacer(Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    StatCell("关注", shown.following, Modifier.weight(1f))
                    StatCell("粉丝", shown.follower, Modifier.weight(1f))
                    StatCell("获赞", shown.likes, Modifier.weight(1f))
                }
            }
        }

        item(key = "quick") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(cardShape)
                    .background(colors.surface)
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                QuickAction("离线缓存", Modifier.weight(1f), onClick = onOpenCache) {
                    DownloadGlyph()
                }
                QuickAction("历史记录", Modifier.weight(1f), onClick = onOpenHistory) {
                    ClockGlyph()
                }
                QuickAction("我的收藏", Modifier.weight(1f), onClick = onOpenLibrary) {
                    ResourceGlyph(res = TeakIcon.Star, tint = MineBlue, size = 26.dp)
                }
                QuickAction("稍后再看", Modifier.weight(1f), onClick = onOpenWatchLater) {
                    ResourceGlyph(res = TeakIcon.Play, tint = MineBlue, size = 26.dp)
                }
            }
        }

        if (showSentHistory) {
            item(key = "sent-history") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(cardShape)
                        .background(colors.surface),
                ) {
                    MineRow("我的评论与弹幕", onClick = onOpenSentHistory)
                    // 最近三条评论 / 弹幕
                    val recent = com.aritxonly.bdfun.AppGraph.sentHistory.all().take(3)
                    recent.forEach { record ->
                        val isDanmaku = record.kind == "danmaku"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    interactionSource = null,
                                    indication = null,
                                ) {
                                    if (record.bvid.isNotBlank()) {
                                        com.aritxonly.bdfun.TeakNavigator.openVideo(
                                            ctx,
                                            com.aritxonly.bdfun.data.VideoItem(
                                                bvid = record.bvid,
                                                aid = 0,
                                                title = record.title,
                                                cover = "",
                                                ownerName = "",
                                                ownerFace = "",
                                                ownerMid = 0,
                                                play = 0,
                                                danmaku = 0,
                                                duration = 0,
                                                pubdate = 0,
                                                startPositionMs = record.videoTimeMs,
                                            ),
                                        )
                                    }
                                }
                                .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(
                                        if (isDanmaku) {
                                            MineBlue.copy(alpha = 0.14f)
                                        } else {
                                            colors.primary.copy(alpha = 0.14f)
                                        },
                                    )
                                    .padding(horizontal = 6.dp, vertical = 1.dp),
                            ) {
                                Text(
                                    text = if (isDanmaku) "弹幕" else "评论",
                                    color = if (isDanmaku) MineBlue else colors.primary,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Normal,
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = record.text,
                                color = colors.onBackgroundVariant,
                                fontSize = 11.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            if (isDanmaku) {
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = formatVideoPosition(record.videoTimeMs),
                                    color = colors.onBackgroundVariant.copy(alpha = 0.8f),
                                    fontSize = 10.sp,
                                )
                            }
                        }
                    }
                }
            }
        }

        item(key = "settings-bar") { SettingsBar(onClick = onOpenSettings) }
    }
    }
}

@Composable
private fun StatCell(label: String, value: Long, modifier: Modifier = Modifier) {
    val colors = MiuixTheme.colorScheme
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = formatCount(value),
            color = colors.onBackground,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
        )
        Text(text = label, color = colors.onBackgroundVariant, fontSize = 11.sp)
    }
}

@Composable
private fun QuickAction(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    icon: @Composable () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    Column(
        modifier = modifier.clickable(interactionSource = null, indication = null, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.size(34.dp), contentAlignment = Alignment.Center) { icon() }
        Spacer(Modifier.height(4.dp))
        Text(text = label, color = colors.onBackground, fontSize = 11.5.sp)
    }
}

@Composable
private fun DownloadGlyph() {
    Canvas(modifier = Modifier.size(24.dp)) {
        val w = size.width
        val stroke = w * 0.09f
        drawLine(
            color = MineBlue,
            start = Offset(w * 0.5f, w * 0.12f),
            end = Offset(w * 0.5f, w * 0.62f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = MineBlue,
            start = Offset(w * 0.3f, w * 0.45f),
            end = Offset(w * 0.5f, w * 0.65f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = MineBlue,
            start = Offset(w * 0.7f, w * 0.45f),
            end = Offset(w * 0.5f, w * 0.65f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = MineBlue,
            start = Offset(w * 0.22f, w * 0.84f),
            end = Offset(w * 0.78f, w * 0.84f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun ClockGlyph() {
    Canvas(modifier = Modifier.size(24.dp)) {
        val w = size.width
        val stroke = w * 0.09f
        drawArc(
            color = MineBlue,
            startAngle = -70f,
            sweepAngle = 290f,
            useCenter = false,
            topLeft = Offset(stroke, stroke),
            size = Size(w - stroke * 2, w - stroke * 2),
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
        drawLine(
            color = MineBlue,
            start = Offset(w * 0.5f, w * 0.3f),
            end = Offset(w * 0.5f, w * 0.52f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = MineBlue,
            start = Offset(w * 0.5f, w * 0.52f),
            end = Offset(w * 0.66f, w * 0.6f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun MineRow(
    text: String,
    color: Color? = null,
    onClick: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            color = color ?: colors.onBackground,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f),
        )
        Text(text = "＞", color = colors.outline, fontSize = 12.sp)
    }
}

/** 设置：单独一条，和上面的卡片风格区分开。 */
/** 弹幕在视频内的位置：mm:ss / h:mm:ss。 */
private fun formatVideoPosition(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

@Composable
private fun SettingsBar(onClick: () -> Unit) {
    val colors = MiuixTheme.colorScheme
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // 去掉默认的水波纹
                .clickable(interactionSource = null, indication = null, onClick = onClick)
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "⚙", color = colors.onBackgroundVariant, fontSize = 14.sp)
            Spacer(Modifier.width(6.dp))
            Text(text = "设置", color = colors.onBackgroundVariant, fontSize = 13.sp)
        }
    }
}
