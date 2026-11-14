package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.core.model.library.FavoriteFolder
import com.aritxonly.bdfun.feature.library.application.*
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun LibraryFolderBar(
    folders: List<FavoriteFolder>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(selectedIndex, folders.size) {
        if (selectedIndex in folders.indices) {
            runCatching { listState.animateScrollToItem(selectedIndex) }
        }
    }
    LazyRow(
        state = listState,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(folders) { index, folder ->
            LibraryFolderChip(
                text = "${folder.title} ${folder.count}",
                selected = index == selectedIndex,
                onClick = { onSelect(index) },
            )
        }
    }
}

@Composable
internal fun LibraryFolderChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val colors = MiuixTheme.colorScheme
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) colors.primary else colors.surfaceContainer)
            .clickable(interactionSource = null, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else colors.onBackground,
            fontSize = 12.sp,
            maxLines = 1,
        )
    }
}

@Composable
internal fun LibraryBatchAction(
    label: String,
    color: Color,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Text(
        text = label,
        color = if (enabled) color else color.copy(alpha = 0.4f),
        fontSize = 13.sp,
        fontWeight = FontWeight.Normal,
        modifier = Modifier
            .clip(CircleShape)
            .clickable(
                enabled = enabled,
                interactionSource = null,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
    )
}
