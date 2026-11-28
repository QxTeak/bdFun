package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.BrandPink
import com.aritxonly.bdfun.BrandPinkDeep
import com.aritxonly.teak.common.icons.AppGlyph
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.*
import top.yukonga.miuix.kmp.icon.extended.*
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 头像排最左边的「全部动态」：粉色渐变圆 + 四宫格图标，尺寸/描边和 UP 头像一致。
 *
 * 选中环由头像行共用的 [FeedAvatarRingOverlay] 统一绘制，这里不再自己画一圈。
 */
@Composable
internal fun AllFeedAvatar(
    avatarModifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(56.dp)
            .clickable(interactionSource = null, indication = null, onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .then(avatarModifier)
                .clip(CircleShape),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                colors.primary.copy(alpha = 0.55f),
                                BrandPink,
                                BrandPinkDeep,
                            ),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                AppGlyph(MiuixIcons.All, Color.White, size = 20.dp)
            }
        }
        Text(
            text = "全部动态",
            color = colors.onBackgroundVariant,
            fontSize = 9.5.sp,
            lineHeight = 12.sp,
            maxLines = 1,
            // 和 UP 名称同一条淡出：UP 页往下划时整行名字一起收掉。
            modifier = Modifier
                .padding(top = 3.dp)
                .feedAvatarLabelFade(LocalFeedAvatarLabelFade.current),
        )
    }
}
