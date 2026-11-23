package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.teak.common.settings.TeakSettingsCard
import com.aritxonly.teak.common.settings.TeakSettingsPage
import top.yukonga.miuix.kmp.theme.MiuixTheme

import com.aritxonly.bdfun.core.application.DownloadQueueStateHolder
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.launch

@Composable
internal fun CacheScreen(queue: DownloadQueueStateHolder, onOpen: (com.aritxonly.bdfun.core.model.video.VideoItem) -> Unit) {
    val colors = MiuixTheme.colorScheme
    val context = LocalContext.current
    val entries by queue.state.collectAsState()
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    if (entries.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 标题照常渲染，但空提示覆盖整屏居中，不受大标题占位影响。
            TeakSettingsPage(title = "离线缓存", subtitle = null) {}
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "还没有缓存的视频",
                    color = colors.onBackground,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                )
                Text(
                    text = "在播放器「更多」中选择「缓存」即可下载",
                    modifier = Modifier.padding(top = 6.dp),
                    color = colors.onBackgroundVariant,
                    fontSize = 12.sp,
                )
            }
        }
        return
    }

    TeakSettingsPage(
        title = "离线缓存",
        subtitle = "共 ${entries.size} 个 · ${formatCacheSize(entries.sumOf { it.totalBytes })}",
    ) {
        items(entries, key = { it.id }) { entry ->
            val oneTake = rememberOneTakeAnchor()
            val cacheRadiusPx = with(LocalDensity.current) { 16.dp.toPx() }
            // 封面/播放地址要查文件是否存在；算一次记住，滚动时不再逐个查磁盘。
            val coverUri = androidx.compose.runtime.remember(entry.id, entry.coverPath) { queue.cover(entry) }
            val playable = androidx.compose.runtime.remember(entry.id, entry.videoPath, entry.audioPath) {
                queue.video(entry)
            }
            TeakSettingsCard(
                modifier = Modifier.padding(bottom = 10.dp).then(oneTake.modifier),
            ) {
                CacheRow(
                    entry = entry,
                    cover = coverUri,
                    onOpen = {
                        oneTake.open(
                            context = context,
                            video = playable,
                            radiusPx = cacheRadiusPx,
                        ) { onOpen(playable) }
                    },
                    onDelete = {
                        // 删除会在后台递归删文件；界面这边先给提示，列表随后自动更新。
                        scope.launch { queue.delete(entry.id) }
                        context.showToast("已删除缓存")
                    },
                )
            }
        }
    }
}
