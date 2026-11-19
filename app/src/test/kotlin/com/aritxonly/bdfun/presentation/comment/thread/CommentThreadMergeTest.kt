package com.aritxonly.bdfun.ui

import com.aritxonly.bdfun.core.model.comment.CommentExtras
import com.aritxonly.bdfun.core.model.comment.CommentItem
import org.junit.Assert.assertEquals
import org.junit.Test

class CommentThreadMergeTest {
    private fun reply(id: Long, parent: Long) = CommentItem(
        rpid = id, uname = "u$id", avatar = "", message = "m$id", like = 0, ctime = 0, level = 0,
        replyCount = 0, isUp = false, isTop = false, extras = CommentExtras(parent = parent),
    )

    @Test fun localReplyGoesDirectlyBelowTheReplyItAnswers() {
        val merged = mergeThreadReplies(
            server = listOf(reply(2, 1), reply(3, 1)),
            local = listOf(reply(55, 2)),
            deleted = emptySet(),
        )
        assertEquals(listOf(2L, 55L, 3L), merged.map { it.rpid })
    }

    @Test fun replyToRootStaysAtTheTopOfTheReplyList() {
        val merged = mergeThreadReplies(
            server = listOf(reply(2, 1), reply(3, 1)),
            local = listOf(reply(55, 1)),
            deleted = emptySet(),
        )
        assertEquals(listOf(55L, 2L, 3L), merged.map { it.rpid })
    }

    @Test fun multipleLocalRepliesKeepParentOrdering() {
        val merged = mergeThreadReplies(
            server = listOf(reply(2, 1)),
            local = listOf(reply(55, 2), reply(56, 55)),
            deleted = emptySet(),
        )
        assertEquals(listOf(2L, 55L, 56L), merged.map { it.rpid })
    }

    @Test fun deletedRowsDisappearAndServerCopyOfLocalIsNotDuplicated() {
        val merged = mergeThreadReplies(
            server = listOf(reply(2, 1), reply(3, 1)),
            local = listOf(reply(3, 1), reply(55, 1)),
            deleted = setOf(55L),
        )
        assertEquals(listOf(3L, 2L), merged.map { it.rpid })
    }
}
