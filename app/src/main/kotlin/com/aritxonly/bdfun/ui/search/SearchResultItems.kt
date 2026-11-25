package com.aritxonly.bdfun.ui

import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.data.SearchUser
import com.aritxonly.bdfun.data.VideoItem
import com.aritxonly.bdfun.core.model.pgc.SearchPgcItem
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import coil.compose.AsyncImage
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal fun androidx.compose.foundation.lazy.grid.LazyGridScope.searchResultStatusItems(
    content: com.aritxonly.bdfun.feature.search.application.SearchUiState,
    pageColors: top.yukonga.miuix.kmp.theme.Colors,
) {
    val results = content.results
    val users = content.users
    val loading = content.loading
    val error = content.error
    // 首个结果页的加载交给结果列表自己的骨架，这里只保留错误提示。
    error?.let { message ->
        if (results.isEmpty() && users.isEmpty()) {
            item(key = "error", span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                Text(
                    text = message,
                    color = pageColors.onBackgroundVariant,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 24.dp),
                )
            }
        }
    }

}

/**
 * 搜索 / 热门共用的视频结果行。
 *
 * 页面只决定数据、加载态和分页时机，行本身与一镜到底的接入都留在这里，
 * 避免热门页再复制一份卡片。
 */
internal fun androidx.compose.foundation.lazy.grid.LazyGridScope.videoResultRows(
    videos: List<com.aritxonly.bdfun.data.VideoItem>,
    keyPrefix: String,
    onOpenVideo: (com.aritxonly.bdfun.data.VideoItem) -> Unit,
    onLongPressVideo: ((com.aritxonly.bdfun.data.VideoItem) -> Unit)? = null,
    /** 长按/省略号统一传入窗口坐标，菜单锚定在卡片上。 */
    onVideoMenu: ((com.aritxonly.bdfun.data.VideoItem, androidx.compose.ui.geometry.Rect) -> Unit)? = null,
    showSelection: Boolean = false,
    selectedIds: Set<String> = emptySet(),
    enableOneTake: Boolean = true,
    /**
     * 点击后、跳转前执行的准备动作（例如把当前列表塞进播放队列）。
     *
     * 不能写进 [onOpenVideo]：一镜到底由系统动画自己启动 VideoActivity，
     * [onOpenVideo] 只是动画失败时的兜底，正常路径永远不会执行。
     */
    onBeforeOpen: ((com.aritxonly.bdfun.data.VideoItem) -> Unit)? = null,
    /** 页内标签切换动画：由页面传入 contentSwitchItem()，卡片读同一份进度一起滑入。 */
    itemModifier: Modifier = Modifier,
    /** 省略号 / 长按形变菜单：state 是本次锚点，point 非空表示从手指位置展开。 */
    onVideoMorph: ((com.aritxonly.bdfun.data.VideoItem, com.aritxonly.teak.common.navigation.TeakMorphMenuState, androidx.compose.ui.geometry.Offset?) -> Unit)? = null,
) {
    // 搜索/热门接口会返回重复 bvid（同一稿件多个入口），只用 bvid 当 key 会直接崩 LazyGrid。
    itemsIndexed(items = videos, key = { index, video -> keyPrefix + index + "-" + video.bvid }) { _, video ->
        val oneTake = rememberOneTakeAnchor()
        // 页面没显式接线时读取本地形变菜单：搜索、收藏、空间等共用这一行自动获得同一套效果。
        val localMorph = LocalVideoCardMorphMenu.current
        val morph: ((
            com.aritxonly.bdfun.data.VideoItem,
            com.aritxonly.teak.common.navigation.TeakMorphMenuState,
            androidx.compose.ui.geometry.Offset?,
        ) -> Unit)? = androidx.compose.runtime.remember(localMorph, onVideoMorph) {
            onVideoMorph ?: localMorph?.let { handler ->
                { item, state, point -> handler.show(item, state, point) }
            }
        }
        val morphState = if (morph != null) {
            androidx.compose.runtime.remember(video.bvid) {
                com.aritxonly.teak.common.navigation.TeakMorphMenuState()
            }
        } else null
        val pointMenuState = if (morph != null) {
            androidx.compose.runtime.remember(video.bvid) {
                com.aritxonly.teak.common.navigation.TeakMorphMenuState()
            }
        } else null
        val itemContext = LocalContext.current
        val cardRadiusPx = with(LocalDensity.current) { 10.dp.toPx() }
        val open = {
            onBeforeOpen?.invoke(video)
            if (enableOneTake) {
                oneTake.open(itemContext, video, cardRadiusPx) { onOpenVideo(video) }
            } else {
                onOpenVideo(video)
            }
        }
        SearchResultRow(
            video = video,
            onClick = open,
            modifier = (if (enableOneTake) oneTake.modifier else Modifier).then(itemModifier),
            onLongClick = onLongPressVideo?.let { longPress -> { longPress(video) } },
            onLongPressRect = if (morph != null && pointMenuState != null) {
                { rect -> morph(video, pointMenuState, androidx.compose.ui.geometry.Offset(rect.left, rect.top)) }
            } else onVideoMenu?.let { menu -> { rect -> menu(video, rect) } },
            onMoreClick = onVideoMenu?.let { menu -> { rect -> menu(video, rect) } },
            morphState = morphState,
            onMorphOpen = if (morphState != null && morph != null) {
                { morph(video, morphState, null) }
            } else null,
            selection = if (showSelection) video.bvid in selectedIds else null,
        )
    }
}

