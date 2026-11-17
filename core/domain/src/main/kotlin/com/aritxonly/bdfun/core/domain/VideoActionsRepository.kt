package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.model.library.FavoriteFolder

interface VideoActionsRepository {
    suspend fun like(aid: Long, enabled: Boolean): Outcome<Unit>
    suspend fun dislike(aid: Long, enabled: Boolean): Outcome<Unit>
    /** 投币；[alsoLike] 对应 B 站 `select_like`，投币时同时点赞。 */
    suspend fun coin(aid: Long, count: Int, alsoLike: Boolean = false): Outcome<Unit>
    suspend fun follow(mid: Long, enabled: Boolean): Outcome<Unit>
    suspend fun watchLater(aid: Long): Outcome<Boolean>
    suspend fun folders(): Outcome<List<FavoriteFolder>>
    suspend fun folderIds(aid: Long): Outcome<Set<Long>>
    suspend fun setFavorite(aid: Long, folderId: Long, enabled: Boolean): Outcome<Boolean>
    suspend fun removeFavorites(aid: Long, folderIds: Set<Long>): Outcome<Unit>
}
