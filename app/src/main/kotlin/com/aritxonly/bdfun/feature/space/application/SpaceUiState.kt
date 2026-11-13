package com.aritxonly.bdfun.feature.space.application

import com.aritxonly.bdfun.core.model.user.*
import com.aritxonly.bdfun.core.model.feed.FeedItem
import com.aritxonly.bdfun.core.model.video.VideoItem

internal data class SpaceContentState<T>(
    val items: List<T> = emptyList(), val page: Int = 1, val end: Boolean = false,
    val loading: Boolean = false, val loaded: Boolean = false, val error: String? = null,
    val revision: Int = 0,
)
internal data class SpaceFeedState(
    val items: List<FeedItem> = emptyList(), val offset: String = "", val end: Boolean = false,
    val loading: Boolean = false, val error: String? = null,
)
internal data class SpaceProfileState(
    val space: SpaceInfo? = null, val followed: Boolean = false, val followBusy: Boolean = false,
    val loading: Boolean = true, val settled: Boolean = false, val error: String? = null,
)
internal data class SpaceUiState(
    val profile: SpaceProfileState = SpaceProfileState(), val seasons: List<SpaceSeason> = emptyList(),
    val videos: SpaceContentState<VideoItem> = SpaceContentState(),
    val articles: SpaceContentState<SpaceArticle> = SpaceContentState(),
    val bangumi: SpaceContentState<SpaceBangumi> = SpaceContentState(),
    val shop: SpaceContentState<SpaceShopItem> = SpaceContentState(),
    val collection: SpaceContentState<VideoItem> = SpaceContentState(),
    val dynamics: SpaceFeedState = SpaceFeedState(),
)
