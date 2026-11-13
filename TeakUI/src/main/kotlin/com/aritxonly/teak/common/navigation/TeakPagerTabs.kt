package com.aritxonly.teak.common.navigation

import com.aritxonly.teak.common.material.teakContentControlShadow
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.teak.common.chrome.TeakMotion
import com.aritxonly.teak.common.material.TeakGlassRecipes
import com.aritxonly.teak.common.material.TeakGlassSurface
import com.aritxonly.teak.common.material.LocalTeakBackdrop
import com.aritxonly.teak.common.material.LocalTeakMaterialSpec
import com.kyant.shapes.Capsule
import top.yukonga.miuix.kmp.blur.Backdrop
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

enum class TeakTabIndicatorStyle {
    /** 灰色胶囊，默认样式。 */
    Pill,

    /** 细横线，放在文字下方，适合搜索页这种没有底板的 Tab。 */
    Underline,
}

/** 视频页 / 二级页的玻璃 Tab 规范；数值与首页底栏保持同一套视觉节奏。 */
object TeakPagerTabDefaults {
    val Height = 40.dp
    val ItemWidth = 60.dp
    val ContentPadding = 4.dp
    val ItemHeight = 32.dp
    val IndicatorAlpha = 0.075f
    // 与首页底栏同一套弹簧参数（stiffness 400 / damping 30）。
    const val IndicatorStiffness = 400f
    const val IndicatorDamping = 30f
    val LabelFontSize = 13.sp
}

/**
 * 与底栏同一套逻辑的 Tab：玻璃底座 + **灰色**滑动指示器 + **选中粉色字**。
 * 玻璃底座、指示器动画和弹簧参数沿用 HyperPiliPlus 底栏体系的观感；
 * 组件代码是 Teak 重新组合的，不是上游同名文件；来源记录见 docs/PROVENANCE.md。
 * 必须放在采集层之外（Activity 覆盖层），否则玻璃会自引用。
 */
