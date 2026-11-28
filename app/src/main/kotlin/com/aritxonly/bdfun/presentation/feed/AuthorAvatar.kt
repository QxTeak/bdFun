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
    hasUpdate: Boolean = false,
    badge: String? = null,
    avatarModifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(56.dp)
            .semantics { stateDescription = if (hasUpdate) "有更新" else badge.orEmpty() }
            .clickable(interactionSource = null, indication = null, onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .then(avatarModifier),
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
        }
        Text(
            text = name,
            color = colors.onBackgroundVariant,
            fontSize = 9.5.sp,
            lineHeight = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            // 名称跟着顶栏材质一起淡出/回来；只改绘制期 alpha，行高不变。
            modifier = Modifier
                .padding(top = 3.dp)
                .feedAvatarLabelFade(LocalFeedAvatarLabelFade.current),
        )
    }
}
