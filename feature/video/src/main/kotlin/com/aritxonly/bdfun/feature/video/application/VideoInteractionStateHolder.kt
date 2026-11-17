package com.aritxonly.bdfun.feature.video.application

import com.aritxonly.bdfun.core.common.*
import com.aritxonly.bdfun.core.domain.VideoActionsRepository
import com.aritxonly.bdfun.core.model.library.FavoriteFolder
import com.aritxonly.bdfun.core.model.video.VideoDetail
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class VideoInteractionState(
    val detail: VideoDetail? = null,
    val liked: Boolean = false, val disliked: Boolean = false, val likeCount: Long = 0,
    val coined: Boolean = false, val coinCount: Long = 0,
    val favorited: Boolean = false, val favoriteCount: Long = 0, val shareCount: Long = 0,
    val followed: Boolean = false,
    val favorites: VideoFavoriteState = VideoFavoriteState(), val busy: Boolean = false,
) {
    val folders get() = favorites.folders
    val folderIds get() = favorites.folderIds
    val favoritesLoading get() = favorites.favoritesLoading
    val favoritesLoaded get() = favorites.favoritesLoaded
    val pendingFolder get() = favorites.pendingFolder
}

data class VideoFavoriteState(
    val folders: List<FavoriteFolder> = emptyList(), val folderIds: Set<Long> = emptySet(),
    val favoritesLoading: Boolean = false, val favoritesLoaded: Boolean = false,
    val pendingFolder: Long? = null,
)

/** 写操作按视频隔离；失败或取消撤销乐观状态，旧单集响应不能回写当前单集。 */
class VideoInteractionStateHolder(private val repository: VideoActionsRepository) {
    private val mutableState = MutableStateFlow(VideoInteractionState())
    val state = mutableState.asStateFlow()
    private var revision = 0L
    private var relationRevision = 0L

    fun bind(detail: VideoDetail) {
        if (mutableState.value.detail?.bvid == detail.bvid) return
        revision++
        relationRevision++
        mutableState.value = VideoInteractionState(
            detail, detail.liked, false, detail.like, detail.coined, detail.coin,
            detail.favorited, detail.favorite, detail.share, detail.isFollowed,
        )
    }

    fun recordShare() {
        if (mutableState.value.detail != null) mutableState.update { it.copy(shareCount = it.shareCount + 1) }
    }

    fun relationToken(): Long = relationRevision
    fun restoreFollowed(value: Boolean, token: Long) {
        if (token == relationRevision) mutableState.update { it.copy(followed = value) }
    }

    suspend fun loadFavorites() {
        val initial = mutableState.value
        val detail = initial.detail ?: return
        if (initial.favoritesLoading || initial.favoritesLoaded) return
        val version = revision
        mutableState.update { it.copy(
            favorites = it.favorites.copy(
                favoritesLoading = true,
            ),
        ) }
        try {
            val folders = repository.folders()
            if (folders !is Outcome.Success) return
            val ids = repository.folderIds(detail.aid)
            if (version == revision) mutableState.update { it.copy(
                favorites = it.favorites.copy(
                    folders = folders.value,
                    folderIds = (ids as? Outcome.Success)?.value.orEmpty(),
                    favoritesLoaded = true,
                ),
            ) }
        } finally {
            if (version == revision) mutableState.update { it.copy(
                favorites = it.favorites.copy(
                    favoritesLoading = false,
                ),
            ) }
        }
    }

    suspend fun toggleLike(): String? = optimistic(
        { it.copy(liked = !it.liked, disliked = false,
            likeCount = (it.likeCount + if (it.liked) -1 else 1).coerceAtLeast(0)) },
        { repository.like(it.detail!!.aid, it.liked) },
        { if (it.liked) "点赞成功" else "已取消赞" }, "点赞失败",
    )

    suspend fun toggleDislike(): String? = optimistic(
        { it.copy(disliked = !it.disliked, liked = if (!it.disliked) false else it.liked,
            likeCount = (it.likeCount - if (!it.disliked && it.liked) 1 else 0).coerceAtLeast(0)) },
        { repository.dislike(it.detail!!.aid, it.disliked) },
        { if (it.disliked) "感谢反馈" else "已取消反馈" }, "操作失败",
    )

    suspend fun toggleFollow(mid: Long): String? {
        relationRevision++
        return optimistic({ it.copy(followed = !it.followed) },
            { repository.follow(mid, it.followed) },
            { if (it.followed) "关注成功" else "已取消关注" }, "操作失败")
    }

    private suspend fun optimistic(
        transform: (VideoInteractionState) -> VideoInteractionState,
        action: suspend (VideoInteractionState) -> Outcome<Unit>,
        message: (VideoInteractionState) -> String,
        fallback: String,
    ): String? {
        val before = mutableState.value
        if (before.detail == null || before.busy) return null
        val version = revision
        val next = transform(before)
        mutableState.value = next.copy(busy = true)
        try {
            val result = action(next)
            if (version != revision) return null
            return when (result) {
                is Outcome.Success -> message(next)
                is Outcome.Failure -> { mutableState.value = before; result.error.messageOr(fallback) }
            }
        } catch (cancelled: CancellationException) {
            if (version == revision) mutableState.value = before
            throw cancelled
        } finally {
            if (version == revision) mutableState.update { it.copy(busy = false) }
        }
    }

