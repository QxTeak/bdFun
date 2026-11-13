package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
internal fun FeedCardText(item: FeedItem, expanded: Boolean) {
    val colors = MiuixTheme.colorScheme

    if (item.moduleTag.isNotBlank()) {
        Text(item.moduleTag, color = colors.primary, fontSize = 12.sp,
            modifier = Modifier.padding(top = 8.dp))
    }

    if (item.title.isNotBlank()) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = item.title,
            color = colors.onBackground,
            fontSize = 15.5.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Medium,
            maxLines = if (expanded) Int.MAX_VALUE else 2,
            overflow = TextOverflow.Ellipsis,
        )
    }

    if (item.text.isNotBlank()) {
        Spacer(Modifier.height(8.dp))
        EmoteText(
            content = EmoteTextContent(
                text = item.text,
            ),
            style = EmoteTextStyle(
                color = colors.onBackground,
                fontSize = 15.5.sp,
                lineHeight = 23.sp,
                maxLines = if (expanded) Int.MAX_VALUE else 6,
            ),
        )
    }

}
