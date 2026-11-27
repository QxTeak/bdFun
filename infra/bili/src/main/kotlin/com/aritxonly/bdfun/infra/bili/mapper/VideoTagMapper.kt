package com.aritxonly.bdfun.infra.bili.mapper

import com.aritxonly.bdfun.core.model.video.VideoTag
import org.json.JSONArray

/**
 * `/x/web-interface/view/detail/tag` 的 data 数组。
 *
 * 字段与 PiliPlus 的 VideoTagItem 对齐：tag_id/tag_name/tag_type/music_id；
 * 空名标签直接丢掉，避免界面上出现空胶囊。
 */
internal fun mapVideoTags(array: JSONArray?): List<VideoTag> {
    if (array == null) return emptyList()
    return buildList {
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val name = item.optString("tag_name").trim()
            if (name.isEmpty()) continue
            add(
                VideoTag(
                    id = item.optLong("tag_id", 0L),
                    name = name,
                    type = item.optString("tag_type"),
                    musicId = item.optString("music_id"),
                ),
            )
        }
    }
}
