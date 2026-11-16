package com.aritxonly.bdfun.ui

import com.aritxonly.teak.common.icons.ResourceGlyph

import com.aritxonly.teak.common.player.PlayGlyph
import com.aritxonly.teak.common.player.PauseGlyph

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Refresh

@Composable
internal fun LiveChromeRender.LiveChromeBottomControls(modifier: Modifier) {
                Row(
                    modifier = Modifier
                        .then(modifier)
                        .fillMaxWidth()
                        .padding(
                            start = if (landscape) 42.dp else 10.dp,
                            end = if (landscape) 28.dp else 12.dp,
                            bottom = if (landscape) 6.dp else 7.dp,
                        )
                        .height(if (landscape) 42.dp else 36.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LiveChromePrimaryControls()
                    LiveChromeSecondaryControls(this@LiveChromeBottomControls)
                }
}
@Composable
internal fun LiveChromeRender.LiveChromePrimaryControls() {
                    LiveControlButton(
                        size = if (landscape) 40.dp else 28.dp,
                        onClick = {
                            touch()
                            onPlayPause()
                        },
                    ) {
                        if (playing) {
                            PauseGlyph()
                        } else {
                            PlayGlyph(com.aritxonly.bdfun.ui.TeakIcon.PlayerPlay, size = if (landscape) 24.dp else 22.dp)
                        }
                    }
                    Spacer(Modifier.width(if (landscape) 12.dp else 7.dp))

                    // 紧挨播放键：直播重载，竖屏和横屏都在同一位置
                    LiveControlButton(
                        size = 32.dp,
                        onClick = {
                            touch()
                            onReload()
                        },
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Refresh,
                            contentDescription = "直播重载",
                            tint = Color.White,
                            modifier = Modifier.size(21.dp),
                        )
                    }
                    Spacer(Modifier.width(if (landscape) 12.dp else 8.dp))

                    LiveControlButton(
                        size = 32.dp,
                        onClick = {
                            touch()
                            onToggleDanmaku()
                        },
                    ) {
                        // 视频播放器这里用的是原图，不染色；粉色和白色都保留
                        LivePixelIcon(
                            res = if (danmakuEnabled) TeakIcon.PlayerDanmakuOn else TeakIcon.PlayerDanmakuOff,
                            size = 23.dp,
                        )
                    }
                    Spacer(Modifier.width(if (landscape) 8.dp else 4.dp))


}
@Composable
internal fun RowScope.LiveChromeSecondaryControls(render: LiveChromeRender) {
    with(render) {
                    LiveControlButton(
                        size = 32.dp,
                        onClick = {
                            touch()
                            onOpenDanmakuSettings()
                        },
                    ) {
                        LivePixelIcon(res = TeakIcon.PlayerDanmakuSettings, size = 23.dp)
                    }

                    Spacer(Modifier.weight(1f))

                    if (qualityLabel.isNotBlank()) {
                        Text(
                            text = qualityLabel,
                            color = Color.White,
                            fontSize = if (landscape) 14.sp else 12.sp,
                            maxLines = 1,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable(
                                    interactionSource = null,
                                    indication = null,
                                ) {
                                    touch()
                                    onOpenSettings()
                                }
                                .padding(
                                    horizontal = if (landscape) 6.dp else 5.dp,
                                    vertical = if (landscape) 8.dp else 6.dp,
                                ),
                        )
                        Spacer(Modifier.width(if (landscape) 12.dp else 6.dp))
                    }

                    LiveControlButton(
                        size = if (landscape) 32.dp else 29.dp,
                        onClick = {
                            touch()
                            onToggleFullscreen()
                        },
                    ) {
                        ResourceGlyph(
                            res = TeakIcon.Fullscreen,
                            tint = Color.White,
                            size = if (landscape) 20.dp else 17.dp,
                        )
                    }
    }
}
@Composable
internal fun LiveControlButton(
    size: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .clickable(interactionSource = null, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
internal fun LivePixelIcon(
    res: Int,
    size: androidx.compose.ui.unit.Dp,
) {
    Image(
        painter = com.aritxonly.teak.common.icons.rememberAntialiasedIconPainter(painterResource(res)),
        contentDescription = null,
        modifier = Modifier.size(size),
    )
}

/* ------------------------------------------------------------------ */
/* 手势：单击显隐、双击播放/暂停、左竖滑亮度、右竖滑音量                  */
/* ------------------------------------------------------------------ */
