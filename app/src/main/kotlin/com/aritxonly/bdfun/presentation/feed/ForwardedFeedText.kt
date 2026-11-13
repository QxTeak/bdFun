package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.core.model.feed.FeedItem
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun ForwardedFeedText(item: FeedItem) {
    val colors = MiuixTheme.colorScheme

    if (item.title.isNotBlank()) {
        Spacer(Modifier.height(6.dp))
        Text(
            text = item.title,
            color = colors.onBackground,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
    if (item.text.isNotBlank()) {
        Spacer(Modifier.height(6.dp))
        EmoteText(
            content = EmoteTextContent(
                text = item.text,
            ),
            style = EmoteTextStyle(
                color = colors.onBackground,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                maxLines = 4,
            ),
        )
    }
}
