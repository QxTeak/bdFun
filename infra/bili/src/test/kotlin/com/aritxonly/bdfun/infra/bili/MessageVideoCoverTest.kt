package com.aritxonly.bdfun.infra.bili

import com.aritxonly.bdfun.core.model.message.*
import org.junit.Assert.*
import org.junit.Test

class MessageVideoCoverTest {
    private fun item(navigation: MessageNavigation = MessageNavigation(),
        presentation: MessagePresentation = MessagePresentation()) =
        MessageItem("1", "", "回复正文", 0, "用户", "", navigation, presentation)

    @Test fun dynamicSubjectIdIsNeverTreatedAsVideoAid() {
        assertEquals("", messageVideoCoverTarget(item(MessageNavigation(aid = 123, businessId = 17))))
        assertEquals("", messageVideoCoverTarget(item(MessageNavigation(aid = 123))))
    }

    @Test fun replyMentionAndCommentLikeCanAllResolveOriginalVideo() {
        for (presentation in listOf(MessagePresentation(), MessagePresentation(atMe = true),
            MessagePresentation(action = "赞了我的评论"))) {
            val target = messageVideoCoverTarget(item(MessageNavigation(aid = 170001, businessId = 1), presentation))
            assertTrue(target.startsWith("BV"))
        }
        assertEquals("BV17x411w7KC", messageVideoCoverTarget(item(MessageNavigation(bvid = "BV17x411w7KC"))))
    }

    @Test fun nativeVideoLinkIsExplicitEvidenceButDynamicLinkIsNot() {
        assertTrue(messageVideoCoverTarget(item(MessageNavigation(aid = 170001,
            nativeUri = "bilibili://video/170001/?comment_root_id=45"))).startsWith("BV"))
        assertEquals("", messageVideoCoverTarget(item(MessageNavigation(aid = 170001,
            nativeUri = "bilibili://opus/170001"))))
    }

    @Test fun enrichmentKeepsOriginalCommentImageAndTextSeparateFromVideoCover() {
        val original = item(presentation = MessagePresentation(quote = "我的评论", subjectCover = "comment-image"))
        val enriched = applyMessageVideoBrief(original, "BV17x411w7KC", "video-cover", 9)
        assertEquals("video-cover", enriched.presentation.originalVideoCover)
        assertEquals("comment-image", enriched.subjectCover)
        assertEquals("我的评论", enriched.quote)
        assertEquals(original.content, enriched.content)
    }
}