/** 首屏加载：统一用 Miuix 加载动画（骨架屏组件保留但不再挂载）。 */
internal fun androidx.compose.foundation.lazy.grid.LazyGridScope.videoResultSkeleton(
    count: Int = 6,
    keyPrefix: String = "video-skeleton",
) {
    item(key = keyPrefix, span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
        com.aritxonly.teak.common.chrome.TeakLoadingArea()
    }
}

/** 分页尾部：加载中 / 没有更多。颜色由调用方在组合期取好，列表作用域里不读 CompositionLocal。 */
internal fun androidx.compose.foundation.lazy.grid.LazyGridScope.videoResultFooter(
    loadingMore: Boolean,
    end: Boolean,
    colors: top.yukonga.miuix.kmp.theme.Colors,
    endText: String = "没有更多了",
    modifier: Modifier = Modifier,
) {
    if (loadingMore) {
        item(key = "video-loading-more", span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
            Box(
                modifier = modifier.fillMaxWidth().padding(vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("加载中…", color = colors.onBackgroundVariant, fontSize = 12.sp)
            }
        }
    } else if (end) {
        item(key = "video-end", span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
            Box(
                modifier = modifier.fillMaxWidth().padding(vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(endText, color = colors.onBackgroundVariant, fontSize = 12.sp)
            }
        }
    }
}

internal fun androidx.compose.foundation.lazy.grid.LazyGridScope.searchVideoResultItems(
    content: com.aritxonly.bdfun.feature.search.application.SearchUiState,
    pageColors: top.yukonga.miuix.kmp.theme.Colors, onOpenVideo: (VideoItem) -> Unit,
) {
    val results = content.results
    val loading = content.loading
    val loadedOnce = content.loadedOnce
    val error = content.error
    if (loading && results.isEmpty()) {
        videoResultSkeleton(keyPrefix = "search-skeleton")
    }
    videoResultRows(results, "r", onOpenVideo)
    if (loadedOnce && !loading && results.isEmpty() && error == null) {
        item(key = "empty-videos", span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
            Text(
                text = "没有找到相关视频",
                color = pageColors.onBackgroundVariant,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 24.dp),
            )
        }
    }

}

internal fun androidx.compose.foundation.lazy.grid.LazyGridScope.searchVideoPageFooter(
    content: com.aritxonly.bdfun.feature.search.application.SearchUiState,
    pageColors: top.yukonga.miuix.kmp.theme.Colors,
) {
    if (content.results.isEmpty()) return
    videoResultFooter(loadingMore = content.loadingMore, end = content.end, colors = pageColors)
}

internal fun androidx.compose.foundation.lazy.grid.LazyGridScope.searchUserResultItems(
    content: com.aritxonly.bdfun.feature.search.application.SearchUiState,
    pageColors: top.yukonga.miuix.kmp.theme.Colors, onOpenUser: (SearchUser) -> Unit,
) {
    val users = content.users
    val loading = content.loading
    val loadedOnce = content.loadedOnce
    val error = content.error
    if (loading && users.isEmpty()) {
        item(key = "search-user-loading", span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
            com.aritxonly.teak.common.chrome.TeakLoadingArea()
        }
    }
    itemsIndexed(items = users, key = { index, user -> "u$index-" + user.mid }) { _, user ->
        SearchUserRow(user = user, onClick = { onOpenUser(user) })
    }
    if (loadedOnce && !loading && users.isEmpty() && error == null) {
        item(key = "empty-users", span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
            Text(
                text = "没有找到相关用户",
                color = pageColors.onBackgroundVariant,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 24.dp),
            )
        }
    }
    if (users.isNotEmpty()) {
        videoResultFooter(loadingMore = content.userPaging.loadingMore, end = content.userPaging.end, colors = pageColors)
    }
}

