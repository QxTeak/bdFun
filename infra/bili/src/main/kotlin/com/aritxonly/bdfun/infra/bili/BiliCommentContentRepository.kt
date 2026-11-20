package com.aritxonly.bdfun.infra.bili

import com.aritxonly.bdfun.core.domain.*

class BiliCommentContentRepository(private val backend: BiliBackend) : CommentContentRepository {
    override suspend fun page(query: CommentQuery, page: Int) = biliOutcome {
        if (query.root > 0) backend.repliesByOid(query.oid, query.type, query.root, page, query.upMid, !query.sortHot)
        else backend.commentsByOid(query.oid, query.type, page, query.upMid, if (query.sortHot) 2 else 0)
    }
    override suspend fun post(post: CommentPost) = biliOutcome {
        backend.addCommentByOid(
            post.query.oid, post.query.type, post.text, post.root, post.parent,
            post.pictures, post.atNameToMid,
        )
    }
    override suspend fun like(query: CommentQuery, rpid: Long, liked: Boolean) = biliOutcome {
        backend.likeCommentByOid(query.oid, query.type, rpid, liked); Unit
    }
    override suspend fun report(query: CommentQuery, rpid: Long) = biliOutcome {
        backend.reportCommentByOid(query.oid, query.type, rpid); Unit
    }
    override suspend fun delete(query: CommentQuery, rpid: Long) = biliOutcome {
        backend.deleteCommentByOid(query.oid, query.type, rpid); Unit
    }
    override suspend fun block(mid: Long) = biliOutcome { backend.blockUser(mid); Unit }
}
