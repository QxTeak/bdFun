package com.aritxonly.bdfun.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.BrandPink
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 个人空间与「我的」页共用的大会员胶囊：粉底白字，非会员为灰色。 */
@Composable
internal fun AccountVipBadge(text: String, active: Boolean = true, modifier: Modifier = Modifier) {
    val colors = MiuixTheme.colorScheme
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(if (active) BrandPink else colors.onBackground.copy(alpha = 0.10f))
            .padding(horizontal = 5.dp, vertical = 1.dp),
    ) {
        Text(
            text = text,
            color = if (active) Color.White else colors.onBackgroundVariant,
            fontSize = 8.5.sp,
            lineHeight = 11.sp,
            maxLines = 1,
        )
    }
}

/**
 * 头像右侧的弧线 + 小圆点点缀。
 *
 * 所有尺寸按头像外径等比缩放，个人空间（96dp）和「我的」页（76dp）共用同一份绘制，
 * 不需要再维护两套 dp 常量。调用方负责头像圆环和头像本体，这里只画点缀。
 */
@Composable
internal fun BoxScope.AvatarAccent(outerSize: Dp) {
    val color = MiuixTheme.colorScheme.primary.copy(alpha = 0.45f)
    Canvas(Modifier.align(Alignment.Center).size(outerSize - 2.dp)) {
        // 弧线在约 35° 处被小圆点断开，两段都用圆角收尾；角度不随尺寸变化。
        val stroke = outerSize.toPx() * 0.03125f
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 115f,
            useCenter = false,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
        drawArc(
            color = color,
            startAngle = 45f,
            sweepAngle = 15f,
            useCenter = false,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
    }
    Box(
        modifier = Modifier
            .align(Alignment.Center)
            .offset(x = outerSize * 0.401f, y = outerSize * 0.281f)
            .size(outerSize * 0.0833f)
            .clip(CircleShape)
            .background(color),
    )
}
