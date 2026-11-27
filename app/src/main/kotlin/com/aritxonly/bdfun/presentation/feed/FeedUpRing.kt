package com.aritxonly.bdfun.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.BuildConfig
import com.aritxonly.bdfun.data.LogCollector
import com.aritxonly.teak.common.chrome.TeakMotion
import kotlin.math.abs

/** 「全部动态」头像在环里的 key；UP 头像直接用 mid。 */
internal const val FeedAllAvatarKey = "feed-all"

/** 环从一颗头像解体重组到另一颗的总时长。太短看不出"缠绕"，太长会挡下一次操作。 */
private const val FeedRingMoveMs = 440

/**
 * 快起慢收：点击后立刻动起来（前 20% 已经走掉大半距离），尾巴慢慢收进目标圆。
 * 旧的 `FastOutSlowIn` 前 70ms 几乎不动，点完会有"延迟一下"的错觉。
 */
private val FeedRingEasing = CubicBezierEasing(0.18f, 0f, 0.08f, 1f)

/**
 * 动态页头像的选中环。
 *
 * 不画在每颗头像自己身上，而是整个头像行共用一条环：换选中项时，环从源头像底部
 * 解体成一条线向右（或向左）走，到目标头像底部再绕上去（右移→从右侧起绕）。
 * 实现是两段 `drawArc` + `drawLine`：前半段源环 sweep 从 360 收到 0、线伸出；
 * 后半段线收进目标底部、目标环 sweep 从 0 涨到 360。
 *
 * 源头像已经滑出屏幕时只画线：起点放在**被记住的那一侧屏幕外**，线从边缘滑进来，
 * 不再把旧坐标夹回屏幕内。
 *
 * 绘制只读状态、不读写组合：动画期间只有这一个 Canvas 重绘，不会重组头像行。
 */
@Stable
internal class FeedAvatarRingState {
    /**
     * 每颗头像的矩形（root 坐标）。用普通 HashMap：头像栏滚动时每帧每颗头像都会重新测量，
     * 写进快照 Map 会为每次写做一次快照记账；这里只在"被画的那几个 key"变了时才 bump 版本，
     * 其余位置默默更新，等它们变成选中/动画端点时直接读到最新值。
     */
    internal val anchors = HashMap<Any, Rect>()
    /** 只读版本号：绘制阶段读它来订阅"相关位置变了"，避免整行滚动都触发环重绘。 */
    internal var anchorsVersion by mutableStateOf(0)
        private set
    /** 覆盖层自己在 root 里的左上角；绘制时减掉它，锚点才能统一用 root 坐标测。 */
    internal var originInRoot by mutableStateOf(Offset.Zero)
    internal var selectedKey: Any by mutableStateOf(FeedAllAvatarKey)
    /** 覆盖层宽度（px）。头像被回收时用它判断是从左边还是右边滑出去的。 */
    internal var viewportWidth = 0f
        private set

    /** 动画两端冻结的矩形，作为锚点被回收时的兜底。 */
    internal var from by mutableStateOf<Rect?>(null)
    internal var to by mutableStateOf<Rect?>(null)
    /** 动画两端是谁；绘制时优先取它们的实时位置，头像栏滚动时线才跟得上。 */
    private var fromKey: Any? = null
    private var toKey: Any? = null
    internal val progress = Animatable(1f)

    /** 首次组合只落位不播动画，避免恢复已选 UP 时从头绕一圈。 */
    private var primed = false

    /**
     * 选中头像滚出视口后，它的锚点会被回收；下一次切换仍要从它"消失的位置"出发。
     * 这里以**卡片内坐标**留一份最后位置——用 root 坐标存的话，之后整个动态列表再上下滚动，
     * 旧 root 坐标就会指到卡片上方/下方去。
     */
    private var lastSelectedLocal: Rect? = null
    /** 选中头像最后一次可见时是朝哪一侧滑出去的；它才是"屏幕外方向"的真实记忆。 */
    private var lastSelectedSide: FeedRingSide? = null
    /** 选中头像的最后直径：起点圆按它画，线的位置才不会突然变粗变细。 */
    private var lastSelectedSize = 0f

    /** 环现在画在哪（root 坐标）；换选中项前用它冻结起点，头像栏滚动也不会把起点弄丢。 */
    internal fun currentRect(): Rect? =
        fromKey?.let { anchors[it] } ?: anchors[selectedKey]
            ?: lastSelectedLocal?.translate(originInRoot.x, originInRoot.y)

