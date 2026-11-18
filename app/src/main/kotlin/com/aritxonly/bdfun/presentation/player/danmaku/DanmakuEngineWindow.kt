package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.android.purebilibili.danmaku.engine.DANMAKU_LAYER_BOTTOM
import com.android.purebilibili.danmaku.engine.DANMAKU_LAYER_SCROLL
import com.android.purebilibili.danmaku.engine.DANMAKU_LAYER_TOP
import com.android.purebilibili.danmaku.engine.DanmakuItem
import com.android.purebilibili.danmaku.engine.DanmakuTrailingAction
import com.android.purebilibili.danmaku.engine.DanmakuWindow
import com.aritxonly.bdfun.core.domain.isHighLikeScrollingDanmaku
import com.aritxonly.bdfun.core.model.video.DanmakuItem as BiliDanmakuItem


@Composable
internal fun rememberDanmakuEngineWindow(filtered: List<BiliDanmakuItem>, likeAction: DanmakuTrailingAction): DanmakuWindow {
    val engineItems = remember(filtered, likeAction) {
        filtered.map { item ->
            DanmakuItem().apply {
                // 与 BiliPai 一致：服务端合并过的重复弹幕在正文后显示 xN。
                text = if (item.count > 1) "${item.text} x${item.count}" else item.text
                showAtTime = item.timeMs.coerceAtLeast(0L)
                layerType = when (item.mode) {
                    4 -> DANMAKU_LAYER_BOTTOM
                    5 -> DANMAKU_LAYER_TOP
                    6 -> com.android.purebilibili.danmaku.engine.DANMAKU_LAYER_REVERSE
                    else -> DANMAKU_LAYER_SCROLL
                }
                textColor = 0xFF000000.toInt() or item.color
                textSizeScale = danmakuItemTextScale(item.size)
                isSelf = item.self
                isVipGradualColor = item.vipColor
                isUpOwner = item.upIdentity
                userHash = item.userHash
                mergedOriginalText = item.text
                if (item.id > 0L) danmakuId = item.id
                likeCount = item.likeCount
                if (isHighLikeScrollingDanmaku(item)) trailingAction = likeAction
                duplicateCount = item.count
            }
        }
    }
    return remember(engineItems) {
        DanmakuWindow(anchorSegment = 0, segmentIndices = emptyList(), items = engineItems)
    }

}
