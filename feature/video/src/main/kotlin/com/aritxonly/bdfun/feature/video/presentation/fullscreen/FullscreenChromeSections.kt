package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.teak.common.icons.AppGlyph
import com.kyant.shapes.Capsule
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlinx.coroutines.delay
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue


@Composable
internal fun BoxScope.FullscreenChromeMasks(appearance: FullscreenAppearance, portrait: Boolean = false) {
    val edge = if (portrait) 14.dp else 42.dp
    // 顶部 / 底部压暗，保证白字在亮画面上仍可读（参考图一有同款渐层）
    Box(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .fillMaxWidth()
            .height(if (portrait) 260.dp else 190.dp)
            .background(
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.62f),
                    0.5f to Color.Black.copy(alpha = 0.25f),
                    1f to Color.Transparent,
                ),
            ),
    )
    Box(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .height(235.dp)
            .background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.45f to Color.Black.copy(alpha = 0.35f),
                    1f to Color.Black.copy(alpha = 0.82f),
                ),
            ),
    )
    // 横屏时间居中；竖屏没有系统状态栏，把时间放到左上角并给挖孔/前摄留出高度。
    if (appearance.showTime) {
        val topInset = if (portrait) {
            val status = androidx.compose.foundation.layout.WindowInsets.statusBars
                .asPaddingValues().calculateTopPadding()
            val cutout = androidx.compose.foundation.layout.WindowInsets.displayCutout
                .asPaddingValues().calculateTopPadding()
            maxOf(status, cutout, 30.dp)
        } else 0.dp
        FullscreenStatusLine(
            modifier = Modifier
                .align(if (portrait) Alignment.TopStart else Alignment.TopCenter)
                .fillMaxWidth()
                .padding(start = if (portrait) 18.dp else edge, top = if (portrait) 2.dp else topInset),
            alignStart = portrait,
        )
    }

}

@Composable
internal fun FullscreenHeader(
    info: FullscreenPlayerInfo, actions: FullscreenActions,
    onPanelChanged: (FullscreenPanel) -> Unit, appearance: FullscreenAppearance,
    portrait: Boolean = false, danmakuEnabled: Boolean = true,
) {
    if (portrait) {
        FullscreenHeaderPortrait(info, actions, onPanelChanged, appearance, danmakuEnabled)
        return
    }
    // 顶部操作行：返回 / 标题 / 点赞 / 评论 / 收藏 / 分享 / 更多
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 36.dp, end = 24.dp, top = 12.dp)
            .height(46.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable(interactionSource = null, indication = null) {
                    actions.onBack()
                },
            contentAlignment = Alignment.Center,
        ) {
            BackChevron()
        }
        Spacer(Modifier.width(3.dp))
        Text(
            text = info.title,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        FullscreenHeaderActions(info, actions, onPanelChanged, appearance)
    }

}

/** 竖屏全屏：标题单独一行，互动与弹幕开关放下一行，顶部避开前摄。 */
@Composable
private fun FullscreenHeaderPortrait(
    info: FullscreenPlayerInfo, actions: FullscreenActions,
    onPanelChanged: (FullscreenPanel) -> Unit, appearance: FullscreenAppearance,
    danmakuEnabled: Boolean,
) {
    val status = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val cutout = WindowInsets.displayCutout.asPaddingValues().calculateTopPadding()
    val topInset = maxOf(status, cutout, 30.dp)
    // 时间留在状态栏区域，标题与互动行紧贴其下，整体尽量上移。
    Column(Modifier.fillMaxWidth().padding(top = topInset + 4.dp, start = 8.dp, end = 10.dp)) {
        Row(Modifier.fillMaxWidth().height(44.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape)
                    .clickable(interactionSource = null, indication = null) { actions.onBack() },
                contentAlignment = Alignment.Center,
            ) { BackChevron() }
            Spacer(Modifier.width(4.dp))
            Text(
                text = info.title, color = Color.White, fontSize = 16.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
            )
        }
        Row(Modifier.fillMaxWidth().height(36.dp), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.weight(1f))
            FullscreenHeaderActions(info, actions, onPanelChanged, appearance)
        }
    }
}

@Composable
internal fun FullscreenHeaderActions(info: FullscreenPlayerInfo, actions: FullscreenActions, onPanelChanged: (FullscreenPanel) -> Unit, appearance: FullscreenAppearance) {
    // 「全屏显示互动按钮」只管点赞 / 评论 / 收藏 / 分享这四个互动项。
    // 「更多」是听视频、稍后再看、小窗、投屏的唯一入口，跟着一起被关掉会把这些功能一起锁死（issue #154）。
    if (appearance.showActions) {
        Spacer(Modifier.width(12.dp))
        BadgeStatAction(
            accentColor = appearance.accentColor,
            icon = appearance.icons.like(info.liked),
            count = info.likeCount,
            active = info.liked,
            yOffset = (-10).dp,
            xOffset = (-7).dp,
            countXOffset = 8.dp,
            countYOffset = 11.dp,
            onClick = actions.onLike,
        )
        Spacer(Modifier.width(16.dp))
        BadgeStatAction(
            accentColor = appearance.accentColor,
            icon = appearance.icons.comment,
            count = info.replyCount,
            xOffset = (-4).dp,
            countXOffset = 6.dp,
            countYOffset = 8.dp,
            onClick = actions.onComment,
        )
        Spacer(Modifier.width(16.dp))
        BadgeStatAction(
            accentColor = appearance.accentColor,
            icon = appearance.icons.favorite,
            count = info.favoriteCount,
            active = info.favorited,
            xOffset = 0.dp,
            countXOffset = 5.dp,
            countYOffset = 8.dp,
            onClick = {
                onPanelChanged(FullscreenPanel.Favorite)
                actions.onRequestFavorites()
            },
        )
        Spacer(Modifier.width(16.dp))
        IconOnlyAction(
            icon = appearance.icons.share,
            xOffset = 1.dp,
            onClick = actions.onShare,
        )
        Spacer(Modifier.width(16.dp))
    }
    Box(
        modifier = Modifier
            .size(width = 30.dp, height = 40.dp)
            .offset(y = (-7).dp)
            .clickable(interactionSource = null, indication = null) {
                onPanelChanged(FullscreenPanel.More)
            },
        contentAlignment = Alignment.BottomStart,
    ) {
        MoreGlyph()
    }
}

