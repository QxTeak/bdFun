package com.aritxonly.teak.common.motion

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.aritxonly.teak.common.chrome.TeakMotion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 列表（懒列表 / 网格）删除一项时的粒子爆炸。
 *
 * 和 [TeakParticleFlow] 的区别：那边是 FlowRow 自己重排，爆炸中心天然就在组件坐标里；
 * 这里宿主和卡片不在同一棵子树（宿主画在页面顶层，卡片在 LazyGrid 里，还会被回收），
 * 所以拆成三步：
 *
 * 1. [teakParticleAnchor]：卡片每次布局都把**窗口矩形**登记到 state；
 * 2. [TeakParticleRemovalSync]：比较「上一帧的 key 集合」，只对真正从数据里消失的 key 炸；
 * 3. [TeakParticleBurstLayer]：宿主自己记录窗口原点，把窗口坐标换成自己的坐标后绘制。
 *
 * 为什么不认 `onDispose`：滚出视口的卡片也会 dispose，那样一滚动就满屏碎屑。
 * 只有「数据里已经没有这一项」才算删除。
 *
 * 爆炸按**被删项自己的矩形**铺开：整行视频卡片就炸满整行，小胶囊就只炸一小簇，
 * 视觉尺寸跟着被删的东西走，不用调用方自己算半径。颜色同样只取被删项自己的颜色
 * （视频卡片是封面平均色），不给颜色就跳过爆炸、只做列表重排。
 */
class TeakParticleBurstState internal constructor(
    private val scope: CoroutineScope,
    private val density: Density,
) {
    internal val bursts = mutableStateListOf<TeakParticleBurst>()

    /** key -> (窗口矩形, 爆炸颜色)；由卡片自己在布局阶段维护。 */
    private val anchors = mutableMapOf<Any, Anchor>()

    /** 宿主在本窗口里的原点，用来把窗口坐标换成宿主坐标。 */
    internal var hostOrigin: Offset = Offset.Zero

    private var generation: Any? = null
    private var lastKeys: List<Any> = emptyList()
    private var synced = false

    /** 卡片每次被测量时登记一次；同一项只保留最后一次位置。 */
    fun recordAnchor(key: Any, windowBounds: Rect, color: Color) {
        anchors[key] = Anchor(windowBounds, color)
    }

    /**
     * 比较 key 集合并炸掉消失的项。
     *
     * [generation] 变化（切收藏夹 / 切历史分类 / 换搜索词）时只重置基线不炸：
     * 整页换内容不是"删了一项"，否则一换分类就会炸一屏。
     */
    fun sync(generation: Any?, keys: List<Any>) {
        if (!synced || generation != this.generation) {
            this.generation = generation
            lastKeys = keys
            synced = true
            anchors.keys.retainAll(keys.toSet())
            return
        }
        val live = keys.toHashSet()
        val removed = lastKeys.filter { it !in live }
        removed.forEach { key ->
            val anchor = anchors[key] ?: return@forEach
            burstAtWindow(anchor.windowBounds, anchor.color)
        }
        lastKeys = keys
        // 锚点只服务"这一项还在数据里"的删除；已经不在列表里的项顺手清掉，避免无限增长。
        anchors.keys.retainAll(live)
    }

    /** 直接在一个窗口矩形上炸一簇（给没有卡片测量的场景用）。 */
    fun burstAtWindow(windowBounds: Rect, color: Color) {
        if (!TeakMotion.enabled) return
        if (color.alpha <= 0f) return
        if (windowBounds.width <= 0f || windowBounds.height <= 0f) return
        val count = particleCountFor(windowBounds)
        val burst = TeakParticleBurst(
            bounds = windowBounds.translate(-hostOrigin.x, -hostOrigin.y),
            color = color,
            seed = windowBounds.hashCode(),
            count = count,
        )
        bursts += burst
        scope.launch {
            burst.progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(TeakParticleDefaults.DurationMs, easing = LinearEasing),
            )
            bursts.remove(burst)
        }
    }

    /** 碎屑数量跟着面积走：整行视频卡片比一颗胶囊多，但不至于糊成一片。 */
    private fun particleCountFor(bounds: Rect): Int {
        val areaDp = (bounds.width / density.density) * (bounds.height / density.density)
        return (areaDp / TeakParticleDefaults.AreaPerParticleDp).toInt()
            .coerceIn(TeakParticleDefaults.MinParticles, TeakParticleDefaults.MaxParticles)
    }

    private class Anchor(val windowBounds: Rect, val color: Color)
}

@Composable
fun rememberTeakParticleBurstState(): TeakParticleBurstState {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    return remember(scope, density) { TeakParticleBurstState(scope, density) }
}

/**
 * 挂在页面顶层（Box 的最后一项）的爆炸绘制层：完全透明，不吃点击。
 *
 * 它同时把宿主原点记进 state，卡片登记的是窗口坐标也能画在正确位置。
 */
@Composable
fun TeakParticleBurstLayer(state: TeakParticleBurstState, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned {
                state.hostOrigin = it.localToWindow(Offset.Zero)
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            state.bursts.forEach { burst -> drawTeakParticleBurstInto(burst) }
        }
    }
}

/**
 * 给列表项挂上"删除时在这里炸"的能力。
 *
 * [key] 必须和 [TeakParticleRemovalSync] 传进来的 key 一致；[color] 是被删项自己的颜色，
 * 爆炸范围直接取这一项被测量到的矩形。
 */
@Composable
fun Modifier.teakParticleAnchor(
    state: TeakParticleBurstState?,
    key: Any,
    color: Color,
): Modifier {
    if (state == null) return this
    val latestColor by rememberUpdatedState(color)
    return this.onGloballyPositioned {
        state.recordAnchor(key, it.boundsInWindow(), latestColor)
    }
}

/**
 * 把当前 key 列表交给 state 做差分；只有"少了某一项"才炸。
 *
 * 调用方把 [keys] 换成不可变列表即可（`rows.map { it.bvid }`），
 * 相同内容不会重复触发：Compose 的 LaunchedEffect 按 equals 比较。
 */
@Composable
fun TeakParticleRemovalSync(
    state: TeakParticleBurstState,
    generation: Any?,
    keys: List<Any>,
) {
    LaunchedEffect(state, generation, keys) {
        state.sync(generation, keys)
    }
}

private fun DrawScope.drawTeakParticleBurstInto(burst: TeakParticleBurst) = burst.drawInto(this)
