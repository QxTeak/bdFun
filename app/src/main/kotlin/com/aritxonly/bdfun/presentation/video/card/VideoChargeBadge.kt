package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.shapes.Capsule

/** 视频封面右上角的「充电」小胶囊：所有列表卡共用同一个位置与配色。 */
@Composable
internal fun BoxScope.VideoChargeBadge(isCharge: Boolean) {
    if (!isCharge) return
    Box(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(top = 6.dp, end = 6.dp)
            .clip(Capsule())
            .background(ChargeBadgeYellow)
            .padding(horizontal = 6.dp, vertical = 1.5.dp),
    ) {
        Text(
            text = "充电",
            color = Color(0xFF3D2E00),
            fontSize = 9.sp,
            lineHeight = 10.sp,
        )
    }
}

/** 与动态页保持同一块黄色，带一点透明。 */
internal val ChargeBadgeYellow = Color(0xFFFFC107).copy(alpha = 0.92f)