    /** 选一个已知头像当尺寸/行高参考：目标还没滚进视口时用它估算位置。 */
    internal fun referenceAvatarRect(): Rect? =
        anchors[selectedKey]?.toLocal() ?: lastSelectedLocal ?: anchors[FeedAllAvatarKey]?.toLocal()

    /** 选中头像现在还有没有实时测量点（头像栏滚动后据此决定用实测位置还是屏幕外起点）。 */
    internal fun hasLiveAnchor(key: Any = selectedKey): Boolean = anchors[key] != null

    /**
     * 直接把环落在 [target] 上，不播「解体重组」动画。
     *
     * UP 页是第一帧就带着环出现的：那里没有"从上一颗头像绕过来"的过程，
     * 环绕动画只属于这一页里切换 UP（左右滑 / 点头像）。
     */
    internal suspend fun snapTo(target: Any) {
        primed = true
        selectedKey = target
        fromKey = null
        toKey = null
        from = null
        to = null
        progress.snapTo(1f)
    }

    internal fun onViewportWidth(width: Int) {
        viewportWidth = width.toFloat()
    }

    /**
     * 选中项变化时调用：解体重组到 [target]。
     * [source] 是调用方在滚动头像栏之前冻结的起点；为空时退回到当前锚点。
     * [scrollDelta] 是本次切换为了让目标完整露出而滚动的方向：正数=内容左移（源头像从左边出），
     * 负数=内容右移。源头像在滚动后被回收时，只有它才能说明真实方向。
     * [estimatedTarget] 是目标还没被测量时（还在屏幕外）的估算位置，让环能和头像行同时出发。
     */
    internal suspend fun moveTo(
        target: Any,
        source: Rect? = null,
        scrollDelta: Int = 0,
        estimatedTarget: Rect? = null,
    ) {
        if (!primed) {
            primed = true
            selectedKey = target
            progress.snapTo(1f)
            return
        }
        if (target == selectedKey && progress.value >= 1f) return
        // 目标可能刚被滚进视口、这一帧还没测量到：有估算就先用估算，不然最多等两帧。
        var toRect = anchors[target]?.toLocal() ?: estimatedTarget
        var waits = 0
        while (toRect == null && waits < 2) {
            withFrameNanos { }
            toRect = anchors[target]?.toLocal() ?: estimatedTarget
            waits++
        }
        val measuredStart = anchors[selectedKey]?.toLocal()
        val frozenStart = measuredStart ?: source?.translate(-originInRoot.x, -originInRoot.y)
            ?: lastSelectedLocal
        if (BuildConfig.DEBUG) {
            LogCollector.record(
                "FEED_RING",
                "moveTo target=$target from=${frozenStart.desc()} to=${toRect.desc()} " +
                    "scroll=$scrollDelta side=$lastSelectedSide live=${measuredStart != null}",
            )
        }
        if (!TeakMotion.enabled || toRect == null) {
            // 目标还没测量到（比如刚滚进视口）或关掉了动效：直接落到新位置。
            selectedKey = target
            lastSelectedLocal = anchors[target]?.toLocal() ?: lastSelectedLocal
            lastSelectedSide = null
            progress.snapTo(1f)
            return
        }
        val start = measuredStart ?: offscreenStart(frozenStart, toRect, scrollDelta)
        if (start == null) {
            selectedKey = target
            lastSelectedLocal = toRect
            lastSelectedSide = null
            progress.snapTo(1f)
            return
        }
        from = start
        to = toRect
        fromKey = selectedKey
        toKey = target
        // 先把进度归零再挂几何：否则可能有一帧 from/to 已就位但进度还是 1，
        // 会按"动画最后一帧"直接画到目标位置。
        progress.snapTo(0f)
        progress.animateTo(1f, tween(FeedRingMoveMs, easing = FeedRingEasing))
        // 顺序不能反：先把静态环落到目标，再撤掉动画几何。否则中间会有一帧
        // from/to 已清空、selectedKey 还停在源头像，于是按旧位置画一圈静态环——就是"闪一下"。
        selectedKey = target
        lastSelectedLocal = anchors[target]?.toLocal() ?: lastSelectedLocal
        lastSelectedSide = null
        fromKey = null
        toKey = null
        from = null
        to = null
    }

