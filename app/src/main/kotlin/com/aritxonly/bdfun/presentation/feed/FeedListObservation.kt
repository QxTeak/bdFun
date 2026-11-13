package com.aritxonly.bdfun.ui

import androidx.compose.runtime.snapshotFlow

/** 只暴露"最后一个可见项索引"的小工具。 */
internal fun snapshotFlowLastIndex(listState: androidx.compose.foundation.lazy.LazyListState) =
    androidx.compose.runtime.snapshotFlow {
        listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
    }
