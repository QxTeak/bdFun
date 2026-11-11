package com.aritxonly.bdfun.feature.library.application

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 同一视频的收藏变更串行提交，避免快速点击用过期状态反复提交。 */
internal class FavoriteFolderOperations {
    private val mutablePending = MutableStateFlow<Long?>(null)
    val pending: StateFlow<Long?> = mutablePending.asStateFlow()

    fun tryStart(folderId: Long): Boolean = mutablePending.compareAndSet(null, folderId)
    fun finish(folderId: Long) { mutablePending.compareAndSet(folderId, null) }
}
