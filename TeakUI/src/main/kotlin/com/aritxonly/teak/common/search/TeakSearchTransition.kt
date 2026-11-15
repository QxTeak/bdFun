package com.aritxonly.teak.common.search

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * 搜索展开动效参数。
 *
 * 数值全部取自 MIUI「文件管理 9.3.0.8」反编译结果
 * `com.android.fileexplorer.fragment.SearchTransitionAnimator`：
 *
 * - 页面内容整体上移 106dp（`dp106ToPx`），弹簧 `FolmeEase.spring(0.97f, 0.4f)`；
 * - 底层顶栏 / 吸顶栏缩到 0.9 并淡出，同时叠 0 → 50 的高斯模糊（时长 150ms，线性）；
 * - 搜索栏、容器与遮罩：线性 100ms 淡入，容器与搜索栏各延迟 50ms；
 * - 退出时整套反向播放。
 *
 * 这里把「弹簧阻尼比 0.97 / 响应 0.4s」换算成 Compose 的
 * `spring(dampingRatio, stiffness)`：stiffness ≈ (2π / 0.4)² ≈ 247。
 */
/** 官方用的 SineEaseInOut：0.5 - 0.5·cos(πx)。 */
val SineEaseInOut: Easing = Easing { x -> 0.5f - 0.5f * kotlin.math.cos(x * Math.PI.toFloat()) }

object TeakSearchTransitionDefaults {
    /** 搜索页展开时底层内容上移的距离。 */
    val ContentLift = 106.dp

    /** 底层内容的最大模糊半径（px，和原实现的 RenderEffect 一致）。 */
    const val UnderlayBlurPx = 50f

    /**
     * 底层正文的缩放比例。官方 SearchTransitionAnimator 对正文只做 TRANSLATION_Y，
     * 缩到 0.9 的是顶栏和吸顶栏；正文这里保持 1f 才和官方一致。
     * 想要"内容后退"的观感可以改回 0.92，只影响这一个值。
     */
    const val UnderlayScale = 1f

    /** 底层模糊的独立时长：官方用 150ms 线性 ValueAnimator，不跟弹簧。 */
    const val UnderlayBlurMs = 150

    /** 原实现的 `FolmeEase.spring(0.97f, 0.4f)`。 */
    const val SpringDampingRatio = 0.97f

    /** 由 response = 0.4s 换算的刚度。 */
    const val SpringStiffness = 247f

    /** 底层内容在展开过程中的最终透明度：0 = 卡片完全渐隐退出。 */
    const val UnderlayAlphaTo = 0f

    /** 搜索栏 / 遮罩淡入淡出时长。 */
    const val ChromeFadeMs = 100

    /** 搜索栏与容器相对遮罩的延迟。 */
    const val ChromeFadeDelayMs = 50

    /** 底层顶栏淡出的时长。 */
    const val UnderlayAlphaMs = 150

    /** 官方 SearchDetailActivity 的 window 进入：Fade 300ms + startDelay 60ms + SineEaseInOut。 */
    const val WindowFadeMs = 300
    const val WindowFadeDelayMs = 60

    /** 官方返回时的 window 过渡：Fade 300ms + spring(damping 0.99, response 0.67)。 */
    const val WindowSpringDampingRatio = 0.99f
    const val WindowSpringStiffness = 88f

    /** 底栏（miuix Navigator 那套）用的弹簧：spring(0.97f, 0.3f)。 */
    const val TabBarSpringStiffness = 439f

    /** 搜索页进出使用的弹簧。 */
    fun spec(): SpringSpec<Float> = spring(
        dampingRatio = SpringDampingRatio,
        stiffness = SpringStiffness,
    )

    /** 整页淡入淡出（原来由 Activity window 过渡完成，现在是同一 Activity 内的一层）。 */
    fun windowInSpec() = tween<Float>(
        durationMillis = WindowFadeMs,
        delayMillis = WindowFadeDelayMs,
        easing = SineEaseInOut,
    )

    fun windowOutSpec() = spring<Float>(
        dampingRatio = WindowSpringDampingRatio,
        stiffness = WindowSpringStiffness,
    )

    /** 底栏独立下坠：阻尼比同样是 0.97，但 response 是 0.3，比搜索页快一档。 */
    fun tabBarSpec(): SpringSpec<Float> = spring(
        dampingRatio = SpringDampingRatio,
        stiffness = TabBarSpringStiffness,
    )
}

/**
 * 搜索展开时套在「底层页面」上的退让效果：上移 + 轻微缩小 + 渐进模糊 + 渐隐。
 *
 * [progress] 只驱动**几何**（位移 / 缩放 / 模糊），来自弹簧；
 * [alpha] 是独立的线性透明度（原实现里 alpha 用 `linear(150ms)`，
 * 和弹簧是两条并行的时间线）。传 1f 表示不参与淡出。
 */
@Composable
fun Modifier.teakSearchUnderlay(
    progress: Float,
    alpha: Float = 1f,
    blurProgress: Float = progress,
): Modifier {
    val p = progress.coerceIn(0f, 1f)
    val a = alpha.coerceIn(0f, 1f)
    val b = blurProgress.coerceIn(0f, 1f)
    if (p <= 0.001f && a >= 0.999f) return this
    // 只按「是否完全不可见」跳过：退出动画开头 p 还停在 1、alpha 已经回升，
    // 这里如果因为 p>=0.999 就省掉模糊，会出现"先清晰一下再变模糊"的跳变。
    if (a <= 0.001f) return this.graphicsLayer { this.alpha = a }
    val density = LocalDensity.current
    val liftPx = with(density) { TeakSearchTransitionDefaults.ContentLift.toPx() }
    val blurRadius = with(density) {
        (TeakSearchTransitionDefaults.UnderlayBlurPx * b).toDp()
    }
    val scale = 1f - (1f - TeakSearchTransitionDefaults.UnderlayScale) * p
    return this
        .graphicsLayer {
            translationY = -liftPx * p
            scaleX = scale
            scaleY = scale
            this.alpha = a
            transformOrigin = TransformOrigin(0.5f, 0.42f)
        }
        .blur(blurRadius)
}
