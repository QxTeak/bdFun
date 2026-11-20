package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.model.video.VideoDetail

/**
 * 视频页评论区身份。
 *
 * 普通稿件和番剧/影视都用**当前集的 aid** + type=1（与 PiliPlus 的
 * `oid = isPugv ? epId : aid` 一致）。2026-11-19 实测：把 PGC 的 `ep_id`
 * 直接当 oid 请求 `/x/v2/reply/main` 会返回 0 条，必须用该集的 aid。
 * 换集时 aid 会变化，配合视频页把 oid 放进 key，评论区自动重建。
 */
data class VideoCommentTarget(val oid: String, val type: Int)

fun videoCommentTarget(detail: VideoDetail): VideoCommentTarget =
    VideoCommentTarget(detail.aid.toString(), 1)