    /**
     * 源头像已不在屏幕内时的起点圆。
     * 优先用本次滚动方向（最真实）；否则用选出口记住的方向；再不行按最后一帧离哪边近兜底。
     */
    private fun offscreenStart(frozen: Rect?, target: Rect, scrollDelta: Int): Rect? {
        // 覆盖层还没测量到宽度时无法算出屏幕外坐标；宁可按旧位置画，也不要猜反。
        if (viewportWidth <= 0f) return frozen
        val side = when {
            scrollDelta > 0 -> FeedRingSide.Left
            scrollDelta < 0 -> FeedRingSide.Right
            lastSelectedSide != null -> lastSelectedSide
            frozen != null && viewportWidth > 0f -> feedRingNearestSide(frozen, viewportWidth)
            else -> null
        } ?: return frozen
        val size = lastSelectedSize.takeIf { it > 0f } ?: target.width
        return feedRingOffscreenStart(
            side = side,
            avatarSize = size,
            centerY = target.top + target.height / 2f,
            viewportWidth = viewportWidth,
        )
    }

    /** 某个头像被重新测量：只有当前参与绘制的 key 变化才让 Canvas 重绘。 */
    internal fun onAnchor(key: Any, rect: Rect) {
        val previous = anchors.put(key, rect)
        if (key == selectedKey) {
            // 方向必须用**卡片内坐标**判断：锚点是 root 坐标，宽屏带侧栏时和卡片宽度根本不是一个原点。
            val local = rect.toLocal()
            lastSelectedLocal = local
            lastSelectedSize = rect.width
            // 只有真的探出视口才记方向；整颗可见时清空，避免拿水平位置瞎猜左右。
            lastSelectedSide = feedRingExitSide(local, viewportWidth)
            if (previous != rect) anchorsVersion++
        } else if (key == fromKey || key == toKey) {
            if (previous != rect) anchorsVersion++
        }
    }

    /**
     * 头像滚出视口被回收：把最后一次位置和方向一起记住。
     *
     * 头像被回收有两种原因：横向滑出头像行，或整张头像卡纵向滚出动态列表。
     * 后者最后一次测量是完整可见的，不能按它的水平位置猜左右（那会把方向记反）；
     * 这时只信头像行此刻的滚动方向，拿不到就保持未知。
     */
    internal fun clearAnchor(key: Any, rowScrollDirection: Int = 0) {
        val removed = anchors.remove(key) ?: return
        if (key == selectedKey) {
            val local = removed.toLocal()
            lastSelectedLocal = local
            lastSelectedSize = removed.width
            lastSelectedSide = feedRingExitSide(local, viewportWidth)
                ?: when {
                    rowScrollDirection > 0 -> FeedRingSide.Left
                    rowScrollDirection < 0 -> FeedRingSide.Right
                    else -> null
                }
            anchorsVersion++
        } else if (key == fromKey || key == toKey) {
            anchorsVersion++
        }
    }

    internal fun drawInto(scope: DrawScope, color: Color, stroke: Stroke) {
        // 绘制阶段读版本号：只有参与绘制的头像移动时才重绘。
        @Suppress("UNUSED_VARIABLE") val tracked = anchorsVersion
        // 锚点是 root 坐标、冻结矩形是卡片内坐标：统一换到卡片内再画，
        // 这样整个动态列表上下滚动时，起点也不会"跑到屏幕上/下方去"。
        val end = toKey?.let { anchors[it] }?.toLocal() ?: to
        val liveStart = fromKey?.let { anchors[it] }?.toLocal()
        // 源头像在滚动中被回收时，用它最后被测量的位置（lastSelectedLocal）继续，
        // 不要退回动画开始时的冻结位置，否则线会跳回原处。
        val start = liveStart
            ?: if (fromKey != null && fromKey == selectedKey) lastSelectedLocal else from
        if (start != null && end != null) {
            val startOffscreen = feedRingIsOffscreen(start, viewportWidth)
            scope.drawMorphRing(start, end, progress.value, color, stroke, startOffscreen)
            return
        }
        val idle = anchors[selectedKey]?.toLocal()
        if (idle != null) scope.drawStaticRing(idle, color, stroke)
    }

    private fun Rect.toLocal(): Rect =
        if (originInRoot == Offset.Zero) this else translate(-originInRoot.x, -originInRoot.y)
}

/** 给头像圆加这一颗的测量点。 */

