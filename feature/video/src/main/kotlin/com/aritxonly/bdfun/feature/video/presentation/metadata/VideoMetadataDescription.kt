package com.aritxonly.bdfun.ui

import com.aritxonly.teak.common.icons.AppGlyph
import com.aritxonly.teak.common.theme.LocalTeakContentColors
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.text.style.TextOverflow
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
        // 长按直接整段复制 BV 号；不用 detectTapGestures：Text 的链接/选择手势会先吃掉长按。
        // combinedClickable 自带 500ms 长按判定，并把热区补到可点的 32dp 高。
        Text(
            text = bvid,
            color = colors.onBackgroundVariant,
            fontSize = 12.sp,
            modifier = Modifier
                .clip(Capsule())
                .combinedClickable(
                    interactionSource = null,
                    indication = null,
                    onClick = {},
                    onLongClick = { slots.copyText(bvid) },
                )
                .padding(horizontal = 2.dp, vertical = 6.dp),
        )
        Spacer(Modifier.weight(1f))
        if (description.expandable) {
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
    tags: List<com.aritxonly.bdfun.core.model.video.VideoTag> = emptyList(),
) {
    val colors = MiuixTheme.colorScheme
    val tagLabels = tags.map { tag ->
        when (tag.type) {
            "bgm" -> "♫ ${tag.name.removePrefix("发现")}"
            "topic" -> "#${tag.name}"
            else -> tag.name
        }
    }
    if (tagLabels.isNotEmpty()) {
        // 有标签时必须有展开入口，即使简介本身只有一行、甚至没有简介。
        androidx.compose.runtime.LaunchedEffect(tagLabels.size) { description.expandable = true }
    }
    if (descText != null || tagLabels.isNotEmpty()) {
        Spacer(Modifier.height(4.dp))
        // 简介正文带 LinkAnnotation，普通点击会被 Text 内部链接处理先吃掉。
        // 用 Initial 阶段的指针监听自己判定“点按/长按”，并在抬手时消费事件，
        // 从根上保证左半部分文字也能展开；滑动超过阈值就完全放行给列表滚动。
        // Column 而不是 Box：标签必须排在简介正文下面，不能和正文叠在同一层。
        androidx.compose.foundation.layout.Column(
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
                        var copied = false
                        while (true) {
                            val event = awaitPointerEvent(
                                androidx.compose.ui.input.pointer.PointerEventPass.Initial,
                            )
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                if (!moved && !copied) {
                                    change.consume()
                                    description.toggle()
                                }
                                break
                            }
                            if ((change.position - start).getDistance() > viewConfiguration.touchSlop) {
                                moved = true
                            }
                            // 按住到 500ms 就复制，不再等抬手：折叠态只有两行高，
                            // 等抬手期间很容易被文本自身的选择/链接手势打断。
                            if (!moved && !copied && descText != null &&
                                System.currentTimeMillis() - downAt >= 500L
                            ) {
                                copied = true
                                change.consume()
                                slots.copyDescription(descText)
                            }
                        }
                    }
                },
        ) {
            // 标签作为简介的尾部内容传进去，和正文共用同一条高度动画。
            // 收起时一起裁掉，展开时一起长出来，不会再单独 fade/撑开一次。
            CollapsibleDescription(
                text = descText,
                expanded = description.expanded,
                color = colors.onBackgroundVariant,
                onExpandableChange = { description.expandable = true },
                modifier = Modifier.fillMaxWidth().padding(start = 1.dp),
                slots = slots,
                trailing = if (tagLabels.isNotEmpty()) {
                    { VideoTagFlow(tagLabels) }
                } else null,
            )
        }
    }
}

/** 视频标签：32dp 高灰色胶囊，和简介正文同属一个展开块。 */
@Composable
private fun VideoTagFlow(labels: List<String>) {
    val colors = MiuixTheme.colorScheme
    androidx.compose.foundation.layout.FlowRow(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
    ) {
        labels.forEach { label ->
            Box(
                modifier = Modifier
                    .height(32.dp)
                    .clip(Capsule())
                    .background(colors.surfaceContainerHigh)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    color = colors.onBackgroundVariant,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
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
    val showTname = detail?.tname !in pgcTypeLabels && !detail?.tname.isNullOrBlank()
    // 标签和直播分区/搜索历史同一套灰色胶囊：32dp 高、全圆角、16dp 横向内边距。
    val tags = content.tags.map { tag ->
        when (tag.type) {
            "bgm" -> "♫ ${tag.name.removePrefix("发现")}"
            "topic" -> "#${tag.name}"
            else -> tag.name
        }
    }.ifEmpty { if (showTname) listOf(detail?.tname.orEmpty()) else emptyList() }
    if (tags.isEmpty()) return
    Spacer(Modifier.height(12.dp))
    androidx.compose.foundation.layout.FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
    ) {
        tags.forEach { label ->
            Box(
                modifier = Modifier
                    .height(32.dp)
                    .clip(Capsule())
                    .background(colors.surfaceContainerHigh)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    color = colors.onBackgroundVariant,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
