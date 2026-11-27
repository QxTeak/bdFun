package com.aritxonly.bdfun.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.toIntSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

/** UP 页面切换时旧页滑出 / 新页滑入的时长。和空间页翻页一个量级。 */
private const val FeedUpPushMs = 300

/** 拖动时页面最多跟手多少（占页宽比例），两端留一点阻尼才不会把页面拖丢。 */
private const val FeedUpDragLimit = 0.34f

/** 跟手阻尼：手指走 1px 页面只走这么多，松手后再补完整个推动。 */
private const val FeedUpDragResistance = 0.55f

/** 动画结束后允许的浮点残差：大于它才值得再发一帧。 */
private const val FeedUpOffsetEpsilon = 0.5f

/**
 * 一次推动的两页几何：
 * 旧页从 [dragOffset] 滑到 `-direction * width`，新页从 `dragOffset + direction * width`
 * 滑到 0；两段位移相同，所以拖动中途松手也接得上，不会跳。
 */
internal data class FeedUpPushOffsets(
    val outgoingEnd: Float,
    val contentStart: Float,
    val shift: Float,
)

internal fun feedUpPushOffsets(dragOffset: Float, direction: Int, width: Float): FeedUpPushOffsets {
    val dir = direction.coerceIn(-1, 1)
    return FeedUpPushOffsets(
        outgoingEnd = -dir * width,
        contentStart = dragOffset + dir * width,
        shift = -dir * width - dragOffset,
    )
}

/**
 * UP 之间切换的整页推动。
 *
 * 和空间页的 HorizontalPager 同一个观感：手指拖动时当前页跟手，松手后旧页整页滑出、
 * 新页从另一侧滑入。因为动态页的头像行在滚动列表里、每个 UP 的列表又是异步拉的，
 * 这里不真的挂两个 pager 页，而是在切换瞬间把当前页画成一张快照，用它做「旧页」。
 */
@Stable
internal class FeedUpPageTransition {
    /** 当前（新）页的横向位移；拖动和推动都改这一个值。 */
    var contentOffset by mutableFloatStateOf(0f)
        private set
    /** 旧页快照的横向位移。 */
    var outgoingOffset by mutableFloatStateOf(0f)
        private set
    var outgoing by mutableStateOf<ImageBitmap?>(null)
        private set
    /** 整页淡入用：进入/退出 UP 模式时没有左右方向，只做透明过渡。 */
    var contentAlpha by mutableFloatStateOf(1f)
        private set
    var busy by mutableStateOf(false)
        private set
    var widthPx = 0f

    /** 由宿主注入：抓当前帧的页面快照；失败返回 null，走无快照的降级推动。 */
    internal var capture: (suspend () -> ImageBitmap?)? = null
    private var pending: ImageBitmap? = null
    /** 抓图是逐帧录制的，同一时间只允许一个，避免两次抓图互相把录制状态关掉。 */
    private var capturing = false

    /** 手指按下开始拖：先异步抓一张静止画面，松手要推动时就不用现场等。 */
    fun beginDrag(scope: CoroutineScope) {
        if (busy || capturing || pending != null) return
        contentOffset = 0f
        val grab = capture ?: return
        capturing = true
        scope.launch {
            try {
                pending = grab()
            } finally {
                capturing = false
            }
        }
    }

    fun dragBy(delta: Float) {
        if (busy) return
        val limit = (widthPx * FeedUpDragLimit).coerceAtLeast(1f)
        contentOffset = (contentOffset + delta * FeedUpDragResistance).coerceIn(-limit, limit)
    }

