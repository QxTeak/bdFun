package com.aritxonly.bdfun.presentation.common.discover

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue

/**
 * 直播 / 番剧页的下拉位移；手势与补间已统一到
 * [com.aritxonly.bdfun.feature.home.presentation.PullRefreshSession]。
 */
internal class DiscoverPullUi {
    var offset by mutableFloatStateOf(0f)
}
