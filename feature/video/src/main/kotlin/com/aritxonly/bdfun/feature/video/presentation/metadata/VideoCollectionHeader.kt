package com.aritxonly.bdfun.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import com.aritxonly.teak.common.icons.AppGlyph
import com.aritxonly.teak.common.icons.LucideIcons
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Playlist
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun VideoCollectionHeader(
    title: String, count: Int, currentIndex: Int, expanded: Boolean,
    onExpand: () -> Unit, onScroll: (Int) -> Unit,
) {
    val colors = MiuixTheme.colorScheme
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(interactionSource = null, indication = null) { onExpand() }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppGlyph(MiuixIcons.Playlist, colors.onBackground, size = 18.dp)
            Spacer(Modifier.width(8.dp))
            Text(
                text = "合集 · $title",
                color = colors.onBackground,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            // 一键回到顶部，紧贴在合集名称右边。
            CollectionScrollButton(LucideIcons.ArrowUpToLine) { onScroll(0) }
            // 一键到底：排在「一键到顶」右边。
            CollectionScrollButton(LucideIcons.ArrowDownToLine) { onScroll(2) }
            // 定位到当前播放的这一集：展开合集并滚到当前集。
            CollectionScrollButton(LucideIcons.LocateFixedFilled) { onScroll(1) }
            Spacer(Modifier.weight(1f))
            Text(
                text = "${currentIndex + 1}/${count}",
                color = colors.onBackgroundVariant,
                fontSize = 11.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.End,
                modifier = Modifier.width(52.dp),
            )
            Spacer(Modifier.width(2.dp))
            ExpandChevron(expanded = expanded)
        }
}

@Composable
private fun CollectionScrollButton(icon: Int, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .clickable(interactionSource = null, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        AppGlyph(icon, MiuixTheme.colorScheme.onBackgroundVariant, size = 14.dp)
    }
}
