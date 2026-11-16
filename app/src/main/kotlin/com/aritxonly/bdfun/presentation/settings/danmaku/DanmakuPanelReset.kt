package com.aritxonly.bdfun.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.feature.settings.application.*
import com.aritxonly.teak.common.icons.AppGlyph
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.*
import top.yukonga.miuix.kmp.icon.extended.*

@Composable
internal fun DanmakuPanelReset(runtime: DanmakuPanelRuntime) {
    val onInteract = runtime.onInteract
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .clickable(interactionSource = null, indication = null) {
                    onInteract()
                    runtime.controller.reset()
                }
                .padding(vertical = 6.dp, horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ResetGlyph()
            Spacer(Modifier.width(5.dp))
            Text(text = "重置弹幕设置", color = DmSecondary, fontSize = 11.5.sp)
        }
    }
}

@Composable
internal fun ResetGlyph() {
    AppGlyph(MiuixIcons.Reset, DmSecondary, size = 13.dp)
}