private fun DrawScope.drawStaticRing(rect: Rect, color: Color, stroke: Stroke) {
    val radius = rect.minDimension / 2f - stroke.width / 2f
    if (radius <= 0f) return
    drawCircle(
        color = color,
        radius = radius,
        center = Offset(rect.left + rect.width / 2f, rect.top + rect.height / 2f),
        style = stroke,
    )
}

/**
 * 两段式环：前半段解体重组，后半段缠绕。
 * 源头像已在屏幕外（[startOffscreen]）时跳过源环，只留一条从屏幕外滑进来的线。
 * 全部用 Float 算圆心/半径，避免每帧创建 Rect；只有真正要提交 Arc 时才建 Offset/Size。
 */
private fun DrawScope.drawMorphRing(
    from: Rect,
    to: Rect,
    progress: Float,
    color: Color,
    stroke: Stroke,
    startOffscreen: Boolean,
) {
    val strokePx = stroke.width
    // 用两端里较大的那个头像尺寸：只要有一端测量异常（被裁/尚未布局完）偏小，
    // 环也不会跟着缩小，只有两端都正常时才等于真实头像直径。
    val avatarSize = maxOf(minOf(from.width, from.height), minOf(to.width, to.height))
    val radius = avatarSize / 2f - strokePx / 2f
    if (radius <= 0f) return
    val srcCx = from.left + from.width / 2f
    val srcCy = from.top + from.height / 2f
    val dstCx = to.left + to.width / 2f
    val dstCy = to.top + to.height / 2f
    val srcBottomX = srcCx
    val srcBottomY = srcCy + radius
    val dstBottomX = dstCx
    val dstBottomY = dstCy + radius
    // 右移取 +1，左移取 -1：解体和缠绕都朝目标方向，左右镜像。
    val direction = if (dstCx >= srcCx) 1f else -1f
    val diameter = radius * 2f
    val unwindEnd = 0.45f
    if (progress < unwindEnd) {
        // 前半段：环从底部朝目标方向解开，缺口越大；同时一条线从源底部伸出去。
        val q = (progress / unwindEnd).coerceIn(0f, 1f)
        val sweep = 360f * (1f - q) * direction
        if (!startOffscreen && abs(sweep) >= 0.5f) {
            drawArc(
                color = color,
                startAngle = 90f,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = Offset(srcCx - radius, srcCy - radius),
                size = Size(diameter, diameter),
                style = stroke,
            )
        }
        drawSegment(
            color = color, strokePx = strokePx,
            x1 = srcBottomX, y1 = srcBottomY,
            x2 = srcBottomX + (dstBottomX - srcBottomX) * q,
            y2 = srcBottomY + (dstBottomY - srcBottomY) * q,
        )
    } else {
        // 后半段：线往目标底部收，目标环从底部同一方向绕上去。
        val q = ((progress - unwindEnd) / (1f - unwindEnd)).coerceIn(0f, 1f)
        drawSegment(
            color = color, strokePx = strokePx,
            x1 = srcBottomX + (dstBottomX - srcBottomX) * q,
            y1 = srcBottomY + (dstBottomY - srcBottomY) * q,
            x2 = dstBottomX, y2 = dstBottomY,
        )
        val sweep = -360f * q * direction
        if (abs(sweep) >= 0.5f) {
            drawArc(
                color = color,
                // 线是朝目标方向走的，到目标底部时的切线方向就是"继续往目标方向"，
                // 所以缠绕必须沿着这个切线绕上去（右移→从右侧绕起，即逆时针），
                // 和解体那半段的方向相反。
                startAngle = 90f,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = Offset(dstCx - radius, dstCy - radius),
                size = Size(diameter, diameter),
                style = stroke,
            )
        }
    }
}

/** 零长度的线在 Round cap 下会画出一个圆点疙瘩，短于半个描边宽就直接不画。 */
private fun DrawScope.drawSegment(
    color: Color, strokePx: Float,
    x1: Float, y1: Float, x2: Float, y2: Float,
) {
    val dx = x2 - x1
    val dy = y2 - y1
    val minLength = strokePx * 0.5f
    if (dx * dx + dy * dy <= minLength * minLength) return
    drawLine(
        color = color,
        start = Offset(x1, y1),
        end = Offset(x2, y2),
        strokeWidth = strokePx,
        cap = StrokeCap.Round,
    )
}

