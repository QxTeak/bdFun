package com.aritxonly.bdfun.infra.bili

import com.aritxonly.bdfun.core.common.AppError
import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.domain.*
import com.aritxonly.bdfun.core.model.comment.CommentPage
import com.aritxonly.bdfun.core.model.feed.FeedPage
import com.aritxonly.bdfun.core.model.user.*
import com.aritxonly.bdfun.core.model.video.*
import kotlinx.coroutines.CancellationException
import java.io.IOException

/** 回调由启动层装配，仓储不反向依赖 app，也便于离线替换。 */
class BiliVideoRepository(
    private val detail: suspend (String) -> VideoDetail,
    private val relatedVideos: suspend (String) -> List<VideoItem>,
) : VideoRepository {
    override suspend fun videoDetail(bvid: String) = biliOutcome { detail(bvid) }
    override suspend fun related(bvid: String) = biliOutcome { relatedVideos(bvid) }
}

class BiliCommentRepository(
    private val loadComments: suspend (String, Int, Int) -> CommentPage,
    private val loadReplies: suspend (String, Int, Long, Int) -> CommentPage,
) : CommentRepository {
    override suspend fun comments(oid: String, type: Int, page: Int) = biliOutcome { loadComments(oid, type, page) }
    override suspend fun replies(oid: String, type: Int, root: Long, page: Int) =
        biliOutcome { loadReplies(oid, type, root, page) }
}

class BiliFeedRepository(
    private val loadRecommendations: suspend (Int) -> List<VideoItem>,
    private val loadFeeds: suspend (String) -> FeedPage,
) : FeedRepository {
    override suspend fun recommend(page: Int) = biliOutcome { loadRecommendations(page) }
    override suspend fun dynamics(offset: String) = biliOutcome { loadFeeds(offset) }
}

class BiliAccountRepository(
    private val loadProfile: suspend () -> UserProfile?,
    private val createQr: suspend () -> QrLogin,
    private val queryQr: suspend (String) -> QrPoll,
    private val clearSession: () -> Unit,
) : AccountRepository {
    override suspend fun profile() = biliOutcome { loadProfile() }
    override suspend fun generateQr() = biliOutcome { createQr() }
    override suspend fun pollQr(key: String) = biliOutcome { queryQr(key) }
    override fun logout() = clearSession()
}

internal suspend fun <T> biliOutcome(block: suspend () -> T): Outcome<T> = try {
    Outcome.Success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: BiliException) {
    val message = e.message
    val error = when {
        e.code == -101 -> AppError.LoginExpired
        e.code == -352 || e.code == -412 -> AppError.RiskControl(message)
        message == "数据解析失败" || message == "服务器返回为空" -> AppError.Parsing(message)
        e.code == -1 || e.code in 400..599 -> AppError.Network(message)
        else -> AppError.Unknown(message)
    }
    Outcome.Failure(error)
} catch (e: IOException) {
    Outcome.Failure(AppError.Network(e.message.orEmpty()))
} catch (e: Exception) {
    Outcome.Failure(AppError.Unknown(e.message.orEmpty()))
}
