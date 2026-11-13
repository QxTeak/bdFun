package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ChevronBackward

@Composable
internal fun BoxScope.LiveChromeShade(render: LiveChromeRender) {
    val landscape = render.landscape
    // 底部压暗，保证白字在亮画面上仍然可读
    Box(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .height(if (landscape) 235.dp else 80.dp)
            .background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    (if (landscape) 0.45f else 0.35f) to Color.Black.copy(alpha = 0.35f),
                    1f to Color.Black.copy(alpha = if (landscape) 0.82f else 0.65f),
                ),
            ),
    )

}

@Composable
internal fun BoxScope.LiveChromePortraitHeader(render: LiveChromeRender) {
    with(render) {

                if (!landscape) {
                    // 竖屏顶部只叠标题；返回和主播信息在播放器上方的页面顶栏里
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .height(64.dp)
                            .background(
                                Brush.verticalGradient(
                                    0f to Color.Black.copy(alpha = 0.55f),
                                    0.55f to Color.Black.copy(alpha = 0.22f),
                                    1f to Color.Transparent,
                                ),
                            ),
                    )
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(start = 14.dp, end = 14.dp, top = 10.dp),
                    )
                }

    }
}

@Composable
internal fun BoxScope.LiveChromeLandscapeHeader(render: LiveChromeRender) {
    with(render) {

                if (landscape) {
                    // 横屏没有页面顶栏，把返回和标题叠在播放器上，和视频全屏一致
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .height(190.dp)
                            .background(
                                Brush.verticalGradient(
                                    0f to Color.Black.copy(alpha = 0.62f),
                                    0.5f to Color.Black.copy(alpha = 0.25f),
                                    1f to Color.Transparent,
                                ),
                            ),
                    )
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
                                .clickable(
                                    interactionSource = null,
                                    indication = null,
                                ) {
                                    touch()
                                    onBack()
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = MiuixIcons.ChevronBackward,
                                contentDescription = "返回",
                                tint = Color.White,
                                modifier = Modifier.size(23.5.dp),
                            )
                        }
                        Spacer(Modifier.width(3.dp))
                        Text(
                            text = title,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

    }
}
