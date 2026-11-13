package com.aritxonly.bdfun.ui

import androidx.compose.runtime.*
import kotlinx.coroutines.*

/** 页面动作随当前账号的状态对象一起取消；离开 pager 不销毁容器持有的分类缓存。 */
@Composable
internal fun rememberFeedTaskScope(state: FeedState): CoroutineScope {
    val parent = rememberCoroutineScope()
    val scope = remember(state) { CoroutineScope(parent.coroutineContext + SupervisorJob(parent.coroutineContext[Job])) }
    DisposableEffect(state) { onDispose { scope.cancel() } }
    return scope
}
