package com.aritxonly.bdfun.infra.bili.mapper

import com.aritxonly.bdfun.core.model.video.PopularPage
import com.aritxonly.bdfun.infra.bili.cleanBiliHtml
import org.json.JSONObject

/**
 * 热门列表响应 -> [PopularPage]。
 *
 * 真实样本（2026-11-19，匿名请求 pn=1&ps=5）：
 * `data.list[]` 的字段与推荐流同一套（bvid/aid/pic/title/owner/stat/duration/cid），
 * 分页结束标记是 `data.no_more`，不是 has_more。
 */
internal fun readPopularPage(payload: JSONObject): PopularPage {
    val videos = payload.optJSONArray("list").objects().mapNotNull { item ->
        readFeedVideo(item)?.let { mapFeedVideo(it, ::cleanBiliHtml) }
    }
    return PopularPage(videos = videos, hasMore = !payload.optBoolean("no_more", false) && videos.isNotEmpty())
}
