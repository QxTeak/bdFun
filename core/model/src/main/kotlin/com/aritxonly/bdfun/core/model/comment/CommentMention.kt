package com.aritxonly.bdfun.core.model.comment

/** 评论正文里被 @ 的用户。 */
data class CommentMention(
    val mid: Long,
    val name: String,
    /** 被 @ 的人是不是大会员：接口 members[].vip.vipStatus，和评论作者同一个字段。 */
    val isVip: Boolean = false,
)
