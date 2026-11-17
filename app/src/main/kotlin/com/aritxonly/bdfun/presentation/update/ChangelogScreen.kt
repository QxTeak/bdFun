package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.BrandPink
import com.aritxonly.bdfun.BuildConfig
import com.aritxonly.bdfun.core.domain.update.UpdateRelease
import com.aritxonly.bdfun.core.domain.update.releaseNoteLines
import top.yukonga.miuix.kmp.basic.TextButton
import com.aritxonly.teak.common.chrome.TeakLargeTitle
import com.aritxonly.teak.common.chrome.TrackTeakPageScroll
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun ChangelogScreen(onBack: () -> Unit) {
    com.aritxonly.bdfun.ChangelogScreenRoute()
}

@Composable
internal fun ChangelogContent(releases: List<UpdateRelease>, loading: Boolean, error: String?, onRetry: () -> Unit) {
    val colors = MiuixTheme.colorScheme
    val classic = com.aritxonly.teak.common.theme.TeakUiStyle.isClassic
    val cardShape = remember(classic) {
        if (classic) androidx.compose.ui.graphics.RectangleShape else RoundedCornerShape(22.dp)
    }
    val listState = rememberLazyListState()
    TrackTeakPageScroll(listState)

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(
            start = if (classic) 0.dp else 16.dp,
            end = if (classic) 0.dp else 16.dp,
            top = 8.dp, bottom = 40.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(if (classic) 0.dp else 10.dp),
    ) {
        item(key = "header") {
            TeakLargeTitle(
                title = "更新日志",
                subtitle = "当前版本 ${BuildConfig.VERSION_NAME}",
            )
        }

        if (loading && releases.isEmpty()) item { Text("正在加载更新日志", Modifier.padding(16.dp)) }
        if (error != null) item {
            Column(Modifier.padding(16.dp)) {
                Text(error)
                TextButton(text = "重试", onClick = onRetry)
            }
        }
        if (!loading && error == null && releases.isEmpty()) item { Text("暂无更新日志", Modifier.padding(16.dp)) }
        releases.forEach { entry ->
            item(key = entry.page) {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    Text(
                        text = entry.version.name,
                        modifier = Modifier.padding(start = if (classic) 16.dp else 14.dp, top = if (classic) 14.dp else 0.dp, bottom = 5.dp),
                        color = colors.onBackgroundVariant,
                        fontSize = 13.sp,
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(cardShape)
                            .background(colors.surfaceContainer)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                    ) {
                        releaseNoteLines(entry.notes).ifEmpty { listOf("此版本暂无更新日志") }.forEach { line ->
                            Row(
                                modifier = Modifier.padding(bottom = 9.dp),
                                verticalAlignment = Alignment.Top,
                            ) {
                                Text(
                                    text = "•",
                                    color = BrandPink,
                                    fontSize = 14.sp,
                                    lineHeight = 21.sp,
                                    modifier = Modifier.width(18.dp),
                                )
                                Text(
                                    text = line,
                                    color = colors.onBackgroundVariant,
                                    fontSize = 14.sp,
                                    lineHeight = 21.sp,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
