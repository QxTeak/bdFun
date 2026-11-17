package com.aritxonly.bdfun.infra.bili

import com.aritxonly.bdfun.core.domain.VideoActionsRepository

class BiliVideoActionsRepository(private val backend: BiliBackend) : VideoActionsRepository {
    override suspend fun like(aid: Long, enabled: Boolean) = biliOutcome { backend.likeVideo(aid, enabled); Unit }
    override suspend fun dislike(aid: Long, enabled: Boolean) = biliOutcome { backend.dislikeVideo(aid, enabled); Unit }
    override suspend fun coin(aid: Long, count: Int, alsoLike: Boolean) =
        biliOutcome { backend.coinVideo(aid, count, alsoLike); Unit }
    override suspend fun follow(mid: Long, enabled: Boolean) = biliOutcome { backend.followUser(mid, enabled); Unit }
    override suspend fun watchLater(aid: Long) = biliOutcome { backend.addWatchLater(aid) }
    override suspend fun folders() = biliOutcome { backend.favoriteFolders() }
    override suspend fun folderIds(aid: Long) = biliOutcome { backend.favoriteFolderIds(aid) }
    override suspend fun setFavorite(aid: Long, folderId: Long, enabled: Boolean) =
        biliOutcome { backend.setFavorite(aid, folderId, enabled) }
    override suspend fun removeFavorites(aid: Long, folderIds: Set<Long>) =
        biliOutcome { backend.removeFavoriteAll(aid, folderIds); Unit }
}
