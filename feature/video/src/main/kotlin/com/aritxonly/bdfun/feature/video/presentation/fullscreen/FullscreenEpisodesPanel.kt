package com.aritxonly.bdfun.ui

import com.aritxonly.bdfun.core.common.formatDuration

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.first

@Composable
internal fun EpisodesPanel(
    info: FullscreenPlayerInfo,
    actions: FullscreenActions,
    onClose: () -> Unit,
    accentColor: Color,
) {

    PanelCard(title = "选集") {
        Text(
            text = if (info.pages.size <= 1) "当前视频为单集" else "共 ${info.pages.size} 集",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 11.sp,
        )
        Spacer(Modifier.height(8.dp))
        val listState = rememberLazyListState()
        val currentIndex = info.pages.indexOfFirst { it.cid == info.currentCid }
        // 打开面板时把当前集摆在正中：先滚到它，等布局出来再回退半个视口。
        LaunchedEffect(currentIndex, info.pages.size) {
            if (currentIndex < 0) return@LaunchedEffect
            listState.scrollToItem(currentIndex)
            val layout = snapshotFlow { listState.layoutInfo }
                .first { it.visibleItemsInfo.isNotEmpty() }
            val item = layout.visibleItemsInfo.firstOrNull { it.index == currentIndex }
                ?: layout.visibleItemsInfo.first()
            val offset = ((layout.viewportSize.height - item.size) / 2).coerceAtLeast(0)
            if (offset > 0) listState.scrollToItem(currentIndex, -offset)
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
        ) {
            itemsIndexed(
                items = info.pages,
                key = { index, _ -> "episode-$index" },
            ) { index, page ->
                val active = page.cid == info.currentCid
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (active) accentColor.copy(alpha = 0.18f) else Color.Transparent,
                        )
                        .clickable(interactionSource = null, indication = null) {
                            actions.onPageSelected(page)
                            onClose()
                        }
                        .padding(horizontal = 10.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "P${index + 1}",
                        color = if (active) accentColor else Color.White.copy(alpha = 0.55f),
                        fontSize = 12.sp,
                        modifier = Modifier.width(34.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = page.part.ifBlank { "第 ${index + 1} 集" },
                            color = if (active) accentColor else Color.White,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = formatDuration(page.duration),
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 10.sp,
                        )
                    }
                    if (active) {
                        Text("播放中", color = accentColor, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}
