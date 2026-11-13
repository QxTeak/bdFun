package com.aritxonly.bdfun.ui

import com.aritxonly.bdfun.navigateToVideoAtComment
import com.aritxonly.bdfun.navigateToFeed
import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.common.messageOr
import com.aritxonly.bdfun.core.model.message.MessageItem
import kotlinx.coroutines.launch
import com.aritxonly.bdfun.feature.message.application.MessageOpenTarget
import com.aritxonly.bdfun.feature.message.application.messageOpenTarget

internal fun openMessageItem(
    item: MessageItem, kind: MessageFeedKind,
    controller: com.aritxonly.bdfun.feature.message.application.MessageFeedStateHolder,
    scope: kotlinx.coroutines.CoroutineScope, context: android.content.Context,
) {
    com.aritxonly.bdfun.data.LogCollector.record(
        "MSGOPEN",
        "id=${item.id} business=${item.businessId} aid=${item.aid} bvid=${item.bvid} " +
            "root=${item.rootRpid} target=${item.targetRpid} source=${item.sourceRpid} " +
            "kind=$kind",
    )
    val target = messageOpenTarget(item, kind)
    if (target == MessageOpenTarget.Feed) {
        val id = Regex("(?:dynamic|opus)/([0-9]+)").find(item.nativeUri)?.groupValues?.get(1)
            ?: android.net.Uri.parse(item.nativeUri).getQueryParameter("dynamic_id")
        if (!id.isNullOrBlank()) scope.launch {
            try {
                val dynamic = when (val result = controller.dynamic(id)) {
                    is Outcome.Success -> result.value ?: error("动态已删除或暂不可用")
                    is Outcome.Failure -> error(result.error.messageOr("动态加载失败"))
                }
                navigateToFeed(context, dynamic)
            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) { context.showToast(e.message ?: "动态加载失败") }
        } else context.showToast("这条消息未提供可打开的动态链接")
        return
    }
    if (target == MessageOpenTarget.MissingVideo) {
        context.showToast("这条消息没有可打开的视频")
        return
    }
    target as MessageOpenTarget.Video
    navigateToVideoAtComment(
        context = context,
        aid = item.aid,
        bvid = item.bvid,
        title = item.title,
        rootRpid = target.rootRpid,
        targetRpid = target.targetRpid,
        seekMs = item.seekMs,
    )
}