    private suspend fun perform(action: suspend (VideoInteractionState, Long) -> String): String? {
        val before = mutableState.value
        if (before.busy || before.detail == null) return null
        val version = revision
        mutableState.update { it.copy(busy = true) }
        try { return action(before, version).takeIf { version == revision } }
        finally { if (version == revision) mutableState.update { it.copy(
            busy = false,
            favorites = it.favorites.copy(
                pendingFolder = null,
            ),
        ) } }
    }

    suspend fun coin(count: Int, alsoLike: Boolean = false): String? = perform { before, version ->
        val result = repository.coin(before.detail!!.aid, count, alsoLike)
        if (result is Outcome.Success && version == revision) {
            mutableState.update {
                // select_like=1 时服务端会顺带点赞，界面要跟上，否则看起来没生效。
                val likeDelta = if (alsoLike && !it.liked) 1 else 0
                it.copy(
                    coined = true,
                    coinCount = it.coinCount + count,
                    liked = it.liked || alsoLike,
                    likeCount = it.likeCount + likeDelta,
                )
            }
        }
        if (result is Outcome.Success) "投币成功" else "投币失败"
    }

    suspend fun watchLater(): String? = perform { before, _ ->
        val result = repository.watchLater(before.detail!!.aid)
        if (result is Outcome.Success && result.value) "已添加到稍后再看" else "添加稍后再看失败"
    }

    suspend fun toggleFolder(folderId: Long): String? = perform { before, version ->
        val contains = folderId in before.folderIds
        mutableState.update { it.copy(
            favorites = it.favorites.copy(
                pendingFolder = folderId,
            ),
        ) }
        val result = repository.setFavorite(before.detail!!.aid, folderId, !contains)
        if (result is Outcome.Success && result.value && version == revision) {
            mutableState.update { it.copy(
                favorites = it.favorites.copy(
                    folderIds = if (contains) it.folderIds - folderId else it.folderIds + folderId,
                ),
                favorited = if (contains) (it.folderIds - folderId).isNotEmpty() else true,
                favoriteCount = (it.favoriteCount + if (contains) -1 else 1).coerceAtLeast(0),
            ) }
            if (contains) "已取消收藏" else "已收藏"
        } else if (contains) "取消收藏失败" else "收藏失败"
    }

    suspend fun removeFavorites(): String? = perform { before, version ->
        val aid = before.detail!!.aid
        val ids = before.folderIds.ifEmpty { (repository.folderIds(aid) as? Outcome.Success)?.value.orEmpty() }
        if (ids.isEmpty()) return@perform "取消收藏失败"
        val result = repository.removeFavorites(aid, ids)
        var anyOk = result is Outcome.Success
        if (!anyOk) {
            // 保持旧版批量失败时逐夹回退的顺序，取消不会被解释为成功。
            ids.forEach { if (repository.setFavorite(aid, it, false) is Outcome.Success) anyOk = true }
        }
        if (anyOk && version == revision) {
            mutableState.update { it.copy(
                favorites = it.favorites.copy(
                    folderIds = emptySet(),
                ),
                favorited = false,
                favoriteCount = (it.favoriteCount - 1).coerceAtLeast(0),
            ) }
            "已取消收藏"
        } else "取消收藏失败：${(result as? Outcome.Failure)?.error?.messageOr("未知错误") ?: "未知错误"}"
    }

    suspend fun tripleAction(): String? = perform { before, version ->
        val aid = before.detail!!.aid
        var ok = true
        if (!before.liked) {
            val result = repository.like(aid, true)
            if (result is Outcome.Success && version == revision) mutableState.update {
                it.copy(liked = true, likeCount = it.likeCount + 1, disliked = false)
            } else ok = false
        }
        if (version != revision) return@perform ""
        if (!before.coined) {
            val result = repository.coin(aid, 2)
            if (result is Outcome.Success && version == revision) mutableState.update {
                it.copy(coined = true, coinCount = it.coinCount + 2)
            } else ok = false
        }
        if (version != revision) return@perform ""
        if (!before.favorited) {
            val folder = before.folders.firstOrNull() ?: (repository.folders() as? Outcome.Success)?.value?.firstOrNull()
            if (folder != null && version == revision) {
                val result = repository.setFavorite(aid, folder.id, true)
                if (result is Outcome.Success && version == revision) mutableState.update {
                    it.copy(
                        favorited = true,
                        favorites = it.favorites.copy(
                            folderIds = it.folderIds + folder.id,
                        ),
                        favoriteCount = it.favoriteCount + 1,
                    )
                } else ok = false
            } else ok = false
        }
        if (ok) "已一键三连" else "一键三连部分失败"
    }
}
