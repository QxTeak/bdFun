package com.aritxonly.bdfun.ui

import com.aritxonly.teak.common.chrome.TrackTeakPageScroll
import com.aritxonly.teak.common.chrome.TeakPageChrome

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.TeakNavigator
import com.aritxonly.bdfun.data.SentRecord
import com.aritxonly.bdfun.data.VideoItem
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 「我的评论与弹幕」：本机记录用户发出的内容。
 * 弹幕记录视频内时间点，点击后带进度跳回原视频。
 */
@Composable
fun MySentScreen(onBack: () -> Unit) {
    val pageChrome = com.aritxonly.teak.common.chrome.LocalTeakPageScrollState.current
    val colors = MiuixTheme.colorScheme
    val context = LocalContext.current
    val records = remember { com.aritxonly.bdfun.AppGraph.sentHistory.all() }
    var filter by remember { mutableIntStateOf(0) }
    val shown = when (filter) {
        1 -> records.filter { it.kind == "comment" }
        2 -> records.filter { it.kind == "danmaku" }
        else -> records
    }
    val listState = rememberLazyListState()
    TrackTeakPageScroll(listState)
    val cardShape = remember { ContinuousCornerShape(18.dp) }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "title") {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                Spacer(Modifier.height(52.dp))
                Text(
                    text = "我的评论与弹幕",
                    modifier = Modifier
                        .padding(start = 10.dp)
                        .graphicsLayer {
                            val p = (pageChrome.collapse / 0.65f).coerceIn(0f, 1f)
                            alpha = 1f - p
                        }
                        .blur((12f * (pageChrome.collapse / 0.65f).coerceIn(0f, 1f)).dp),
                    color = colors.onBackground,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Normal,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "仅记录在本 App 内发出的评论与弹幕",
                    modifier = Modifier.padding(start = 12.dp),
                    color = colors.onBackgroundVariant,
                    fontSize = 11.5.sp,
                )
            }
        }

        item(key = "filters") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("全部", "评论", "弹幕").forEachIndexed { index, label ->
                    val active = filter == index
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (active) colors.primary else colors.surface)
                            .clickable { filter = index }
                            .padding(horizontal = 16.dp, vertical = 7.dp),
                    ) {
                        Text(
                            text = "$label ${
                                when (index) {
                                    1 -> records.count { it.kind == "comment" }
                                    2 -> records.count { it.kind == "danmaku" }
                                    else -> records.size
                                }
                            }",
                            color = if (active) Color.White else colors.onBackground,
                            fontSize = 12.sp,
                            fontWeight = if (active) FontWeight.Normal else FontWeight.Normal,
                        )
                    }
                }
            }
        }

        if (shown.isEmpty()) {
            item(key = "empty") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(cardShape)
                        .background(colors.surface)
                        .padding(vertical = 44.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "还没有记录，发一条评论或弹幕试试",
                        color = colors.onBackgroundVariant,
                        fontSize = 12.5.sp,
                    )
                }
            }
        }

        items(shown.size) { index ->
            val record = shown[index]
            SentRecordRow(
                record = record,
                shape = cardShape,
                onClick = {
                    if (record.bvid.isBlank()) {
                        context.showToast("记录缺少视频信息")
                    } else {
                        TeakNavigator.openVideo(
                            context,
                            VideoItem(
                                bvid = record.bvid,
                                aid = 0,
                                title = record.title,
                                cover = "",
                                ownerName = "",
                                ownerFace = "",
                                ownerMid = 0,
                                play = 0,
                                danmaku = 0,
                                duration = 0,
                                pubdate = 0,
                                startPositionMs = record.videoTimeMs,
                            ),
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun SentRecordRow(
    record: SentRecord,
    shape: androidx.compose.ui.graphics.Shape,
    onClick: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val isDanmaku = record.kind == "danmaku"
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(
                        if (isDanmaku) {
                            Color(0xFF3C9BD9).copy(alpha = 0.14f)
                        } else {
                            colors.primary.copy(alpha = 0.14f)
                        },
                    )
                    .padding(horizontal = 7.dp, vertical = 2.dp),
            ) {
                Text(
                    text = if (isDanmaku) "弹幕" else "评论",
                    color = if (isDanmaku) Color(0xFF3C9BD9) else colors.primary,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Normal,
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (isDanmaku) {
                    "视频内 ${formatVideoTime(record.videoTimeMs)}"
                } else {
                    formatSentAt(record.sentAt)
                },
                color = colors.onBackgroundVariant,
                fontSize = 10.5.sp,
            )
            Spacer(Modifier.weight(1f))
            Text(text = "›", color = colors.outline, fontSize = 14.sp)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = record.text,
            color = colors.onBackground,
            fontSize = 13.5.sp,
            lineHeight = 19.sp,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = record.title.ifBlank { record.bvid },
            color = colors.onBackgroundVariant,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (isDanmaku) {
            Spacer(Modifier.height(3.dp))
            Text(
                text = "发送于 ${formatSentAt(record.sentAt)}",
                color = colors.onBackgroundVariant.copy(alpha = 0.8f),
                fontSize = 10.sp,
            )
        }
    }
}

private fun formatVideoTime(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) {
        "%d:%02d:%02d".format(h, m, s)
    } else {
        "%02d:%02d".format(m, s)
    }
}

private fun formatSentAt(ms: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(ms))
