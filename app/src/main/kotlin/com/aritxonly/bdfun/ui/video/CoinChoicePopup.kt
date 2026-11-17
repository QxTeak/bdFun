package com.aritxonly.bdfun.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider

/**
 * 投币数量浮层：两枚硬币卡并排，点卡片直接投币。
 *
 * 视觉照搬参考实现（PiliPlus / 同源的 BiliPai CoinDialog 的 CoinChoice）：
 * 棕橙圆角卡 + 四角螺钉 + 「币」字硬币 + 白色「N 硬币」标签，
 * 不再自己另画一套渐变。描述见 AGENTS.md 本轮检查点。
 */
@Composable
internal fun CoinChoicePopup(
    visible: Boolean,
    onDismiss: () -> Unit,
    onPick: (Int) -> Unit,
) {
    if (!visible) return
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnClickOutside = true,
        ),
    ) {
        val view = LocalView.current
        SideEffect {
            val window = (view.parent as? DialogWindowProvider)?.window
            window?.setDimAmount(0f)
            window?.setBackgroundDrawable(
                android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { onDismiss() },
            contentAlignment = Alignment.Center,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CoinChoice(count = 1, onPick = onPick)
                CoinChoice(count = 2, onPick = onPick)
            }
        }
    }
}

@Composable
private fun CoinChoice(count: Int, onPick: (Int) -> Unit) {
    Box(
        modifier = Modifier
            .width(96.dp)
            .aspectRatio(1f)
            .semantics { contentDescription = "$count 硬币" }
            .clickable(role = Role.Button) { onPick(count) }
            .background(Color(0xFFC48A52), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawRoundRect(
                Color(0xFFE7B781),
                cornerRadius = CornerRadius(12.dp.toPx()),
                style = Stroke(3.dp.toPx()),
            )
            val inset = 12.dp.toPx()
            listOf(
                Offset(inset, inset),
                Offset(size.width - inset, inset),
                Offset(inset, size.height - inset),
                Offset(size.width - inset, size.height - inset),
            ).forEach { drawCircle(Color(0xFF98603D), 4.dp.toPx(), it) }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    if (count == 2) {
                        drawOval(
                            color = Color(0xFFB5B9BC),
                            topLeft = Offset(0f, size.height * 0.3f),
                            size = Size(size.width * 0.65f, size.height * 0.65f),
                        )
                    }
                    drawCircle(Color(0xFFE4E7E9), size.minDimension * 0.36f)
                    drawCircle(Color(0xFF8C9398), size.minDimension * 0.36f, style = Stroke(2.dp.toPx()))
                    drawCircle(Color(0xFFBBC1C5), size.minDimension * 0.28f, style = Stroke(1.dp.toPx()))
                }
                Text(
                    text = "币",
                    color = Color(0xFF848B90),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "$count 硬币",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
            )
        }
    }
}
