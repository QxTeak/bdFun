package com.aritxonly.bdfun.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
     * 选中头像滚出视口后，它的锚点会被回收；但下一次切换仍然要从它"消失的位置"出发，
     * 否则起点丢失就只能瞬移。这里以**卡片内坐标**留一份最后位置——用 root 坐标存的话，
     * 之后整个动态列表再上下滚动，旧 root 坐标就会指到卡片上方/下方去。
     */
    private var lastSelectedLocal: Rect? = null

    /** 环现在画在哪（root 坐标）；换选中项前用它冻结起点，头像栏滚动也不会把起点弄丢。 */
    internal fun currentRect(): Rect? =
        fromKey?.let { anchors[it] } ?: anchors[selectedKey]
            ?: lastSelectedLocal?.translate(originInRoot.x, originInRoot.y)

    /**
     * 选中项变化时调用：解体重组到 [target]。
     * [source] 是调用方在滚动头像栏之前冻结的起点；为空时退回到当前锚点。
     */
    internal suspend fun moveTo(target: Any, source: Rect? = null) {
        if (!primed) {
            primed = true
            selectedKey = target
            progress.snapTo(1f)
            return
        }
        if (target == selectedKey && progress.value >= 1f) return
        val fromRect = source?.translate(-originInRoot.x, -originInRoot.y)
            ?: anchors[selectedKey]?.toLocal()
            ?: lastSelectedLocal
        val toRect = anchors[target]?.toLocal()
        if (BuildConfig.DEBUG) {
            LogCollector.record(
                "FEED_RING",
                "moveTo target=$target from=${fromRect.desc()} to=${toRect.desc()}",
            )
        }
        if (!TeakMotion.enabled || fromRect == null || toRect == null) {
            // 目标还没测量到（比如刚滚进视口）或关掉了动效：直接落到新位置。
            selectedKey = target
            lastSelectedLocal = anchors[target]?.toLocal() ?: lastSelectedLocal
            progress.snapTo(1f)
            return
        }
        from = fromRect
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
        fromKey = null
        toKey = null
        from = null
        to = null
    }

    /** 某个头像被重新测量：只有当前参与绘制的 key 变化才让 Canvas 重绘。 */
    internal fun onAnchor(key: Any, rect: Rect) {
        val previous = anchors.put(key, rect)
        if (key == selectedKey) lastSelectedLocal = rect.toLocal()
        if (previous == rect) return
        if (key == selectedKey || key == fromKey || key == toKey) anchorsVersion++
    }

    /** 头像滚出视口被回收。 */
    internal fun clearAnchor(key: Any) {
        val removed = anchors.remove(key) ?: return
        if (key == selectedKey) {
            // 留着最后位置：下一次从它开始切换时还能播"从屏幕外飞进来"的动画。
            lastSelectedLocal = removed.toLocal()
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
        val start = fromKey?.let { anchors[it] }?.toLocal()
            ?: from?.let { frozen -> snapOffscreenStart(frozen, end, scope.size.width, scope.size.height) }
        val idle = anchors[selectedKey]?.toLocal()
        if (start != null && end != null) {
            scope.drawMorphRing(start, end, progress.value, color, stroke)
            return
        }
        if (idle != null) scope.drawStaticRing(idle, color, stroke)
    }

    /**
     * 起点是"已经滚出视口的头像"时，它的坐标只可能是历史值：高度一律对齐到目标那一行，
     * 横向夹到卡片边缘。这样无论之前列表怎么上下滚，都只会从左右侧飞进来。
     */
    private fun snapOffscreenStart(frozen: Rect, end: Rect?, width: Float, height: Float): Rect {
        val centerY = end?.let { it.top + it.height / 2f } ?: (height / 2f)
        val centerX = (frozen.left + frozen.width / 2f).coerceIn(0f, width)
        val halfW = frozen.width / 2f
        val halfH = frozen.height / 2f
        return Rect(centerX - halfW, centerY - halfH, centerX + halfW, centerY + halfH)
    }

    private fun Rect.toLocal(): Rect =
        if (originInRoot == Offset.Zero) this else translate(-originInRoot.x, -originInRoot.y)
}

/** 给头像圆加这一颗的测量点。 */
internal fun Modifier.feedAvatarAnchor(state: FeedAvatarRingState, key: Any): Modifier =
    onGloballyPositioned { state.onAnchor(key, it.boundsInRoot()) }

@Composable
internal fun FeedAvatarRingOverlay(
    state: FeedAvatarRingState,
    color: Color,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 2.dp,
) {
    // Stroke 每帧都要传进 drawArc，提到组合里复用，动画期间不再逐帧新建。
    val strokePx = with(LocalDensity.current) { strokeWidth.toPx() }
    val stroke = remember(strokePx) { Stroke(width = strokePx, cap = StrokeCap.Round) }
    Canvas(modifier.onGloballyPositioned { state.originInRoot = it.positionInRoot() }) {
        state.drawInto(this, color, stroke)
    }
}

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
 * 全部用 Float 算圆心/半径，避免每帧创建 Rect；只有真正要提交 Arc 时才建 Offset/Size。
 */
private fun DrawScope.drawMorphRing(from: Rect, to: Rect, progress: Float, color: Color, stroke: Stroke) {
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
        if (abs(sweep) >= 0.5f) {
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

/**
 * 头像行里参与「UP 模式」的顺序：直播 UP、我、关注 UP，和横向列表的排列一致。
 * 「全部动态」不在里面——它是退出键，不参与左右滑动。
 */
internal fun feedUpMids(state: FeedState, selfMid: Long?): List<Long> {
    val live = state.liveUsers.map { it.mid }
    val me = selfMid?.takeIf { it !in live }
    val authors = state.authors.map { it.mid }.filterNot { it in live }
    return (live + listOfNotNull(me) + authors).distinct()
}

/** 左右滑动切换 UP：越过两端就停在原地，不绕回「全部动态」。 */
internal suspend fun FeedState.stepUpFocus(delta: Int, selfMid: Long?) {
    val current = filterMid ?: return
    val order = feedUpMids(this, selfMid)
    val next = order.getOrNull(order.indexOf(current) + delta) ?: return
    if (next != current) filterTo(next)
}

/** 退出 UP 模式：回到这一页自己的分类（全部/视频/番剧），而不是一律切成「全部」。 */
internal suspend fun FeedState.exitUpFocus(tabKey: String, loggedIn: Boolean) {
    if (filterMid == null) return
    switchTo(tabKey, loggedIn)
}

/** 头像在列表里的下标（含「全部动态」），用于把选中项滚进视口。 */
internal fun feedAvatarIndex(state: FeedState, selfMid: Long?, target: Any?): Int {
    val live = state.liveUsers.map { it.mid }
    val me = selfMid?.takeIf { it !in live }
    val authors = state.authors.map { it.mid }.filterNot { it in live }
    if (target == FeedAllAvatarKey) return live.size
    val mid = target as? Long ?: return -1
    if (mid in live) return live.indexOf(mid)
    if (mid == me) return live.size + 1
    val offset = live.size + 1 + if (me != null) 1 else 0
    val index = authors.indexOf(mid)
    return if (index >= 0) offset + index else -1
}

/** 调试用：把矩形缩成一行，方便排查环为什么会缩。 */
private fun Rect?.desc(): String =
    this?.let { "${it.left.toInt()},${it.top.toInt()} ${it.width.toInt()}x${it.height.toInt()}" } ?: "null"

