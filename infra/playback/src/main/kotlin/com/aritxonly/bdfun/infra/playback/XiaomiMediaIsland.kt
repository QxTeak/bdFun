package com.aritxonly.bdfun.infra.playback

import android.os.Build
import android.os.Bundle
import org.json.JSONObject

/**
 * 小米 HyperOS「媒体通知上岛 + 拖拽分享」适配。
 *
 * 参见《媒体通知岛上拖拽分享适配说明》：媒体通知带 `miui.focus.param.media`
 * 这个 extra 后，用户可以把超级岛直接拖进聊天窗口分享；`pic` 对媒体通知无效，
 * 封面由系统取通知的大图标，所以这里只传标题、内容和分享链接。
 *
 * 非小米机型返回 null，不往通知里塞无用字段。
 */
internal fun xiaomiMediaIslandExtras(title: String, content: String, shareUrl: String): Bundle? {
    if (!Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true)) return null
    if (title.isBlank()) return null
    val payload = runCatching {
        val shareData = JSONObject()
            .put("title", title)
            .put("content", content.ifBlank { "分享自 bdFun" })
        if (shareUrl.isNotBlank()) shareData.put("shareContent", shareUrl)
        JSONObject()
            .put("param_v2", JSONObject().put("param_island", JSONObject().put("shareData", shareData)))
            .toString()
    }.getOrNull() ?: return null
    return Bundle().apply { putString("miui.focus.param.media", payload) }
}
