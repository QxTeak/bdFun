package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.model.comment.CommentPage

data class CommentQuery(val oid: String, val type: Int, val upMid: Long, val root: Long = 0, val sortHot: Boolean = true)
data class CommentPost(
    val query: CommentQuery, val text: String, val root: Long, val parent: Long,
    val pictures: String? = null,
    /**
     * 正文里 @ 的名字 → 对方 mid。
     *
     * 回复楼里的某个人时，只把「回复 @某人」写进正文，服务端会当成普通文字：
     * 对方收不到真正的 @，这段文字在客户端里也点不开。带上这张表，服务端才会
     * 把这段文字登记成提及（官方网页和 PiliPlus 都这么传）。
     */
    val atNameToMid: Map<String, Long> = emptyMap(),
)

interface CommentContentRepository {
    suspend fun page(query: CommentQuery, page: Int): Outcome<CommentPage>
    suspend fun post(post: CommentPost): Outcome<Long?>
    suspend fun like(query: CommentQuery, rpid: Long, liked: Boolean): Outcome<Unit>
    suspend fun report(query: CommentQuery, rpid: Long): Outcome<Unit>
    suspend fun delete(query: CommentQuery, rpid: Long): Outcome<Unit>
    suspend fun block(mid: Long): Outcome<Unit>
}