/** 番剧 / 影视搜索结果：封面 + 标题 + 类型/评分/地区。 */
internal fun androidx.compose.foundation.lazy.grid.LazyGridScope.searchPgcResultItems(
    items: List<SearchPgcItem>,
    content: com.aritxonly.bdfun.feature.search.application.SearchUiState,
    pageColors: top.yukonga.miuix.kmp.theme.Colors,
    label: String,
) {
    val loading = content.pgc.loading
    val loadedOnce = content.pgc.loadedOnce
    if (loading && items.isEmpty()) {
        videoResultSkeleton(count = 5, keyPrefix = "pgc-skeleton-$label")
    }
    itemsIndexed(items = items, key = { index, item -> "pgc-$label-$index-" + item.seasonId }) { _, item ->
        SearchPgcRow(item)
    }
    if (loadedOnce && !loading && items.isEmpty()) {
        item(key = "empty-pgc-$label", span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
            Text(
                text = "没有找到相关$label",
                color = pageColors.onBackgroundVariant,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 24.dp),
            )
        }
    }
}

@androidx.compose.runtime.Composable
private fun SearchPgcRow(item: SearchPgcItem) {
    val context = LocalContext.current
    val colors = MiuixTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surfaceContainer)
            .padding(8.dp)
            .clickable { openPgc(context, item.seasonId) },
        verticalAlignment = Alignment.Top,
    ) {
        AsyncImage(
            model = rememberImageRequest(item.cover),
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .width(100.dp)
                .aspectRatio(3f / 4f)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.surfaceContainerHigh),
        )
        Spacer(Modifier.width(12.dp))
        androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
            Text(
                text = item.title,
                color = colors.onBackground,
                fontSize = 14.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            val meta = remember(item.typeName, item.score, item.areas) {
                listOfNotNull(
                    item.typeName.takeIf { it.isNotBlank() },
                    item.score.takeIf { it.isNotBlank() }?.let { "评分 $it" },
                    item.areas.takeIf { it.isNotBlank() },
                ).joinToString(" · ")
            }
            if (meta.isNotBlank()) {
                Text(
                    text = meta,
                    color = colors.onBackgroundVariant,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
            val extra = remember(item.styles, item.indexShow) {
                listOfNotNull(
                    item.styles.takeIf { it.isNotBlank() },
                    item.indexShow.takeIf { it.isNotBlank() },
                ).joinToString(" · ")
            }
            if (extra.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = extra,
                    color = colors.onBackgroundVariant,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** 番剧/影视卡片只带 season_id 进详情页，避免为找首集多拉一整季。 */
private fun openPgc(context: android.content.Context, seasonId: Long) {
    com.aritxonly.bdfun.presentation.pgc.openBangumi(context, seasonId)
}
