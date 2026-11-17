package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.data.SearchUser
import com.aritxonly.bdfun.data.VideoItem

internal fun androidx.compose.foundation.lazy.LazyListScope.searchResultStatusItems(
    content: com.aritxonly.bdfun.feature.search.application.SearchUiState,
    pageColors: top.yukonga.miuix.kmp.theme.Colors,
) {
    val results = content.results
    val users = content.users
    val loading = content.loading
    val error = content.error
    if (loading && results.isEmpty() && users.isEmpty()) {
        item(key = "loading") {
            Box(
                modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                contentAlignment = Alignment.Center,
            ) {
                TeakLoading()
            }
        }
    }
    error?.let { message ->
        if (results.isEmpty() && users.isEmpty()) {
            item(key = "error") {
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

internal fun androidx.compose.foundation.lazy.LazyListScope.searchVideoResultItems(
    content: com.aritxonly.bdfun.feature.search.application.SearchUiState,
    pageColors: top.yukonga.miuix.kmp.theme.Colors, onOpenVideo: (VideoItem) -> Unit,
) {
    val results = content.results
    val loading = content.loading
    val loadedOnce = content.loadedOnce
    val error = content.error
    items(items = results, key = { "r" + it.bvid }) { video ->
        val oneTake = rememberOneTakeAnchor()
        val itemContext = LocalContext.current
        val cardRadiusPx = with(LocalDensity.current) { 10.dp.toPx() }
        SearchResultRow(
            video = video,
            modifier = oneTake.modifier,
            onClick = {
                oneTake.open(itemContext, video, cardRadiusPx) {
                    onOpenVideo(video)
                }
            },
        )
    }
    if (loadedOnce && !loading && results.isEmpty() && error == null) {
        item(key = "empty-videos") {
            Text(
                text = "没有找到相关视频",
                color = pageColors.onBackgroundVariant,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 24.dp),
            )
        }
    }

}

internal fun androidx.compose.foundation.lazy.LazyListScope.searchVideoPageFooter(
    content: com.aritxonly.bdfun.feature.search.application.SearchUiState,
    pageColors: top.yukonga.miuix.kmp.theme.Colors,
) {
    if (content.results.isEmpty()) return
    if (content.loadingMore) {
        item(key = "search-loading-more") {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("加载中…", color = pageColors.onBackgroundVariant, fontSize = 12.sp)
            }
        }
    } else if (content.end) {
        item(key = "search-end") {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("没有更多了", color = pageColors.onBackgroundVariant, fontSize = 12.sp)
            }
        }
    }
}

internal fun androidx.compose.foundation.lazy.LazyListScope.searchUserResultItems(
    content: com.aritxonly.bdfun.feature.search.application.SearchUiState,
    pageColors: top.yukonga.miuix.kmp.theme.Colors, onOpenUser: (SearchUser) -> Unit,
) {
    val users = content.users
    val loading = content.loading
    val loadedOnce = content.loadedOnce
    val error = content.error
    items(items = users, key = { "u" + it.mid }) { user ->
        SearchUserRow(user = user, onClick = { onOpenUser(user) })
    }
    if (loadedOnce && !loading && users.isEmpty() && error == null) {
        item(key = "empty-users") {
            Text(
                text = "没有找到相关用户",
                color = pageColors.onBackgroundVariant,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 24.dp),
            )
        }
    }
}
