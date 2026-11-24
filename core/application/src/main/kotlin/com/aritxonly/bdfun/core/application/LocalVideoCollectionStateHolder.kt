package com.aritxonly.bdfun.core.application

import com.aritxonly.bdfun.core.model.video.VideoItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class LocalVideoCollection(
    val bvid: String = "",
    val items: List<VideoItem> = emptyList(),
    val title: String = "",
)

/**
 * 只属于本地入口的临时合集（例如从稍后再看整页进入）。
 * 和播放队列分开保存，避免账号刷新清空队列时把合集也一起丢掉。
 */
class LocalVideoCollectionStateHolder {
    private val mutableState = MutableStateFlow(LocalVideoCollection())
    val state = mutableState.asStateFlow()

    fun set(bvid: String, items: List<VideoItem>, title: String) {
        mutableState.value = LocalVideoCollection(bvid, items.toList(), title)
    }

    fun forBvid(bvid: String): LocalVideoCollection =
        state.value.takeIf { it.bvid == bvid && it.items.size > 1 } ?: LocalVideoCollection()

    fun clear() { mutableState.value = LocalVideoCollection() }
}
