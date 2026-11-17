package com.aritxonly.bdfun.feature.home.presentation

import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import com.aritxonly.teak.common.skeleton.TeakSkeletonVideoCard
import com.aritxonly.teak.common.skeleton.rememberTeakSkeletonSweep

/**
 * 首页/推荐首屏骨架：按真实双列视频卡的尺寸摆 8 张。
 * 骨架直接进网格，数据到了原位替换，不会先出现一个转圈再整页跳一下。
 */
internal fun LazyGridScope.homeFeedSkeletonItems() {
    items(count = 8, key = { "home-skeleton-$it" }) {
        HomeFeedSkeletonCard()
    }
}

@Composable
private fun HomeFeedSkeletonCard() {
    TeakSkeletonVideoCard(progress = rememberTeakSkeletonSweep())
}
