package com.aritxonly.bdfun.ui


import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.aritxonly.teak.common.chrome.teakProgressiveBarBlur


/**
 * 下拉刷新的小电视指示器。**故意挂在 App 层**（不在顶栏内部）：
 * 顶栏背景是 `progressiveTextureBlur`，会给整层建离屏图层并按矩形裁剪，
 * 放里面的话探出顶栏的部分会被切掉。
 *
 * 出现：快速淡入 + 轻微下移；松手开始加载后播放 GIF 动画；加载完带一点回弹缩回。
 */
@Composable
fun HomePullIndicator(
    reveal: Float,
    animated: Boolean,
    modifier: Modifier = Modifier,
) {
    val alpha = reveal.coerceIn(0f, 1f)
    if (alpha <= 0.005f) return

    TeakTvLoading(
        size = 48.dp,
        animated = animated,
        modifier = modifier.graphicsLayer {
            this.alpha = alpha
            translationY = (1f - alpha) * -18f
        },
    )
}

/**
 * 绘制期读取版本：拖动过程中只失效绘制、不触发重组。
 * 调用方需自己判断是否合成（不可见时不要合成，避免语义节点常驻）。
 */
@Composable
fun HomePullIndicator(
    reveal: () -> Float,
    animated: Boolean,
    modifier: Modifier = Modifier,
) {
    TeakTvLoading(
        size = 48.dp,
        animated = animated,
        modifier = modifier.graphicsLayer {
            val value = reveal().coerceIn(0f, 1f)
            alpha = value
            translationY = (1f - value) * -18f
        },
    )
}

/**
 * 顶栏渐变模糊：实现已抽到 TeakUI 的 `Modifier.teakProgressiveBarBlur()`。
 * 这里保留旧名字，避免首页 / 动态页的调用点到处改。
 */
@Composable
fun Modifier.teakBarBlur(): Modifier = teakProgressiveBarBlur()
