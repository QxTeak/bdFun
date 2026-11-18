package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.runtime.getValue

@Composable
internal fun ActionStat(
    modifier: Modifier = Modifier,
    label: String,
    value: Long,
    kind: ActionKind,
    iconRes: Int, accentColor: Color, activity: android.app.Activity?,
    active: Boolean = false,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)? = null,
) {
    val colors = MiuixTheme.colorScheme
    val state = remember { VideoActionPressStateHolder() }
    VideoActionPressDecay(state, kind)
    val colorTarget = if (active || state.isPressing || state.press > 0.01f) accentColor else colors.onBackgroundVariant
    val animatedTint by androidx.compose.animation.animateColorAsState(
        targetValue = colorTarget,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 240,
            easing = androidx.compose.animation.core.FastOutSlowInEasing,
        ),
        label = "action-tint",
    )
    VideoActionLongPress(state, onLongPress)
    VideoActionVisual(modifier, label, value, iconRes, onClick, onLongPress != null, state, animatedTint, activity)
}

internal enum class ActionKind { ThumbUp, ThumbDown, Coin, Star, Share }

@Composable
internal fun MiniStat(text: String, playIcon: Boolean) {
    val colors = MiuixTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(12.dp)) {
            if (playIcon) {
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(size.width * 0.3f, size.height * 0.12f)
                    lineTo(size.width * 0.3f, size.height * 0.88f)
                    lineTo(size.width * 0.9f, size.height * 0.5f)
                    close()
                }
                drawPath(path, colors.onBackgroundVariant)
            } else {
                drawRoundRect(
                    color = colors.onBackgroundVariant,
                    topLeft = androidx.compose.ui.geometry.Offset(0f, size.height * 0.15f),
                    size = androidx.compose.ui.geometry.Size(size.width, size.height * 0.5f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height * 0.15f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = size.width * 0.1f),
                )
            }
        }
        Spacer(Modifier.width(4.dp))
        Text(text = text, color = colors.onBackgroundVariant, fontSize = 12.sp)
    }
}
