package com.aritxonly.bdfun.ui

import com.aritxonly.teak.common.theme.LocalTeakContentColors
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import top.yukonga.miuix.kmp.utils.overScrollVertical
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun VideoCollectionEpisodes(
    episodes: List<com.aritxonly.bdfun.core.model.video.VideoEpisode>, currentCid: Long,
    presentation: VideoCollectionPresentation, slots: VideoDetailTailSlots, onSelect: (com.aritxonly.bdfun.core.model.video.VideoEpisode) -> Unit,
) {
    val collectionColor = LocalTeakContentColors.current.groupedBackground
    Box(modifier = Modifier.fillMaxWidth().height(198.dp)) {
        LazyColumn(
            state = presentation.scrollState,
            contentPadding = PaddingValues(start = 10.dp, end = 10.dp, bottom = 8.dp),
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(presentation.keepInsideCollection)
                .overScrollVertical(),
        ) {
            itemsIndexed(episodes, key = { index, episode -> "$index:${episode.cid}" }) { _, episode ->
                val active = episode.cid == currentCid
                VideoCollectionEpisodeRow(episode, active, onSelect, slots)
                Spacer(Modifier.height(6.dp))
            }
        }
        // 上下 4dp 渐变，让列表在卡片边缘自然消失。
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(6.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(collectionColor, collectionColor.copy(alpha = 0f)),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(6.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(collectionColor.copy(alpha = 0f), collectionColor),
                    ),
                ),
        )
    }
}
