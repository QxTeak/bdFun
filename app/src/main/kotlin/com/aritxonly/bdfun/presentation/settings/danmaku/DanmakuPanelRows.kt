package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.BrandPink
import com.aritxonly.bdfun.feature.settings.application.*
import com.aritxonly.teak.common.icons.AppGlyph
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.*
import top.yukonga.miuix.kmp.icon.extended.*

internal val DmText = Color.White
internal val DmSecondary = Color(0xFFB9BDC6)
internal val DmPink = BrandPink

@Composable
internal fun DmGroup(
    title: String,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            color = Color.White.copy(alpha = 0.92f),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.055f), RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            content = content,
        )
    }
}

@Composable
internal fun DmSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onInteract: () -> Unit,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = null, indication = null) {
                onInteract()
                onChange(!checked)
            }
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = DmText, fontSize = 14.sp)
            if (description.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(text = description, color = Color.White.copy(alpha = 0.45f), fontSize = 11.sp)
            }
        }
        Spacer(Modifier.width(10.dp))
        DmSwitch(checked = checked)
    }
}

@Composable
internal fun DmSwitch(checked: Boolean) {
    Box(
        modifier = Modifier
            .width(40.dp)
            .height(22.dp)
            .background(
                if (checked) DmPink else Color.White.copy(alpha = 0.16f),
                RoundedCornerShape(11.dp),
            ),
    ) {
        Box(
            modifier = Modifier
                .padding(start = if (checked) 20.dp else 2.dp, top = 2.dp)
                .size(18.dp)
                .background(Color.White, RoundedCornerShape(9.dp)),
        )
    }
}

@Composable
internal fun DmActionRow(
    title: String,
    trailing: String = "",
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = null, indication = null, onClick = onClick)
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, color = DmText, fontSize = 14.sp)
        Spacer(Modifier.weight(1f))
        if (trailing.isNotBlank()) {
            Text(text = trailing, color = DmSecondary, fontSize = 11.5.sp)
            Spacer(Modifier.width(6.dp))
        }
        AppGlyph(MiuixIcons.Basic.ArrowRight, size = 16.dp, tint = Color.White.copy(alpha = 0.4f))
    }
}

internal fun speedWord(speed: Float): String = when {
    speed < 0.75f -> "极慢"
    speed < 0.95f -> "慢"
    speed <= 1.05f -> "正常"
    speed < 1.5f -> "快"
    else -> "极快"
}
