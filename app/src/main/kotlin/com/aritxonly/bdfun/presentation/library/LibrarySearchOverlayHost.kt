package com.aritxonly.bdfun.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 媒体库搜索层的跨层状态。
 *
 * 搜索页必须画在页面顶栏之下（顶栏按进度淡出），所以不能再塞进
 * `@Composable` 闭包状态里：闭包在退出动画结束后不会自动失效，会一直
 * 盖住列表、吞掉滑动。这里只暴露普通状态，由 Activity 自己组合搜索页。
 */
internal class LibrarySearchOverlayHost {
    /** 当前页面的运行时；搜索页直接读它的 StateFlow，数据变化能即时刷新。 */
    var runtime: LibraryRuntime? by mutableStateOf(null)
    /** 搜索是否展开（不含退出动画），用于驱动底层退让和顶栏淡出。 */
    var searching by mutableStateOf(false)
}
