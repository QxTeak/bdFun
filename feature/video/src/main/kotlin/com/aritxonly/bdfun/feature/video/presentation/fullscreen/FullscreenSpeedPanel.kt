package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 选中框的固定高度：选项再多再少，粉色框都是这个尺寸。 */
internal val OptionRowHeight = 40.dp

@Composable
internal fun SpeedPanel(
    playback: FullscreenPlaybackState,
    actions: FullscreenActions,
    onClose: () -> Unit,
    accentColor: Color,
) {
    PanelCard(title = "倍速") {
        Column(
            modifier = Modifier.fillMaxSize(),
            // 选项均分剩余高度，但每行自身保持固定高度：选中框不会跟着间距一起长高。
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            FullscreenSpeeds.forEach { speed ->
                val active = kotlin.math.abs(speed - playback.speed) < 0.001f
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(OptionRowHeight)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (active) accentColor.copy(alpha = 0.18f) else Color.Transparent,
                        )
                        .clickable(interactionSource = null, indication = null) {
                            actions.onSpeedSelected(speed)
                            onClose()
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${trimSpeed(speed)}x",
                        color = if (active) accentColor else Color.White,
                        fontSize = 16.sp,
                        fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
                    )
                    Spacer(Modifier.weight(1f))
                    if (active) CheckGlyph(accentColor)
                }
            }
        }
    }
}
