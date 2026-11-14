package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kyant.shapes.Capsule
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 「正在直播」的 UP 头像。
 *
 * 和普通 UP 头像的区别：图片底部压一个「直播中」小胶囊（3dp 白描边，压在粉圈之上），
 * 外面套一圈和动态页"选中 UP"同款的粉圈，粉圈再定时复制一份向外扩散淡出（雷达波）。
 *
 * 扩散用 `delay` 手动推进：本项目基于 frame clock 的动画不会跑。
 */
@Composable
internal fun LiveAvatar(
    name: String,
    face: String,
    onClick: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val pulse = rememberLivePulse()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        // 宽度 = 头像 46dp + 扩散圈余量：扩散到最大时左右两侧不会被列表裁掉。
        modifier = Modifier
            .width(72.dp)
            .clickable(interactionSource = null, indication = null, onClick = onClick),
    ) {
        Box(Modifier.fillMaxWidth().height(72.dp), contentAlignment = Alignment.Center) {
            // 扩散粉圈：比头像大一圈，越大越淡。
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .graphicsLayer {
                        val scale = 1f + 0.55f * pulse
                        scaleX = scale
                        scaleY = scale
                        alpha = (1f - pulse).coerceIn(0f, 1f) * 0.7f
                    }
                    .border(1.dp, colors.primary, CircleShape),
            )
            AsyncImage(
                model = rememberImageRequest(face),
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(colors.surfaceContainerHigh),
            )
            // 静态粉圈：和动态页选中 UP 头像同一套，1dp。
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .border(1.dp, colors.primary, CircleShape),
            )
            // 「直播中」胶囊压在头像底边、粉圈之上。
            // 白边是往外加的一圈：外层白色胶囊 + 内缩 2dp 的粉色胶囊，而不是往胶囊里描边。
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 5.dp)
                    .clip(Capsule())
                    .background(Color.White)
                    .padding(1.5.dp)
                    .clip(Capsule())
                    .background(colors.primary),
            ) {
                Box(Modifier.padding(horizontal = 5.dp)) {
                    Text(
                        text = "直播中",
                        color = Color.White,
                        fontSize = 8.5.sp,
                        lineHeight = 10.sp,
                        maxLines = 1,
                    )
                }
            }
        }
        Text(
            text = name,
            color = colors.onBackgroundVariant,
            fontSize = 9.5.sp,
            lineHeight = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 1.dp),
        )
    }
}

/** 0 → 1 循环推进，一个周期 1.8s；`delay` 驱动，不依赖 frame clock。 */
@Composable
private fun rememberLivePulse(): Float {
    var pulse by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val periodMs = 1800f
        val startNanos = System.nanoTime()
        while (true) {
            pulse = ((System.nanoTime() - startNanos) / 1_000_000f % periodMs) / periodMs
            delay(16)
        }
    }
    return pulse
}
