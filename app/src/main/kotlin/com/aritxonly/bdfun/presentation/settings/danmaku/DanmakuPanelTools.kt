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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.feature.settings.application.*

@Composable
internal fun DanmakuPanelTools(runtime: DanmakuPanelRuntime) {
    val settings = runtime.settings
    val onInteract = runtime.onInteract
    val danmakuItems = runtime.items
    DmGroup(title = "更多工具") {
        DmSwitchRow(
            title = "弹幕观看屏蔽词",
            description = "仅对我不展示特定弹幕",
            checked = settings.danmakuKeywordFilter,
            onInteract = onInteract,
        ) { settings.danmakuKeywordFilter = it }

        DmActionRow(
            title = "添加屏蔽词",
            onClick = {
                onInteract()
                runtime.controller.toggleKeywords()
            },
            trailing = if (settings.danmakuKeywords.isEmpty()) "" else "${settings.danmakuKeywords.size} 个",
        )
        if (runtime.ui.showKeywords) DanmakuKeywordEditor(runtime)

        if (!runtime.liveMode) {
            DmActionRow(
                title = "当前弹幕列表",
                trailing = "${danmakuItems.size}",
                onClick = {
                    onInteract()
                    runtime.controller.toggleList()
                },
            )
            if (runtime.ui.showList) DanmakuCurrentList(runtime)
        }
    }
}

@Composable
internal fun DanmakuKeywordEditor(runtime: DanmakuPanelRuntime) {
    val settings = runtime.settings
    val palette = LocalDanmakuPanelPalette.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value = runtime.ui.keyword,
            onValueChange = { runtime.controller.changeKeyword(it) },
            singleLine = true,
            textStyle = TextStyle(color = DmText, fontSize = 12.sp),
            cursorBrush = SolidColor(DmPink),
            modifier = Modifier
                .weight(1f)
                .height(34.dp)
                .background(palette.chipActive, RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            decorationBox = { inner ->
                Box {
                    if (runtime.ui.keyword.isEmpty()) {
                        Text(
                            "输入要屏蔽的词",
                            color = palette.muted,
                            fontSize = 12.sp,
                        )
                    }
                    inner()
                }
            },
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "添加",
            color = DmPink,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.clickable(
                interactionSource = null,
                indication = null,
            ) {
                runtime.controller.addKeyword()
            },
        )
    }
    if (settings.danmakuKeywords.isNotEmpty()) {
        Text(
            text = settings.danmakuKeywords.joinToString(" · "),
            color = palette.muted,
            fontSize = 11.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp)
                .clickable(interactionSource = null, indication = null) {
                    runtime.controller.clearKeywords()
                },
        )
    }
}

@Composable
internal fun DanmakuCurrentList(runtime: DanmakuPanelRuntime) {
    val danmakuItems = runtime.items
    val palette = LocalDanmakuPanelPalette.current
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
        danmakuItems.take(60).forEach { item ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                Text(
                    text = formatPlayTime(item.timeMs),
                    color = palette.muted,
                    fontSize = 10.sp,
                    modifier = Modifier.width(48.dp),
                )
                Text(
                    text = item.text,
                    color = palette.text.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
