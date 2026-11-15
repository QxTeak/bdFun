package com.aritxonly.bdfun

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import com.aritxonly.teak.common.chrome.LocalTopBarScrollProgress
import com.aritxonly.teak.common.chrome.ProgressiveTopBarMaterial
import com.aritxonly.teak.common.chrome.TeakPageScrollState
import com.aritxonly.teak.common.material.LocalTeakBackdrop
import com.aritxonly.teak.common.search.TeakSearchTransitionDefaults
import kotlin.math.abs

/**
 * 主壳顶栏的搜索过渡：首页三段（推荐 / 直播 / 番剧）和动态页共用这一份实现。
 *
 * 以前首页和动态页各写一份，参数慢慢就漂了——「推荐修好了、直播/番剧/动态还是撕裂」
 * 这类问题基本都是两份实现不同步造成的。这里只保留一条规则：
 *
 * 1. 材质层（渐进模糊 + 玻璃）只做**淡出**，不缩放；
 * 2. 缩放 / 横移交给内容层，否则模糊采样矩形跟着缩到 0.9，动画里会切出一条清晰/模糊的硬边；
 * 3. 一镜到底时内容整体上移并被模糊，顶栏采样要按同样的位移和模糊量补偿，接缝才不会露出来；
 * 4. 搜索页打开时采样源切到搜索页自己的 backdrop，进度切到搜索页的滚动进度。
 */
@Composable
internal fun BoxScope.MainTopBarChrome(
    shell: MainShellState,
    /** 这个顶栏属于 pager 的第几页：首页三段都是 0，动态页是 1。 */
    pageIndex: Int,
    pageFloat: Float,
    /** 该页自己的滚动进度（直播/番剧用各自的 chrome）。 */
    surface: TeakPageScrollState,
    /** 本页没有搜索框时（动态页），搜索页盖上来要把顶栏一起淡掉，而不是硬切。 */
    fadesWhenSearchOpens: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val ui = shell.ui
    val bottom = shell.bottom
    val pageAlpha = (1f - abs(pageFloat - pageIndex)).coerceIn(0f, 1f)
    if (pageAlpha <= 0.01f) return

    // 一镜到底：内容被整体上移并模糊，顶栏画在变换之外，采样必须补偿同样的量。
    val liftPx = with(LocalDensity.current) { TeakSearchTransitionDefaults.ContentLift.toPx() } *
        bottom.bottomSearchProgress.value
    val underlayBlurPx = TeakSearchTransitionDefaults.UnderlayBlurPx * bottom.bottomBlurProgress.value
    val searchFade = if (fadesWhenSearchOpens && ui.showSearch) {
        (1f - shell.searchReveal).coerceIn(0f, 1f)
    } else {
        1f
    }

    CompositionLocalProvider(
        // 搜索页打开后顶栏改采样搜索页自己的 backdrop，否则边缘会露出底下的首页。
        LocalTeakBackdrop provides if (ui.showSearch) shell.backdrops.search else LocalTeakBackdrop.current,
        LocalTopBarScrollProgress provides if (ui.showSearch) {
            shell.chrome.search.materialProgress
        } else {
            surface.materialProgress
        },
    ) {
        ProgressiveTopBarMaterial(
            underlayLiftPx = liftPx,
            underlayBlurPx = underlayBlurPx,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .graphicsLayer {
                    alpha = pageAlpha * (1f - bottom.bottomTopBarAlpha.value) * searchFade
                },
        ) {
            Box(
                modifier = Modifier.graphicsLayer {
                    val searchScale = 1f - 0.1f * bottom.bottomSearchProgress.value
                    scaleX = searchScale
                    scaleY = searchScale
                    translationX = -(pageFloat - pageIndex) * size.width * 0.92f
                },
            ) {
                content()
            }
        }
    }
}