@Composable
internal fun BoxScope.FullscreenAuthor(
    info: FullscreenPlayerInfo, actions: FullscreenActions,
    appearance: FullscreenAppearance, slots: FullscreenChromeSlots, portrait: Boolean = false,
) {
    val colors = MiuixTheme.colorScheme
    // 观看人数 + UP 头像 / 昵称；竖屏排在两行标题/操作下面。
    Column(
        modifier = Modifier
            .align(Alignment.TopStart)
            .padding(start = if (portrait) 14.dp else 50.dp, top = if (portrait) 112.dp else 55.dp),
    ) {
        if (appearance.showViewerCount) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.offset(x = 29.dp),
            ) {
                ViewerGlyph()
                Spacer(Modifier.width(4.dp))
                Text(
                    text = viewerText(info),
                    color = Color.White.copy(alpha = 0.86f),
                    fontSize = 11.sp,
                )
            }
            Spacer(Modifier.height(10.dp))
        }
        UpFollowChip(
            authorFace = info.authorFace,
            followed = info.followed,
            avatar = slots.avatar,
        ) { actions.onOpenSpace() }
    }

}

/**
 * 播放器左上角的 UP 头像 + 关注胶囊。互动弹幕里的关注卡片也复用它，
 * 保证两处是完全同款，不各画一套。
 */
@Composable
fun UpFollowChip(
    authorFace: String,
    followed: Boolean,
    avatar: @Composable (String, Modifier) -> Unit,
    onClick: () -> Unit = {},
) {
    val colors = MiuixTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(26.dp)
            .clip(Capsule())
            // 已关注也要保持半透明，不能是一块实色底盖在视频上。
            .background(if (followed) colors.surfaceContainerHigh.copy(alpha = 0.45f) else Color.Black.copy(alpha = 0.3f))
            .clickable(interactionSource = null, indication = null, onClick = onClick),
    ) {
        avatar(
            authorFace,
            Modifier
                .padding(start = 2.dp, top = 2.dp, bottom = 2.dp)
                .size(22.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.12f)),
        )
        Spacer(Modifier.width(7.dp))
        if (!followed) {
            AppGlyph(MiuixIcons.Add, Color.White, size = 11.dp)
            Spacer(Modifier.width(2.dp))
        }
        Text(
            text = if (followed) "已关注" else "关注",
            color = if (followed) colors.onBackgroundVariant else Color.White,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.width(10.dp))
    }
}

@Composable
internal fun BoxScope.FullscreenTimeline(
    playback: FullscreenPlaybackState, actions: FullscreenActions, scrub: FullscreenScrubActions,
    portrait: Boolean = false,
) {
    val edge = if (portrait) 14.dp else 42.dp
    // 进度：时长在上，进度条紧贴在下；竖屏全屏时底部控件整体抬高，进度条再留一点空隙。
    Box(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .padding(start = edge, end = edge, bottom = if (portrait) 82.dp else 52.dp)
            .height(58.dp),
    ) {
        Text(
            text = "${formatPlayTime(playback.positionMs)}/${formatPlayTime(playback.durationMs)}",
            color = Color.White.copy(alpha = 0.95f),
            fontSize = 11.5.sp,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = 10.dp)
                .padding(bottom = 36.dp),
        )
        if (playback.sleepAtMs > System.currentTimeMillis()) {
            val left = (playback.sleepAtMs - System.currentTimeMillis()) / 1000L
            Text(
                text = "定时关闭 ${left / 60}:${"%02d".format(left % 60)}",
                color = Color.White.copy(alpha = 0.78f),
                fontSize = 11.sp,
                modifier = Modifier.align(Alignment.TopEnd),
            )
        }
        FullscreenSeekBar(
            positionMs = playback.positionMs,
            durationMs = playback.durationMs,
            bufferedMs = playback.bufferedMs,
            onInteract = {},
            onSeek = actions.onSeek,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 10.dp),
            externalPressed = playback.scrubbing,
            externalLook = playback.seekLook,
            onScrubStart = scrub.onScrubStart,
            onScrubEnd = scrub.onScrubEnd,
            onLookChanged = scrub.onScrubLook,
            onScrubPreview = scrub.onScrubPreview,
        )
    }

}
