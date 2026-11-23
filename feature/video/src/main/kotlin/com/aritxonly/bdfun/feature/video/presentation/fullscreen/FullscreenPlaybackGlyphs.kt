package com.aritxonly.bdfun.ui

import com.aritxonly.teak.common.player.PixelIcon

import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ChevronBackward
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kyant.shapes.Capsule

@Composable
internal fun NextGlyph(
    res: Int,
    enabled: Boolean,
    /** 上一集复用同一个「下一集」像素图标，仅做水平镜像。 */
    mirror: Boolean = false,
    description: String = "下一集",
) {

    PixelIcon(
        res = res,
        tint = Color.White,
        contentDescription = description,
        modifier = Modifier
            .size(19.dp)
            .graphicsLayer {
                alpha = if (enabled) 1f else 0.28f
                if (mirror) scaleX = -1f
            },
    )
}

@Composable
internal fun DanmakuToggleGlyph(onRes: Int, offRes: Int, enabled: Boolean) {

    PixelIcon(
        res = if (enabled) onRes else offRes,
        contentDescription = if (enabled) "关闭弹幕" else "开启弹幕",
        modifier = Modifier.size(23.dp),
    )
}

@Composable
internal fun SlidersGlyph(res: Int) {

    PixelIcon(
        res = res,
        contentDescription = "弹幕设置",
        modifier = Modifier.size(23.dp),
    )
}
