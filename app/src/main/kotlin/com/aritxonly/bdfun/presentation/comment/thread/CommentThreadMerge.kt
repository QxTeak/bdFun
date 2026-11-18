package com.aritxonly.bdfun.ui

import com.aritxonly.bdfun.core.model.comment.CommentItem

/**
 * 楼中楼展示顺序：服务端回复保持原顺序，本地刚发出的回复插到它回复的那条下面。
 * 回复根评论时父级不在回复列表里，落在列表最前，也就是根评论下面。
 */
internal fun mergeThreadReplies(
    server: List<CommentItem>,
    local: List<CommentItem>,
    deleted: Set<Long>,
): List<CommentItem> {
    val localIds = local.mapTo(HashSet()) { it.rpid }
    val merged = server.filterNot { it.rpid in deleted || it.rpid in localIds }.toMutableList()
    local.filterNot { it.rpid in deleted }.forEach { reply ->
        if (merged.any { it.rpid == reply.rpid }) return@forEach
        val parentIndex = merged.indexOfFirst { it.rpid == reply.parent }
        if (parentIndex >= 0) merged.add(parentIndex + 1, reply) else merged.add(0, reply)
    }
    return merged
}
