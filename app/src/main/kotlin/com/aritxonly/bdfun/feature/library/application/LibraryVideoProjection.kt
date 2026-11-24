package com.aritxonly.bdfun.feature.library.application

import com.aritxonly.bdfun.core.model.library.HistoryItem
import com.aritxonly.bdfun.core.model.video.VideoItem
import com.aritxonly.bdfun.core.model.video.VideoItemExtras

/** 历史页分类胶囊：按接口 history.business 归组，未知来源仍按普通视频展示。 */
internal enum class HistoryCategory(val label: String) {
    All("全部"), Video("视频"), Article("专栏"), Live("直播"), Bangumi("番剧"), Cinema("影视"),
}

internal fun HistoryItem.historyCategory(): HistoryCategory = when (business.lowercase()) {
    "article", "opus" -> HistoryCategory.Article
    "live" -> HistoryCategory.Live
    "pgc", "bangumi" -> HistoryCategory.Bangumi
    "cinema", "movie", "film", "tv" -> HistoryCategory.Cinema
    else -> HistoryCategory.Video
}

internal fun HistoryItem.toVideoItem(): VideoItem = VideoItem(
    bvid = bvid, aid = aid, title = title, cover = cover, ownerName = author,
    ownerFace = "", ownerMid = 0, play = play.toLong(), danmaku = danmaku.toLong(),
    duration = duration, pubdate = viewedAt, extras = VideoItemExtras(cid = cid),
)
