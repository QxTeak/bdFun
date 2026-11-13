package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.core.model.message.MessageItem
import com.aritxonly.bdfun.navigateToSpace
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun MessageActorAvatar(item: MessageItem) {
    val context = LocalContext.current
    if (item.actor.likeAvatars.size > 1) {
        Box(Modifier.size(44.dp)) {
            item.actor.likeAvatars.take(4).forEachIndexed { index, avatar ->
                Box(Modifier.offset(x = (14 * (index % 2)).dp, y = (14 * (index / 2)).dp)) {
                    MessageAvatar(avatar, size = 30.dp)
                }
            }
        }
    } else {
        Box(Modifier.clickable(enabled = item.userMid > 0) { navigateToSpace(context, item.userMid) }) {
            MessageAvatar(item.avatar, size = 44.dp)
        }
    }
}

@Composable
internal fun MessageActorHeadline(item: MessageItem, aggregate: Boolean = false) {
    val colors = MiuixTheme.colorScheme
    Text(buildAnnotatedString {
        withStyle(SpanStyle(color = colors.primary)) { append(item.name.ifBlank { "用户" }) }
        if (aggregate && item.likeCount > 1) append(" 等${item.likeCount}人")
        withStyle(SpanStyle(color = colors.onBackgroundVariant)) { append(" ${item.action}") }
    }, fontSize = 14.sp, lineHeight = 21.sp)
}

@Composable
internal fun MessageOriginalText(text: String) {
    androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth()
        .clip(com.aritxonly.teak.common.chrome.TeakContinuousCornerShape(10.dp))
        .background(MiuixTheme.colorScheme.surfaceContainerHigh.copy(alpha = .45f))
        .padding(horizontal = 12.dp, vertical = 10.dp)) {
        QuoteBlock("", text)
    }
}

@Composable
internal fun MessageRowFooter(item: MessageItem, onMore: () -> Unit) {
    val colors = MiuixTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(formatMessageDate(item.time), color = colors.onBackgroundVariant, fontSize = 12.sp)
        if (item.noticeState == 1) {
            Text(" · 已关闭通知", color = colors.onBackgroundVariant, fontSize = 12.sp)
        }
        Spacer(Modifier.weight(1f))
        MessageMoreDots(onMore)
    }
}
