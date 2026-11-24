package com.aritxonly.bdfun.ui

import com.aritxonly.bdfun.navigateToSpace
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.core.model.comment.CommentItem
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun CommentRowMessage(item: CommentItem) {
    val colors = MiuixTheme.colorScheme
    val context = LocalContext.current
    val photo = LocalPhotoViewer.current
    val view = androidx.compose.ui.platform.LocalView.current
    EmoteText(
        content = EmoteTextContent(
            text = item.message,
            extraEmotes = item.emotes,
            // 正文里 @ 到的人：和折叠回复同色，统一用「我的」页图标蓝（见 EmoteTextStyle 默认值）。
            mentions = remember(item.mentions) {
            item.mentions.associate { it.name to it.mid }
        },
        ),
        style = EmoteTextStyle(
            color = colors.onBackground,
            fontSize = CommentTypography.bodySize,
            lineHeight = CommentTypography.bodyLineHeight,
            fontWeight = FontWeight.Normal,
        ),
        onMentionClick = { mid ->
            if (mid > 0L) {
                navigateToSpace(context, mid)
            }
        },
    )
    if (item.pictures.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        CommentPictures(item.pictures) { index, rect, radius, origins ->
            // 独立楼中楼窗口的坐标需换算到全屏预览窗口。
            val screen = IntArray(2)
            val window = IntArray(2)
            view.getLocationOnScreen(screen)
            view.getLocationInWindow(window)
            val offset = androidx.compose.ui.geometry.Offset((screen[0] - window[0]).toFloat(), (screen[1] - window[1]).toFloat())
            photo.open(item.pictures, index, rect.translate(offset), radius, origins.map { it?.translate(offset) })

        }
    }
}
