/*
 * AboutAppHeader layout adapted from Deadliner (MIT), Copyright (c) 2024 Aritx Zhou.
 * Source revision and complete license: docs/SETTINGS_DEADLINER_PROVENANCE.md.
 */
package com.aritxonly.bdfun.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.BuildConfig
import com.aritxonly.bdfun.R
import com.aritxonly.bdfun.feature.settings.presentation.LocalSettingsBindings
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 保留 Deadliner 252dp 身份区、96dp 图标和版本层级；产品信息由 bdFun 提供。 */
@Composable
internal fun SettingsAboutAppHeader(appName: String, onCheckUpdate: () -> Unit) {
    // 关于页使用透明 PNG 品牌图，不转成 JPG，也不裁成圆角。
    val logo = painterResource(R.drawable.bdfun_about)
    // 和背景共用同一套调色板与时间源：描边颜色逐帧跟光晕一致。
    val animationsEnabled = LocalSettingsBindings.current.animationsEnabled
    val time = rememberAboutAnimationTime(animationsEnabled)
    val dark = MiuixTheme.colorScheme.background.luminance() < 0.5f
    // 描边每帧都要画一次离屏层，Paint 只建一次。
    val outlinePaint = androidx.compose.runtime.remember { Paint() }
    val float = rememberInfiniteTransition(label = "about-logo-float")
    val offsetY by float.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = com.aritxonly.teak.common.search.SineEaseInOut),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "about-logo-offset",
    )
    Column(Modifier.fillMaxWidth().heightIn(min = 252.dp)
        .clickable(role = Role.Button, onClickLabel = "检查更新", onClick = onCheckUpdate)
        .padding(top = 28.dp, bottom = 44.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Image(
            painter = logo,
            contentDescription = appName,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .width(252.dp)
                .aspectRatio(1578f / 997f)
                // 浮动在前，描边在层内跟着一起动。
                .graphicsLayer { translationY = offsetY.dp.toPx() }
                // 一圈 1dp 的流光描边：颜色取关于页背景当前时刻的调色板，并绕 logo 慢慢转。
                .drawWithContent {
                    drawFlowingOutline(logo, time.floatValue, dark, outlinePaint)
                    drawContent()
                },
        )
        // 品牌图已自带 bdFun 字样，不再重复显示文字标题。
        Text("v${BuildConfig.VERSION_NAME}", color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            fontSize = 14.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
        Spacer(Modifier.height(12.dp))
    }
}

/**
 * 用 logo 自身轮廓做遮罩，把“当前时刻背景的调色板”铺成一圈 1dp 描边。
 *
 * 先在一层离屏里把 8 份 1dp 偏移的 logo 轮廓画成并集，再用 `SrcIn` 把渐变
 * 只留在轮廓区域；原图随后盖回中间部分，剩下的就是外圈流光。
 */
private fun DrawScope.drawFlowingOutline(logo: Painter, time: Float, dark: Boolean, layerPaint: Paint) {
    val palette = animatedAboutColors(time, dark)
    if (palette.isEmpty()) return
    val colors = outlineColors(palette, dark)
    val edge = 1.dp.toPx()
    val center = Offset(size.width / 2f, size.height / 2f)
    // 离屏层必须比图片四周多留出偏移量：只按图片尺寸开层，会把这圈
    // “画在图片外面”的描边裁掉，看起来就像完全没有描边。
    val pad = edge * 3f
    drawIntoCanvas { canvas ->
        canvas.saveLayer(
            Rect(Offset(-pad, -pad), Size(size.width + pad * 2f, size.height + pad * 2f)),
            layerPaint,
        )
        RingDirections.forEach { (ux, uy) ->
            translate(ux * edge, uy * edge) { with(logo) { draw(size) } }
        }
        canvas.save()
        // 70°/s 绕中心转，约 5 秒一圈，流光方向变化能看出来。
        canvas.translate(center.x, center.y)
        canvas.rotate((time * 70f) % 360f)
        canvas.translate(-center.x, -center.y)
        drawRect(
            brush = Brush.sweepGradient(colors + colors.first(), center),
            topLeft = Offset(-size.width, -size.height),
            size = Size(size.width * 3f, size.height * 3f),
            blendMode = BlendMode.SrcIn,
        )
        canvas.restore()
        canvas.restore()
    }
}

/** 8 个方向的单位偏移；实际距离乘描边宽度，避免每帧重建列表。 */
private val RingDirections = listOf(
    -1f to 0f, 1f to 0f, 0f to -1f, 0f to 1f,
    -1f to -1f, 1f to -1f, -1f to 1f, 1f to 1f,
)

/**
 * 背景调色板是给大面积光斑用的浅色，1dp 细线直接沿用几乎看不见；
 * 色相保持和背景一致，只把饱和度/明度提到能看清的程度。
 */
private fun outlineColors(palette: List<Color>, dark: Boolean): List<Color> {
    if (dark) return palette.map { it.copy(alpha = 1f) }
    val hsv = FloatArray(3)
    return palette.map { color ->
        android.graphics.Color.colorToHSV(color.toArgb(), hsv)
        hsv[1] = (hsv[1] * 2.2f).coerceIn(0.35f, 0.9f)
        hsv[2] = (hsv[2] * 0.9f).coerceIn(0.55f, 1f)
        Color(android.graphics.Color.HSVToColor(hsv))
    }
}
