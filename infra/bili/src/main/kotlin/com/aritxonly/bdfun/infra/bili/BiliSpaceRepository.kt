package com.aritxonly.bdfun.infra.bili

import com.aritxonly.bdfun.core.domain.SpaceRepository
import com.aritxonly.bdfun.core.model.user.SpaceSeason

class BiliSpaceRepository(private val backend: BiliBackend) : SpaceRepository {
    override suspend fun profile(mid: Long) = biliOutcome { backend.space(mid) }
    override suspend fun seasons(mid: Long) = biliOutcome { backend.spaceSeasonsSeries(mid) }
    override suspend fun dynamics(mid: Long, offset: String) = biliOutcome { backend.userFeeds(mid, offset) }
    override suspend fun relation(mid: Long) = biliOutcome { backend.relation(mid) }
    override suspend fun videos(mid: Long, page: Int) = biliOutcome { backend.spaceVideos(mid, page) }
    override suspend fun articles(mid: Long, page: Int) = biliOutcome { backend.spaceArticles(mid, page) }
    override suspend fun bangumi(mid: Long, page: Int) = biliOutcome { backend.spaceBangumi(mid, page) }
    override suspend fun shop(mid: Long) = biliOutcome { backend.spaceShop(mid) }
    override suspend fun collection(mid: Long, collection: SpaceSeason) = biliOutcome { backend.spaceCollectionArchives(mid, collection) }
    override suspend fun follow(mid: Long, followed: Boolean) = biliOutcome { backend.followUser(mid, followed) }
}
