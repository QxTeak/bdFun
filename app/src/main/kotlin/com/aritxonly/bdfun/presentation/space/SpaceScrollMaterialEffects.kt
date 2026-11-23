package com.aritxonly.bdfun

import com.aritxonly.teak.common.chrome.TrackTeakPageScroll

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/** 一次滚动采样：头部收起量、头部高度、首个可见项与其内偏移。 */
private data class SpaceScrollSample(
    val collapsePx: Float,
    val heightPx: Int,
    val index: Int,
    val offset: Int,
)

@Composable
internal fun SpaceScrollMaterialEffects(
    scroll: SpaceScrollContext, progress: SpaceScrollProgress,
    onBarBlurFractionChange: (Float) -> Unit,
) {
    val wideSpace = scroll.layout.wideSpace
    val listState = scroll.listState
    val header = scroll.layout.header
    val headerMaxCollapsePx = scroll.layout.headerMaxCollapsePx
    val blurDensity = scroll.density
    val pageChrome = scroll.pageChrome
    // 模糊进度也是每帧变的：只通知顶栏，不让本函数跟着重组。
    LaunchedEffect(progress) {
        snapshotFlow { progress.blur.value }.distinctUntilChanged()
            .collect { onBarBlurFractionChange(it) }
    }
    if (wideSpace) TrackTeakPageScroll(listState, pageChrome)
    if (!wideSpace) {
        // 原来把这些每帧变化的值当 key 重启协程；改成一条协程持续观察。
        LaunchedEffect(header, listState, pageChrome, headerMaxCollapsePx, blurDensity) {
            snapshotFlow {
                SpaceScrollSample(
                    collapsePx = header.collapsePx,
                    heightPx = header.heightPx,
                    index = listState.firstVisibleItemIndex,
                    offset = listState.firstVisibleItemScrollOffset,
                )
            }.distinctUntilChanged().collect { sample ->
                val collapsePx = sample.collapsePx
                val index = sample.index
                val offset = sample.offset
                val headerDone = headerMaxCollapsePx > 0f && collapsePx >= headerMaxCollapsePx - 1f
                val extra = if (headerDone) offset.toFloat() else 0f
                val target = if (headerDone && index > 0) 1 else 0
                pageChrome.update(target, collapsePx + extra, density = blurDensity.density)
            }
        }
    }

}
