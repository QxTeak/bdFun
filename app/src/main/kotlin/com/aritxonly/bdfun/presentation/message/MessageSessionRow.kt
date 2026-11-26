package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aritxonly.bdfun.core.model.message.MessageSession
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun SessionRow(session: MessageSession, onClick: () -> Unit, onLongClick: () -> Unit) {
    val colors = MiuixTheme.colorScheme
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(interactionSource = null, indication = null, onClick = onClick, onLongClick = onLongClick)
            // 左右留白交给列表 contentPadding（与视频页评论区同为 16dp），行内不再叠加。
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(CircleShape).background(colors.surfaceContainerHigh)
                .clickable(enabled = session.preferences.avatarMid > 0) {
                    com.aritxonly.bdfun.navigateToSpace(context, session.preferences.avatarMid)
                },
        ) {
            if (session.avatar.isNotBlank()) {
                AsyncImage(
                    model = rememberImageRequest(session.avatar),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.name.ifBlank { "消息" },
                    color = colors.onBackground,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    // 预览只到时间列左缘为止，给时间下面的未读点留出位置。
                    text = (if (session.pinned) "[置顶] " else "") + (if (session.muted) "[免打扰] " else "") + session.preview.ifBlank { "暂无新消息" },
                    color = colors.onBackgroundVariant,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatSessionTime(session.time),
                    color = colors.onBackgroundVariant.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    maxLines = 1,
                )
                if (session.unread > 0 || session.preferences.unreadDot) {
                    Spacer(Modifier.height(4.dp))
                    UnreadBadge(session.preferences.unreadDot, session.unread)
                }
            }
        }
    }
}

/** 免打扰的未读提示必须是正圆：旧写法用「•」加横向内边距，渲染出来是椭圆。 */
@Composable
private fun UnreadBadge(dotOnly: Boolean, unread: Int) {
    if (dotOnly) {
        Box(Modifier.size(8.dp).background(MessageRed, CircleShape))
    } else {
        UnreadCountBadge(count = unread, color = MessageRed, contentColor = Color.White)
    }
}
