package com.aritxonly.bdfun.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.teak.common.material.TeakGlassRecipes
import com.aritxonly.teak.common.material.TeakGlassSurface
import com.aritxonly.teak.common.material.LocalTeakBackdrop
import com.aritxonly.teak.common.material.LocalTeakMaterialSpec
import com.kyant.shapes.Capsule
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 底栏同款玻璃分段控件（全部 / 视频）。
 *
 * 指示器不再是“整块瞬间变色”，而是和底栏一样用弹簧在选项之间滑动；
 * 粉色底做了半透明处理，能透出底下的玻璃模糊。
 */
@Composable
fun TeakSegmentedBar(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    itemWidth: Dp = 74.dp,
    materialAlpha: Float = 1f,
) {
    val colors = MiuixTheme.colorScheme
    val advancedMaterial = LocalTeakMaterialSpec.current
    val recipe = TeakGlassRecipes.floatingNavigation(advancedMaterial.fineTuning)

    // 和底栏 / 视频页 tab 同一套手动弹簧，避免项目 frame clock 不推进的问题
    val animIndex = rememberSpringFloat(
        target = selected.toFloat(),
        stiffness = 340f,
        damping = 25f,
    )
    val indicatorAlpha = 0.92f

    TeakGlassSurface(
        modifier = modifier.height(40.dp),
        shape = Capsule(),
        materialAlpha = materialAlpha,
        flatBackgroundAlpha = com.aritxonly.teak.common.material.TeakGlassDefaults.flatBackgroundAlpha(),
        recipe = recipe,
        backdrop = LocalTeakBackdrop.current,
    ) {
        Box(modifier = Modifier.padding(4.dp)) {
            // 滑动的粉色指示器（半透明，带一点高光描边）
            Box(
                modifier = Modifier
                    .offset(x = itemWidth * animIndex)
                    .width(itemWidth)
                    .height(32.dp)
                    .clip(Capsule())
                    .background(colors.primary.copy(alpha = indicatorAlpha)),
            )
            Row {
                options.forEachIndexed { index, label ->
                    val active = index == selected
                    val textColor by animateColorAsState(
                        targetValue = if (active) Color.White else colors.onBackgroundVariant,
                        label = "segmented_text_color",
                    )
                    Box(
                        modifier = Modifier
                            .width(itemWidth)
                            .height(32.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { onSelect(index) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = label,
                            color = textColor,
                            fontSize = 13.sp,
                            fontWeight = if (active) FontWeight.Normal else FontWeight.Normal,
                        )
                    }
                }
            }
        }
    }
}
