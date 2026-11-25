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

enum class MenuGlyphKind { Reply, Copy, Report, Block, Delete, Coin, Star }

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
