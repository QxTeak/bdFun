package com.aritxonly.bdfun.feature.space.application

internal sealed interface SpacePageRequest {
    data object Feeds : SpacePageRequest
    data class Videos(val page: Int) : SpacePageRequest
    data class Articles(val page: Int) : SpacePageRequest
    data class Bangumi(val page: Int) : SpacePageRequest
}
