package com.aritxonly.bdfun.ui

import com.aritxonly.teak.common.material.teakContentControlShadow
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.teak.common.material.TeakGlassRecipes
import com.aritxonly.teak.common.material.TeakGlassSurface
import com.aritxonly.teak.common.material.LocalTeakBackdrop
import com.aritxonly.teak.common.material.LocalTeakMaterialSpec
import com.aritxonly.bdfun.BrandPink
import com.kyant.shapes.Capsule
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * tab 栏右侧的小胶囊（「点我发弹幕」「弹」）。
 *
 * 与「简介/评论」同一套真玻璃；「弹」开启时叠一层粉色玻璃（半透明，仍然能透出底层模糊），
 * 文字转白；关闭时回到与 tab 一致的灰字白玻璃。
 */
@Composable
fun TeakGlassChip(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 34.dp,
    horizontalPadding: Dp = 14.dp,
    fontSize: androidx.compose.ui.unit.TextUnit = 12.sp,
) {
    val advancedMaterial = LocalTeakMaterialSpec.current
    val recipe = TeakGlassRecipes.floatingNavigation(advancedMaterial.fineTuning)
    val backdrop = LocalTeakBackdrop.current
    val colors = MiuixTheme.colorScheme

    TeakGlassSurface(
        modifier = modifier
            .teakContentControlShadow(Capsule(), if (com.aritxonly.teak.common.chrome.LocalProgressiveTopBarMaterialHosted.current)
                com.aritxonly.teak.common.chrome.LocalTopBarButtonMaterialProgress.current else 1f)
            .height(height)
            .clip(Capsule())
            .clickable(interactionSource = null, indication = null, onClick = onClick),
        shape = Capsule(),
        recipe = recipe,
        backdrop = backdrop,
        contentAlignment = Alignment.Center,
    ) {
        Box(modifier = Modifier.height(height)) {
            if (active) {
                // 粉色玻璃：不是纯色块，留一点透明度让底下的模糊透出来
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    BrandPink.copy(alpha = 0.92f),
                                    BrandPink.copy(alpha = 0.78f),
                                ),
                            ),
                        )
                        .border(
                            1.dp,
                            Color.White.copy(alpha = 0.38f),
                            Capsule(),
                        ),
                )
            }
            androidx.compose.foundation.layout.Row(
                modifier = Modifier
                    .height(height)
                    .padding(horizontal = horizontalPadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    color = if (active) Color.White else if (com.aritxonly.teak.common.theme.LocalTeakControlEmphasis.current) {
                        colors.onBackground.copy(alpha = 0.88f)
                    } else colors.onBackgroundVariant,
                    fontWeight = if (com.aritxonly.teak.common.theme.LocalTeakControlEmphasis.current) {
                        androidx.compose.ui.text.font.FontWeight.Medium
                    } else androidx.compose.ui.text.font.FontWeight.Normal,
                    fontSize = fontSize,
                    maxLines = 1,
                )
            }
        }
    }
}

enum class MenuGlyphKind { Reply, Copy, Report, Block, Delete, Coin, Star }

/** 空占位（保持 import 稳定用）。 */
@Composable
internal fun MenuGlyphSpacer() {
    Spacer(Modifier.size(0.dp))
}

/** UP 标识：首页卡片与评论区共用同一套样式。 */
@Composable
fun UpBadge() = RoleBadge("UP")

/**
 * 「主播」标识：和 UP 标识同一个组件、同一套颜色/字号，只换文字。
 * 直播卡片上的主播名前缀用它，保证两处视觉一致。
 */
@Composable
fun AnchorBadge() = RoleBadge("主播")

/** 身份小胶囊的统一实现：UP / 主播 都用它，避免两处样式漂移。 */
@Composable
fun RoleBadge(text: String, color: Color = BrandPink) {
    Box(
        modifier = Modifier
            .clip(Capsule())
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 4.dp, vertical = 1.dp),
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 9.sp,
            lineHeight = 10.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
        )
    }
}

/** 首页推荐里已关注 UP 的橙色小胶囊，尺寸和 UP 标签完全一致。 */
@Composable
fun FollowedBadge() {
    val orange = Color(0xFFFF8A00)
    Box(
        modifier = Modifier
            .clip(Capsule())
            .background(orange.copy(alpha = 0.14f))
            .padding(horizontal = 4.dp, vertical = 1.dp),
    ) {
        Text(
            text = "已关注",
            color = orange,
            fontSize = 9.sp,
            lineHeight = 10.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
        )
    }
}
