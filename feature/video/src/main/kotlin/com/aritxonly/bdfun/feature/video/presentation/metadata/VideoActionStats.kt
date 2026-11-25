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
