package com.aritxonly.bdfun.feature.comment.application

import com.aritxonly.bdfun.core.model.comment.CommentExtras
import com.aritxonly.bdfun.core.model.comment.CommentItem
import com.aritxonly.bdfun.core.model.user.UserProfile

/**
 * 本地乐观评论/回复的唯一构造点。
 *
 * 主评论区和楼中楼在发送成功后都要先显示一条本地条目；字段一旦两边各写一份，
 * 迟早会出现头像、父级或 rpid 兜底规则不一致，所以统一放在这里。
 */
internal fun localCommentItem(
    rpid: Long,
    text: String,
    profile: UserProfile?,
    oid: Long,
    root: Long,
    parent: Long,
    nowMs: Long,
): CommentItem = CommentItem(
    rpid = if (rpid > 0) rpid else nowMs,
    uname = profile?.name ?: "我",
    avatar = profile?.face.orEmpty(),
    message = text,
    like = 0,
    ctime = nowMs / 1000,
    level = profile?.level ?: 0,
    replyCount = 0,
    isUp = false,
    isTop = false,
    extras = CommentExtras(
        mid = profile?.mid ?: 0L,
        oid = oid,
        root = root,
        parent = parent,
    ),
)

/**
 * 折叠回复展示顺序：本地刚发出的回复放最前，服务端预览保持原顺序并去重。
 * 下一次页面刷新/重置时本地条目消失，由接口返回的正常顺序接管。
 */
internal fun mergePreviewReplies(
    server: List<CommentItem>,
    local: List<CommentItem>,
): List<CommentItem> {
    if (local.isEmpty()) return server
    val localIds = local.mapTo(HashSet()) { it.rpid }
    return (local + server.filterNot { it.rpid in localIds }).distinctBy { it.rpid }
}
