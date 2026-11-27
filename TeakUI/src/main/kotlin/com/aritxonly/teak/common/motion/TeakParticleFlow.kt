package com.aritxonly.teak.common.motion

import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.animateBounds
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aritxonly.teak.common.chrome.TeakMotion
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * 可删除的标签流：某一项从 [items] 消失时，在它原来的位置炸出一簇粒子，
 * 其余项用 [animateBounds] 滑到新位置，不会整块瞬间重排。
 *
 * 为什么放在 TeakUI：搜索历史、标签编辑、收藏夹多选都要「删一项、别的项别瞬移」
 * 这个过渡，粒子只是过渡上的装饰层；实现留在业务包里，下一个页面就只能复制一份。
 *
 * 粒子颜色只取 [burstColor]——**被删项自己的那个颜色**（通常是它的底色）。
 * 不要在这里混主题强调色或文字色：灰胶囊炸出红/黑碎屑会比不炸还突兀。
 * 层次感靠同色的透明度差异，不靠换色。
 *
 * @param items 当前存在的项；消失的项会触发一次爆炸。
 * @param itemKey 稳定标识，必须和 Compose 的 key 一样能区分每一项，
 *   否则删中间项时槽位会被复用，表现出来是「原地换字」而不是滑动。
 * @param burstColor 被删项的颜色；返回透明色时跳过这一次爆炸（只做重排）。
 * @param itemContent 单项内容，尺寸由它自己决定。
 */
@Composable
fun <T> TeakParticleFlow(
    items: List<T>,
    itemKey: (T) -> Any,
    burstColor: (T) -> Color,
    modifier: Modifier = Modifier,
    horizontalSpacing: Dp = 8.dp,
    verticalSpacing: Dp = 8.dp,
    itemContent: @Composable (T) -> Unit,
) {
    val scope = rememberCoroutineScope()
    // 每项最后一次测量到的位置（相对本组件左上角），用完即删，不跟着列表无限增长。
    val anchors = remember { mutableMapOf<Any, Rect>() }
    val bursts = remember { mutableStateListOf<TeakParticleBurst>() }
    var previous by remember { mutableStateOf(items) }

    LaunchedEffect(items) {
        val liveKeys = items.map(itemKey).toHashSet()
        // 只有「上一帧还在、这一帧没了」的项才算删除：新增和重排都不炸。
        val removed = previous.filter { itemKey(it) !in liveKeys }
        removed.forEach { item ->
            val key = itemKey(item)
            val anchor = anchors.remove(key) ?: return@forEach
            if (!TeakMotion.enabled) return@forEach
            val color = burstColor(item)
            if (color.alpha <= 0f) return@forEach
            val burst = TeakParticleBurst(anchor, color, key.hashCode(), TeakParticleDefaults.MinParticles)
            bursts += burst
            scope.launch {
                burst.progress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(TeakParticleDefaults.DurationMs, easing = LinearEasing),
                )
                bursts.remove(burst)
            }
        }
        previous = items
    }

    val boundsTransform = remember {
        // 在动画真正开始时读开关：设置里关掉动效后，下一次删除就直接落位。
        BoundsTransform { _, _ ->
            tween(
                durationMillis = if (TeakMotion.enabled) TeakParticleDefaults.BoundsMs else 0,
                easing = FastOutSlowInEasing,
            )
        }
    }

    Box(modifier) {
        LookaheadScope {
            val lookaheadScope = this
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
                verticalArrangement = Arrangement.spacedBy(verticalSpacing),
            ) {
                items.forEach { item ->
                    key(itemKey(item)) {
                        Box(
                            Modifier
                                .animateBounds(
                                    lookaheadScope = lookaheadScope,
                                    boundsTransform = boundsTransform,
                                )
                                .onGloballyPositioned { anchors[itemKey(item)] = it.boundsInParent() },
                        ) {
                            itemContent(item)
                        }
                    }
                }
            }
        }
        Canvas(Modifier.matchParentSize()) {
            // 在绘制阶段读 progress，动画帧只触发重绘，不会把整个标签流重组一遍。
            bursts.forEach { burst -> drawTeakParticleBurst(burst) }
        }
    }
}

/** 粒子飘散的可调参数；改观感只动这里，不要散落到调用方。 */
