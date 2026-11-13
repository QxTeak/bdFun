package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.aritxonly.bdfun.feature.sent.application.*

@Composable
internal fun SentHistoryFilters(state: SentHistoryState, onSelect: (Int) -> Unit) {
    val colors = MiuixTheme.colorScheme
    val filter = state.filter
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("全部", "评论", "弹幕").forEachIndexed { index, label ->
            val active = filter == index
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (active) colors.primary else colors.surfaceContainer)
                    .clickable { onSelect(index) }
                    .padding(horizontal = 16.dp, vertical = 7.dp),
            ) {
                Text(
                    text = "$label ${
                        state.count(index)
                    }",
                    color = if (active) Color.White else colors.onBackground,
                    fontSize = 12.sp,
                    fontWeight = if (active) FontWeight.Normal else FontWeight.Normal,
                )
            }
        }
    }
}
