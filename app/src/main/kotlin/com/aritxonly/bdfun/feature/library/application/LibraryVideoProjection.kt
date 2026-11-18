package com.aritxonly.bdfun.feature.library.application

import com.aritxonly.bdfun.core.model.library.HistoryItem
import com.aritxonly.bdfun.core.model.video.VideoItem
import com.aritxonly.bdfun.core.model.video.VideoItemExtras

internal fun HistoryItem.toVideoItem(): VideoItem = VideoItem(
    bvid = bvid, aid = aid, title = title, cover = cover, ownerName = author,
    ownerFace = "", ownerMid = 0, play = play.toLong(), danmaku = danmaku.toLong(),
    duration = duration, pubdate = viewedAt, extras = VideoItemExtras(cid = cid),
)
