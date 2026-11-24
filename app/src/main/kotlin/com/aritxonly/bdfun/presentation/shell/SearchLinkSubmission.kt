package com.aritxonly.bdfun

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import com.aritxonly.bdfun.navigateToVideoByBvid
import kotlinx.coroutines.launch

/**
 * 搜索框提交：如果输入本身就是 B 站视频链接，直接按链接打开视频，
 * 不再把它当关键词去搜。短链解析失败时仍退回普通搜索。
 */
@Composable
internal fun rememberSearchLinkSubmitter(
    context: Context,
    onSearch: (String) -> Unit,
): (String) -> Unit {
    val scope = rememberCoroutineScope()
    val holder = remember { AppGraph.incomingVideoLinks() }
    val latestSearch = rememberUpdatedState(onSearch)
    DisposableEffect(holder) { onDispose { holder.close() } }
    return remember(holder, context, scope) {
        { raw ->
            val word = raw.trim()
            if (word.isEmpty()) return@remember
            val pending = holder.begin(word)
            if (pending == null) {
                latestSearch.value(word)
            } else {
                scope.launch {
                    val route = holder.resolve(pending)
                    if (route != null) navigateToVideoByBvid(context, route.bvid)
                    else latestSearch.value(word)
                }
            }
        }
    }
}
