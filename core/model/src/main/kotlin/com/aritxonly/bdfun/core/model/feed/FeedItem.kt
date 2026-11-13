package com.aritxonly.bdfun.core.model.feed

import com.aritxonly.bdfun.core.model.video.VideoItem

data class FeedItem(
    val id: String,
    val author: FeedItemAuthor,
    val time: String,
    val text: String,
    val images: List<String>,
    val video: VideoItem?,
    val counts: FeedCounts,
    /** 评论区 oid / type：动态接口的 basic.comment_id_str / comment_type。 */
    val commentOid: String = id,
    val commentType: Int = 17,
    val attachments: FeedAttachments = FeedAttachments(),
) {
    val name get() = author.name
    val face get() = author.face
    val mid get() = author.mid
    val like get() = counts.like
    val comment get() = counts.comment
    val forward get() = counts.forward
    val liked get() = counts.liked
    val title get() = attachments.title
    val orig get() = attachments.orig
    val authorVip get() = author.authorVip
    val isCollection get() = attachments.isCollection
    val pgcCover get() = attachments.pgcCover
    val pgcTitle get() = attachments.pgcTitle
    val pgcLabel get() = attachments.pgcLabel
    val moduleTag get() = attachments.moduleTag
    val liveRecommendation get() = attachments.liveRecommendation
    val ipLocation get() = author.ipLocation
}

data class FeedItemAuthor(
    val name: String,
    val face: String,
    val mid: Long,
    /** 年度大会员 UP：只有这种 UP 的名字在动态卡片里显示粉色。 */
    val authorVip: Boolean = false,
    /** 动态发布 IP 属地（接口 `module_author.pub_location_text`）。 */
    val ipLocation: String = "",
)

data class FeedCounts(
    val like: Int,
    val comment: Int,
    val forward: Int,
    val liked: Boolean = false,
)

data class FeedAttachments(
    /** 专栏 / 图文 opus 的标题（正文仍放 [FeedItem.text]）。 */
    val title: String = "",
    /** 转发动态的原动态内容。 */
    val orig: FeedItem? = null,
    /** 合集 / UGC 合集视频，封面上显示「合集」小胶囊。 */
    val isCollection: Boolean = false,
    /** 番剧 / 电影 / 纪录片等 PGC 内容（动态 PGC 卡）。 */
    val pgcCover: String = "",
    val pgcTitle: String = "",
    val pgcLabel: String = "",
    val moduleTag: String = "",
    val liveRecommendation: FeedLiveRecommendation? = null,
)
