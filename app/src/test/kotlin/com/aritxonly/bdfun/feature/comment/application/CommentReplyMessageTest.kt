package com.aritxonly.bdfun.feature.comment.application

import org.junit.Assert.assertEquals
import org.junit.Test

class CommentReplyMessageTest {
    @Test fun replyingToFloorOwnerKeepsText() {
        assertEquals("你好", threadReplyMessage("你好", "楼主", targetRpid = 900L, rootRpid = 900L))
    }

    @Test fun replyingToAnotherReplierPrefixesTarget() {
        assertEquals(
            " 回复 @路人甲 : 你好",
            threadReplyMessage("你好", "路人甲", targetRpid = 901L, rootRpid = 900L),
        )
    }

    @Test fun missingTargetNameFallsBackToPlainText() {
        assertEquals("你好", threadReplyMessage("你好", null, targetRpid = 901L, rootRpid = 900L))
        assertEquals("你好", threadReplyMessage("你好", "   ", targetRpid = 901L, rootRpid = 900L))
    }

    @Test fun unknownTargetKeepsText() {
        assertEquals("你好", threadReplyMessage("你好", "路人甲", targetRpid = 0L, rootRpid = 900L))
    }
}
