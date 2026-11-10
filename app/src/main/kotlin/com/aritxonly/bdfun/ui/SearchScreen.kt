package com.aritxonly.bdfun.ui

import com.aritxonly.teak.common.chrome.centeredListWidth
import com.aritxonly.teak.common.chrome.TrackTeakPageScroll

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aritxonly.bdfun.data.BiliApi
import com.aritxonly.bdfun.data.SettingsStore
import com.aritxonly.bdfun.data.VideoItem
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun SearchScreen(
    api: BiliApi,
    settings: SettingsStore,
    onBack: () -> Unit,
    onOpenVideo: (VideoItem) -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val scope = rememberCoroutineScope()

    var query by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var hot by remember { mutableStateOf<List<String>>(emptyList()) }
    var history by remember { mutableStateOf(settings.searchHistory) }

    BackHandler(onBack = onBack)

    LaunchedEffect(Unit) {
        if (settings.showHotSearch) {
            hot = runCatching { api.hotSearch() }.getOrDefault(emptyList())
        }
    }

    fun submit(keyword: String) {
        val q = keyword.trim()
        if (q.isEmpty()) return
        query = q
        submitted = true
        settings.addSearchHistory(q)
        history = settings.searchHistory
        scope.launch {
            loading = true
            error = null
            runCatching { api.search(q) }
                .onSuccess { list ->
                    results = list
                    if (list.isEmpty()) error = "没有找到相关视频"
                }
                .onFailure { error = it.message ?: "搜索失败" }
            loading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, top = 8.dp, end = 16.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.size(40.dp))

            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(CircleShape)
                    .background(colors.surfaceContainerHigh)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = TextStyle(color = colors.onBackground, fontSize = 14.sp),
                    cursorBrush = SolidColor(colors.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { submit(query) }),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        Box {
                            if (query.isEmpty()) {
                                Text(
                                    text = "搜索视频",
                                    color = colors.onBackgroundVariant,
                                    fontSize = 14.sp,
                                )
                            }
                            inner()
                        }
                    },
                )
            }

            Spacer(Modifier.width(10.dp))
            Text(
                text = "搜索",
                color = colors.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.clickable { submit(query) },
            )
        }

        Box(modifier = Modifier.fillMaxSize().centeredListWidth()) {
            if (!submitted) {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    if (history.isNotEmpty()) {
                        item(key = "history-title") {
                            SectionHeader(
                                title = "搜索历史",
                                action = "清空",
                                onAction = {
                                    settings.clearSearchHistory()
                                    history = settings.searchHistory
                                },
                            )
                        }
                        item(key = "history") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(18.dp))
                                    .background(colors.surface)
                                    .padding(horizontal = 14.dp, vertical = 14.dp),
                            ) {
                                ChipFlow(
                                    options = history,
                                    onClick = { submit(it) },
                                )
                            }
                        }
                    }
                    if (settings.showHotSearch && hot.isNotEmpty()) {
                        item(key = "hot-title") { SectionHeader(title = "哔哩哔哩热搜") }
                        item(key = "hot") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(18.dp))
                                    .background(colors.surface)
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                            ) {
                                hot.forEachIndexed { index, word ->
                                    HotSearchRow(rank = index + 1, word = word) { submit(word) }
                                }
                            }
                        }
                    }
                }
            } else {
                val resultsListState = rememberLazyListState()
                TrackTeakPageScroll(resultsListState)
                LazyColumn(
                    state = resultsListState,
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                ) {
                    items(items = results, key = { it.bvid }) { item ->
                        SearchResultRow(item = item, onClick = { onOpenVideo(item) })
                    }
                    error?.let { message ->
                        item(key = "error") {
                            Text(
                                text = message,
                                modifier = Modifier.padding(vertical = 24.dp),
                                color = colors.onBackgroundVariant,
                                fontSize = 13.sp,
                            )
                        }
                    }
                }
                if (loading) {
                    TeakLoading(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 40.dp),
                        color = colors.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun HotSearchRow(rank: Int, word: String, onClick: () -> Unit) {
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
            color = if (rank <= 3) colors.primary else colors.onBackgroundVariant,
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

@Composable
private fun SectionHeader(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    val colors = MiuixTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = colors.onBackground,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.weight(1f))
        if (action != null && onAction != null) {
            Text(
                text = action,
                color = colors.onBackgroundVariant,
                fontSize = 12.sp,
                modifier = Modifier.clickable { onAction() },
            )
        }
    }
}

/** 简易流式排布：固定 3 列，避免引入额外布局库。 */
@Composable
private fun ChipFlow(options: List<String>, onClick: (String) -> Unit) {
    val colors = MiuixTheme.colorScheme
    options.chunked(3).forEach { row ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            row.forEach { option ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(CircleShape)
                        .background(colors.surfaceContainerHigh)
                        .clickable { onClick(option) }
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                ) {
                    Text(
                        text = option,
                        color = colors.onBackground,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun SearchResultRow(item: VideoItem, onClick: () -> Unit) {
    val colors = MiuixTheme.colorScheme
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
    ) {
        Box {
            AsyncImage(
                model = rememberImageRequest(item.cover),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(148.dp)
                    .aspectRatio(16f / 9f)
                    .clip(ContinuousCornerShape(12.dp))
                    .background(colors.surfaceContainerHigh),
            )
            if (item.duration > 0) {
                Text(
                    text = formatDuration(item.duration),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(5.dp)
                        .clip(CircleShape)
                        .background(Color(0x99000000))
                        .padding(horizontal = 6.dp, vertical = 1.dp),
                    color = Color.White,
                    fontSize = 10.sp,
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                color = colors.onBackground,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = item.ownerName,
                color = colors.onBackgroundVariant,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${formatCount(item.play)} 播放",
                color = colors.onBackgroundVariant,
                fontSize = 11.sp,
            )
        }
    }
}
