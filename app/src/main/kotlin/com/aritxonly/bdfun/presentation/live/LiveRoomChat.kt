package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aritxonly.bdfun.BrandPink
import com.aritxonly.bdfun.core.model.live.LiveChatKind
import com.aritxonly.bdfun.core.model.live.LiveChatMessage
import com.kyant.shapes.Capsule
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun LiveChatList(
    messages: List<LiveChatMessage>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    if (!enabled) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                text = "弹幕已关闭",
                color = Color.White.copy(alpha = 0.35f),
                fontSize = 13.sp,
            )
        }
        return
    }
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }
    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 14.dp,
            end = 14.dp,
            top = 2.dp,
            bottom = 6.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        items(messages, key = { it.id }) { message ->
            LiveChatRow(message)
        }
    }
}


@Composable
internal fun LiveChatRow(message: LiveChatMessage) {
    when (message.kind) {
        LiveChatKind.SYSTEM -> LiveSystemText(message)

        LiveChatKind.GIFT -> Text(
            text = buildString {
                append(message.uname)
                append("：")
                append(message.text)
            },
            color = Color(0xFFFFD36B),
            fontSize = 14.4.sp,
        )

        LiveChatKind.SUPER_CHAT -> Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0x33FF6699))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = message.amountText.orEmpty(),
                color = BrandPink,
                fontSize = 13.2.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = message.uname,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.2.sp,
                )
                Text(
                    text = message.text,
                    color = Color.White,
                    fontSize = 14.4.sp,
                )
            }
        }

        LiveChatKind.DANMAKU -> {
            // 粉丝牌改成和个人主页同款的小胶囊，不再拼成 "[牌子] " 文本前缀。
            val prefix = if (message.uname.isNotBlank()) "${message.uname}：" else ""
            val medal: @Composable () -> Unit = { LiveMedalBadge(message) }
            when {
                // 整条消息就是一张大表情
                message.emoteUrl != null -> Row(verticalAlignment = Alignment.CenterVertically) {
                    medal()
                    Text(prefix, color = Color.White.copy(alpha = 0.92f), fontSize = 14.4.sp)
                    AsyncImage(
                        model = message.emoteUrl,
                        contentDescription = message.text,
                        modifier = Modifier.height(30.dp),
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                    )
                }
                message.emotes.isEmpty() -> Row(verticalAlignment = Alignment.CenterVertically) {
                    medal()
                    Text(
                        text = prefix + message.text,
                        color = Color.White.copy(alpha = 0.92f),
                        fontSize = 14.4.sp,
                    )
                }
                else -> Row(verticalAlignment = Alignment.CenterVertically) {
                    medal()
                    androidx.compose.foundation.layout.Box(Modifier.weight(1f)) {
                        ChatWithEmotes(prefix, message)
                    }
                }
            }
        }
    }
}

/** 系统提示：只读、弱化的单行文本。 */
@Composable
private fun LiveSystemText(message: LiveChatMessage) {
    Text(
        text = message.text,
        color = Color.White.copy(alpha = 0.45f),
        fontSize = 14.4.sp,
    )
}

/** 直播弹幕的粉丝牌：和 `SpaceProfileMedal` 同一套「半透明底色 + 同色文字」胶囊。 */
@Composable
private fun LiveMedalBadge(message: LiveChatMessage) {
    val name = message.medalName?.takeIf { it.isNotBlank() } ?: return
    val medalColor = if (message.medalColor != 0) {
        Color(0xFF000000L or (message.medalColor.toLong() and 0xFFFFFFL))
    } else {
        MiuixTheme.colorScheme.primary
    }
    Box(
        modifier = Modifier
            .padding(end = 4.dp)
            .clip(Capsule())
            .background(medalColor.copy(alpha = 0.16f))
            .padding(horizontal = 5.dp, vertical = 1.dp),
    ) {
        Text(
            text = if (message.medalLevel > 0) "$name Lv.${message.medalLevel}" else name,
            color = medalColor,
            fontSize = 9.sp,
            lineHeight = 10.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

/** 文内表情：把 [表情名] 这类 token 换成图片，其余仍按文字排版。 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

@Composable
internal fun ChatWithEmotes(prefix: String, message: LiveChatMessage) {
    val segments = remember(message.text, message.emotes) {
        splitByEmotes(message.text, message.emotes.keys)
    }
    androidx.compose.foundation.layout.FlowRow(
        verticalArrangement = Arrangement.Center,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(prefix, color = Color.White.copy(alpha = 0.92f), fontSize = 14.4.sp)
        segments.forEach { segment ->
            when (segment) {
                is ChatSegment.Text -> Text(
                    text = segment.value,
                    color = Color.White.copy(alpha = 0.92f),
                    fontSize = 14.4.sp,
                )
                is ChatSegment.Emote -> {
                    val url = message.emotes[segment.token]
                    if (url.isNullOrBlank()) {
                        Text(
                            text = segment.token,
                            color = Color.White.copy(alpha = 0.92f),
                            fontSize = 14.4.sp,
                        )
                    } else {
                        AsyncImage(
                            model = url,
                            contentDescription = segment.token,
                            modifier = Modifier
                                .height((message.emoteHeights[segment.token] ?: 20).dp)
                                .width(((message.emoteHeights[segment.token] ?: 20) * 1.2f).dp),
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                        )
                    }
                }
            }
        }
    }
}

private sealed interface ChatSegment {
    data class Text(val value: String) : ChatSegment
    data class Emote(val token: String) : ChatSegment
}

/** 按表情 token 把文本切开；token 可能是「[笑]」也可能是不带括号的「笑」。 */

private fun splitByEmotes(text: String, tokens: Set<String>): List<ChatSegment> {
    if (tokens.isEmpty()) return listOf(ChatSegment.Text(text))
    val sorted = tokens.sortedByDescending { it.length }
    val result = mutableListOf<ChatSegment>()
    var index = 0
    val buffer = StringBuilder()
    while (index < text.length) {
        val match = sorted.firstOrNull { text.startsWith(it, index) }
        if (match != null) {
            if (buffer.isNotEmpty()) {
                result += ChatSegment.Text(buffer.toString())
                buffer.clear()
            }
            result += ChatSegment.Emote(match)
            index += match.length
        } else {
            buffer.append(text[index])
            index++
        }
    }
    if (buffer.isNotEmpty()) result += ChatSegment.Text(buffer.toString())
    return result
}

/* ------------------------------------------------------------------ */
/* 输入条：和视频页同款的胶囊，但用纯色底（本页不用玻璃）                */
/* ------------------------------------------------------------------ */
