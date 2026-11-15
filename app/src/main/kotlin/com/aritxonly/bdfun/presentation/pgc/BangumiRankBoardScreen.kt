package com.aritxonly.bdfun.presentation.pgc

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aritxonly.bdfun.feature.pgc.application.BangumiRankBoardStateHolder
import com.aritxonly.bdfun.ui.FeedCardShape
import com.aritxonly.bdfun.ui.TeakLoading
import com.aritxonly.bdfun.ui.rememberImageRequest
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 单榜全量页：官方是进独立页面（RankFragment），这里用独立 Activity 对齐。
 * 接口一次给整榜，所以只有「加载中 / 空 / 出错 + 重试」三态，没有分页。
 */
@Composable
internal fun BangumiRankBoardScreen(
    holder: BangumiRankBoardStateHolder,
    onOpenSeason: (Long) -> Unit,
) {
    val ui by holder.state.collectAsState()
    val scope = rememberCoroutineScope()
    LaunchedEffect(holder) { holder.ensureLoaded() }
    val colors = MiuixTheme.colorScheme

    when {
        ui.entries.isEmpty() && ui.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            TeakLoading(size = 40.dp)
        }

        ui.entries.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(ui.error ?: "暂无内容", color = colors.onBackgroundVariant, fontSize = 13.sp)
                Text(
                    text = "点击重试",
                    color = colors.primary,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { scope.launch { holder.retry() } }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }

        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (ui.note.isNotBlank()) {
                item(key = "note") {
                    Text(
                        text = ui.note,
                        color = colors.onBackgroundVariant,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(bottom = 2.dp),
                    )
                }
            }
            items(ui.entries, key = { it.seasonId }) { entry ->
                RankBoardRow(entry) { onOpenSeason(entry.seasonId) }
            }
        }
    }
}

@Composable
private fun RankBoardRow(entry: com.aritxonly.bdfun.core.model.pgc.BangumiRankEntry, onClick: () -> Unit) {
    val colors = MiuixTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(FeedCardShape)
            .background(colors.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = entry.rank.takeIf { it > 0 }?.toString().orEmpty(),
            color = if (entry.rank in 1..3) Color(0xFFFF7A45) else colors.onBackgroundVariant,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            modifier = Modifier.width(28.dp),
        )
        AsyncImage(
            model = rememberImageRequest(entry.cover),
            contentDescription = entry.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .width(64.dp)
                .aspectRatio(3f / 4f)
                .clip(RoundedCornerShape(6.dp))
                .background(colors.surfaceContainerHigh),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = entry.title,
                color = colors.onBackground,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = listOf(entry.subtitle, entry.indexShow)
                    .filter { it.isNotBlank() }
                    .joinToString(" · "),
                color = colors.onBackgroundVariant,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (entry.rating.isNotBlank()) {
            Spacer(Modifier.width(8.dp))
            Text(entry.rating, color = Color(0xFFFF7A45), fontSize = 12.sp, maxLines = 1)
        }
    }
}