    /**
     * 松手结算。[direction] 为 +1 时新页从右侧进、旧页往左出；-1 反之。
     * [commit] 为 null 表示没过阈值：只回弹，不换页。
     *
     * 返回 false 表示上一次推动还没播完，调用方应当直接换页而不是把这次操作吞掉。
     */
    suspend fun settle(direction: Int, commit: (suspend () -> Unit)?): Boolean {
        if (busy) return false
        val width = widthPx
        if (commit == null || width <= 0f) {
            slideBack()
            return true
        }
        busy = true
        val start = contentOffset
        val dir = direction.coerceIn(-1, 1)
        try {
            // 拖动开始时可能已经在抓图了：等它结束再用，别同时录两次。
            while (capturing) withFrameNanos { }
            val snapshot = pending ?: capture?.invoke()
            pending = null
            if (snapshot == null) {
                // 抓图失败（首帧还没画过等）：不出旧页，只把新页从正确一侧推入。
                contentOffset = dir * width
                commit()
                animate(0f, 1f, animationSpec = tween(FeedUpPushMs, easing = FastOutSlowInEasing)) { p, _ ->
                    contentOffset = dir * width * (1f - p)
                }
            } else {
                val push = feedUpPushOffsets(start, dir, width)
                outgoing = snapshot
                outgoingOffset = start
                contentOffset = push.contentStart
                commit()
                animate(0f, 1f, animationSpec = tween(FeedUpPushMs, easing = FastOutSlowInEasing)) { p, _ ->
                    outgoingOffset = start + push.shift * p
                    contentOffset = push.contentStart + push.shift * p
                }
            }
        } finally {
            outgoing = null
            outgoingOffset = 0f
            contentOffset = 0f
            busy = false
        }
        return true
    }

    /** 没有左右方向的切换（进入/退出 UP 模式）：只淡入，不位移。 */
    suspend fun fadeSwap(commit: suspend () -> Unit): Boolean {
        if (busy) return false
        busy = true
        try {
            contentAlpha = 0f
            commit()
            animate(0f, 1f, animationSpec = tween(220, easing = FastOutSlowInEasing)) { p, _ ->
                contentAlpha = p
            }
        } finally {
            contentAlpha = 1f
            busy = false
        }
        return true
    }

    /** 没过阈值 / 拖到一半取消：用同一段缓动退回原位。 */
    private suspend fun slideBack() {
        pending = null
        val start = contentOffset
        if (abs(start) < FeedUpOffsetEpsilon) {
            contentOffset = 0f
            return
        }
        animate(0f, 1f, animationSpec = tween(200, easing = FastOutSlowInEasing)) { p, _ ->
            contentOffset = start * (1f - p)
        }
        contentOffset = 0f
    }
}

/**
 * 页面内容 + 推动动画的宿主。
 *
 * [content] 被画进一个可录制的图层：切换时录一帧给旧页快照，之后旧页快照在上层滑出、
 * 真实内容（新页）在下层从另一侧滑入。裁剪保证滑动中的页不会画到顶栏/底栏上。
 */
@Composable
internal fun FeedUpPageTransitionHost(
    transition: FeedUpPageTransition,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val layer = rememberGraphicsLayer()
    var recording by remember { mutableStateOf(false) }
    val capture = remember(layer) {
        suspend {
            recording = true
            try {
                // 先让这一帧把内容录进图层，再取位图。
                withFrameNanos { }
                withFrameNanos { }
                runCatching { layer.toImageBitmap() }.getOrNull()
                    ?.takeIf { it.width > 0 && it.height > 0 }
            } finally {
                recording = false
            }
        }
    }
    SideEffect { transition.capture = capture }
    Box(modifier.fillMaxSize().clipToBounds()) {
        Box(
            Modifier
                .fillMaxSize()
                .onSizeChanged { transition.widthPx = it.width.toFloat() }
                .graphicsLayer {
                    translationX = transition.contentOffset
                    alpha = transition.contentAlpha
                },
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .drawWithContent {
                        if (recording) {
                            layer.record(
                                density = this,
                                layoutDirection = layoutDirection,
                                size = size.toIntSize(),
                            ) { this@drawWithContent.drawContent() }
                        }
                        drawContent()
                    },
            ) {
                content()
            }
        }
        transition.outgoing?.let { image ->
            Image(
                bitmap = image,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { translationX = transition.outgoingOffset },
            )
        }
    }
}
