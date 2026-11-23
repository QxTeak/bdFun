package com.aritxonly.bdfun.ui

import com.aritxonly.teak.common.icons.AppGlyph
import com.aritxonly.teak.common.theme.LocalTeakContentColors
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.core.model.video.VideoDetail
import com.aritxonly.bdfun.core.model.video.VideoItem
import com.kyant.shapes.Capsule
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun VideoMetadataIdentifier(
    content: VideoMetadataContent, description: VideoDescriptionStateHolder,
    descText: String?, descProgress: Float, slots: VideoMetadataSlots,
) {
    val colors = MiuixTheme.colorScheme
    val detail = content.detail
    val video = content.video
    val bvid = detail?.bvid ?: video.bvid
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 长按直接整段复制 BV 号；不再使用 SelectionContainer，避免先弹文字选择手柄再点复制。
        Text(
            text = bvid,
            color = colors.onBackgroundVariant,
            fontSize = 12.sp,
            modifier = Modifier.pointerInput(bvid) {
                detectTapGestures(onLongPress = { slots.copyText(bvid) })
            },
        )
        Spacer(Modifier.weight(1f))
        if (descText != null && description.expandable) {
            Box(
                modifier = Modifier
                    .size(width = 34.dp, height = 24.dp)
                    .clip(Capsule())
                    .clickable(
                        interactionSource = null,
                        indication = null,
                    ) { description.toggle() },
                contentAlignment = Alignment.Center,
            ) {
                // 换成 Miuix 自带的箭头（向右的 ChevronForward 转 90° 就是向下），
                // 展开时再转 180°；外面 34×24 的点击区域不变，图标本体和原来的小三角差不多大。
                AppGlyph(
                    MiuixIcons.ChevronForward,
                    tint = colors.onBackgroundVariant,
                    modifier = Modifier.graphicsLayer { rotationZ = 90f + 180f * descProgress },
                    size = 15.dp,
                )
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
internal fun VideoMetadataDescription(
    descText: String?, description: VideoDescriptionStateHolder, slots: VideoMetadataSlots,
) {
    val colors = MiuixTheme.colorScheme
    if (descText != null) {
        Spacer(Modifier.height(4.dp))
        // 简介正文带 LinkAnnotation，普通点击会被 Text 内部链接处理先吃掉。
        // 用 Initial 阶段的指针监听自己判定“点按/长按”，并在抬手时消费事件，
        // 从根上保证左半部分文字也能展开；滑动超过阈值就完全放行给列表滚动。
        Box(
            Modifier
                .fillMaxWidth()
                .pointerInput(descText, description.expandable) {
                    awaitEachGesture {
                        val down = awaitFirstDown(
                            requireUnconsumed = false,
                            pass = androidx.compose.ui.input.pointer.PointerEventPass.Initial,
                        )
                        val start = down.position
                        val downAt = System.currentTimeMillis()
                        var moved = false
                        while (true) {
                            val event = awaitPointerEvent(
                                androidx.compose.ui.input.pointer.PointerEventPass.Initial,
                            )
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                if (!moved) {
                                    change.consume()
                                    val elapsed = System.currentTimeMillis() - downAt
                                    if (elapsed >= 500L) slots.copyDescription(descText)
                                    else description.toggle()
                                }
                                break
                            }
                            if ((change.position - start).getDistance() > viewConfiguration.touchSlop) {
                                moved = true
                            }
                        }
                    }
                },
        ) {
            CollapsibleDescription(
                text = descText,
                expanded = description.expanded,
                color = colors.onBackgroundVariant,
                onExpandableChange = { description.expandable = true },
                modifier = Modifier.fillMaxWidth().padding(start = 1.dp),
                slots = slots,
            )
        }
    }
}

/** 番剧/影视内容类型标签；普通稿件不会有这些分区名。 */
private val pgcTypeLabels = setOf("番剧", "电影", "纪录片", "国创", "电视剧", "综艺")

@Composable
internal fun VideoMetadataCategory(content: VideoMetadataContent) {
    val colors = MiuixTheme.colorScheme
    val detail = content.detail
    // 番剧/影视的 tname 就是内容类型（"番剧"/"电影"……），这不是分区，
    // 挂在简介下面只会多一个看不懂的小标签，所以这些内容不显示。
    if (detail?.tname in pgcTypeLabels) return
    if (!detail?.tname.isNullOrBlank()) {
        Spacer(Modifier.height(12.dp))
        Row {
            Box(
                modifier = Modifier
                    .clip(Capsule())
                    .background(colors.surfaceContainerHigh)
                    .padding(horizontal = 14.dp, vertical = 7.dp),
            ) {
                Text(
                    text = detail.tname,
                    color = colors.onBackgroundVariant,
                    fontSize = 12.sp,
                )
            }
        }
    }
}
