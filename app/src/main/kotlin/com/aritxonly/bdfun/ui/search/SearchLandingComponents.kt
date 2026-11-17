package com.aritxonly.bdfun.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.BrandPink
import com.aritxonly.teak.common.icons.AppGlyph
import com.kyant.shapes.Capsule
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.*
import top.yukonga.miuix.kmp.icon.extended.*
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun SearchSuggestGlyph() {
    AppGlyph(MiuixIcons.Search, MiuixTheme.colorScheme.onBackgroundVariant, size = 16.dp)
}

@Composable
internal fun HistoryWord(
    word: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MiuixTheme.colorScheme
    Row(
        modifier = modifier
            .clip(Capsule())
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = word,
            color = colors.onBackground,
            fontSize = 13.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun SectionHeader(title: String, action: String?, onAction: (() -> Unit)?) {
    val colors = MiuixTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth()
            .padding(
                start = if (com.aritxonly.teak.common.theme.TeakUiStyle.isClassic) 16.dp else 0.dp,
                end = if (com.aritxonly.teak.common.theme.TeakUiStyle.isClassic) 16.dp else 0.dp,
                bottom = 10.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, color = colors.onBackground, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.weight(1f))
        if (action != null && onAction != null) {
            Text(
                text = action,
                color = colors.onBackgroundVariant,
                fontSize = 12.sp,
                modifier = Modifier.clickable(onClick = onAction),
            )
        }
    }
}

@Composable
internal fun HotRow(rank: Int, word: String, onClick: () -> Unit) {
    val colors = MiuixTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = rank.toString(),
            color = if (rank <= 3) BrandPink else colors.onBackgroundVariant,
            fontSize = 13.sp,
            fontWeight = if (rank <= 3) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.width(22.dp),
        )
        Text(
            text = word,
            color = colors.onBackground,
            fontSize = 13.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