@Composable
fun TeakPagerTabs(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    itemWidth: androidx.compose.ui.unit.Dp = TeakPagerTabDefaults.ItemWidth,
    /** 放在页面采集层内部时必须传 null，否则会自引用；放在 topBar/overlay 才用默认值。 */
    backdrop: Backdrop? = LocalTeakBackdrop.current,
    /**
     * 纯色模式：不启用玻璃模糊，只画页面同款底色。
     * 消息 / 收藏 / 历史等页面内的 Tab 用 true；顶栏、底栏仍用默认的玻璃。
     */
    plain: Boolean = false,
    materialProgress: Float = if (com.aritxonly.teak.common.chrome.LocalProgressiveTopBarMaterialHosted.current)
        com.aritxonly.teak.common.chrome.LocalTopBarButtonMaterialProgress.current else 1f,
    /**
     * 连续位置（0.0 = 第一项，1.5 = 正从第二项滑向第三项）。
     * 传入后指示器在布局阶段跟随手指移动，不再等页面切换结束才跳过去。
     */
    position: State<Float>? = null,
    /** 去掉 plain 模式的白色胶囊底板，让 Tab 直接浮在页面/模糊层上。 */
    transparentBackground: Boolean = false,
    /** 指示器样式：默认灰色胶囊；Underline 为文字下方细线。 */
    indicatorStyle: TeakTabIndicatorStyle = TeakTabIndicatorStyle.Pill,
) {
    val advancedMaterial = LocalTeakMaterialSpec.current
    val recipe = TeakGlassRecipes.floatingNavigation(advancedMaterial.fineTuning)
    val colors = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

    // 有连续位置时直接跟位置，不再套第二层弹簧：
    // 之前的做法每帧重启弹簧循环，实际速度被拖得极慢。
    // 没有连续位置时才用和首页底栏同参数的弹簧。
    val springIndex = rememberPagerSpring(
        target = selected.toFloat(),
        stiffness = TeakPagerTabDefaults.IndicatorStiffness,
        damping = TeakPagerTabDefaults.IndicatorDamping,
    )
    val animIndex = position?.value ?: springIndex

    // 文字高亮也跟随连续位置：滑过一半就切到下一页，而不是等动画结束。
    val activeIndex = if (position != null) {
        val nearest = remember(position) {
            derivedStateOf {
                position.value.roundToInt().coerceIn(0, options.lastIndex)
            }
        }
        nearest.value
    } else {
        selected
    }

    val classic = com.aritxonly.teak.common.theme.TeakUiStyle.isClassic
    val underline = classic || indicatorStyle == TeakTabIndicatorStyle.Underline
    val content: @Composable () -> Unit = {
        Box(modifier = Modifier.padding(if (classic) 0.dp else TeakPagerTabDefaults.ContentPadding)) {
            // 指示器：官方是灰色胶囊，经典/搜索页是文字下方一条短横线
            if (underline) {
                Box(
                    modifier = Modifier
                        .offset(x = itemWidth * animIndex + itemWidth * 0.34f, y = 36.dp)
                        .width(itemWidth * 0.32f)
                        .height(2.dp)
                        .background(colors.primary, Capsule()),
                )
            } else {
                Box(
                    modifier = Modifier
                        .offset(x = itemWidth * animIndex)
                        .width(itemWidth)
                        .height(TeakPagerTabDefaults.ItemHeight)
                        .background(
                            colors.onBackground.copy(alpha = TeakPagerTabDefaults.IndicatorAlpha),
                            Capsule(),
                        ),
                )
            }
            Row {
                options.forEachIndexed { index, label ->
                    val active = index == activeIndex
                    Box(
                        modifier = Modifier
                            .width(itemWidth)
                            .height(TeakPagerTabDefaults.ItemHeight)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { onSelect(index) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = label,
                            color = if (active) colors.primary else if (com.aritxonly.teak.common.theme.LocalTeakControlEmphasis.current) {
                                colors.onBackground.copy(alpha = 0.88f)
                            } else colors.onBackgroundVariant,
                            fontSize = TeakPagerTabDefaults.LabelFontSize,
                            fontWeight = if (com.aritxonly.teak.common.theme.LocalTeakControlEmphasis.current) {
                                FontWeight.Medium
                            } else FontWeight.Normal,
                        )
                    }
                }
            }
        }
    }

    if (classic) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(TeakPagerTabDefaults.Height)
                .drawBehind {
                    val y = size.height - 1.dp.toPx() / 2f
                    drawLine(
                        color = colors.onBackgroundVariant.copy(alpha = 0.28f),
                        start = androidx.compose.ui.geometry.Offset(0f, y),
                        end = androidx.compose.ui.geometry.Offset(size.width, y),
                        strokeWidth = 1.dp.toPx(),
                    )
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            content()
        }
    } else if (plain && transparentBackground) {
        Box(
            modifier = modifier.teakContentControlShadow(Capsule(), materialProgress)
                .height(TeakPagerTabDefaults.Height),
            contentAlignment = Alignment.CenterStart,
        ) {
            content()
        }
    } else if (plain) {
        Box(
            modifier = modifier
                .height(TeakPagerTabDefaults.Height)
                .clip(Capsule())
                .background(colors.surface),
            contentAlignment = Alignment.CenterStart,
        ) {
            content()
        }
    } else {
        TeakGlassSurface(
            modifier = modifier.teakContentControlShadow(Capsule(), materialProgress)
                .height(TeakPagerTabDefaults.Height),
            shape = Capsule(),
            recipe = recipe,
            backdrop = backdrop,
            materialAlpha = materialProgress,
            flatBackgroundAlpha = com.aritxonly.teak.common.material.TeakGlassDefaults.flatBackgroundAlpha(),
        ) {
            content()
        }
    }
}

/** 与页面 chrome 同一套手动弹簧：不依赖宿主 App 的 rememberSpringFloat。 */
@Composable
private fun rememberPagerSpring(
    target: Float,
    stiffness: Float,
    damping: Float,
): Float {
    var value by remember { mutableFloatStateOf(target) }
    var velocity by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(target) {
        if (!TeakMotion.enabled) {
            value = target
            velocity = 0f
            return@LaunchedEffect
        }
        var last = System.nanoTime()
        while (true) {
            val now = System.nanoTime()
            val dt = ((now - last) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
            last = now
            val accel = -stiffness * (value - target) - damping * velocity
            velocity += accel * dt
            value += velocity * dt
            if (kotlin.math.abs(value - target) < 0.0006f &&
                kotlin.math.abs(velocity) < 0.006f
            ) {
                value = target
                velocity = 0f
                break
            }
            delay(8)
        }
    }
    return value
}
