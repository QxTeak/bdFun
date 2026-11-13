package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kyant.shapes.Capsule
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun AuthorAvatar(
    name: String,
    face: String,
    selected: Boolean,
    hasUpdate: Boolean = false,
    badge: String? = null,
    onClick: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val ring = rememberManualProgress(if (selected) 1f else 0f, 200f)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(56.dp)
            .semantics { stateDescription = if (hasUpdate) "有更新" else badge.orEmpty() }
            .clickable(interactionSource = null, indication = null, onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .size(46.dp),
        ) {
            AsyncImage(
                model = rememberImageRequest(face),
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(colors.surfaceContainerHigh),
            )
            if (hasUpdate) {
                Box(Modifier.align(Alignment.TopEnd).size(8.dp).background(colors.primary, CircleShape))
            }
            if (badge != null) {
                Text(badge, color = colors.onPrimary, fontSize = 9.sp,
                    modifier = Modifier.align(Alignment.BottomCenter).clip(Capsule())
                        .background(colors.primary).padding(horizontal = 4.dp))
            }
            // 选中圈只做透明度渐显，不改变头像尺寸和位置
            if (ring > 0.01f) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .border(
                            2.dp,
                            colors.primary.copy(alpha = ring.coerceIn(0f, 1f)),
                            CircleShape,
                        ),
                )
            }
        }
        Text(
            text = name,
            color = colors.onBackgroundVariant,
            fontSize = 9.5.sp,
            lineHeight = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}

/** 头像排最左边的「全部动态」：粉色渐变圆 + 四宫格图标，尺寸/描边和 UP 头像一致。 */
