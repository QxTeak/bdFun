package com.aritxonly.bdfun

import com.aritxonly.bdfun.ui.AvatarAccent
import com.aritxonly.bdfun.ui.rememberImageRequest

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
private fun rememberCachedAvatarModel(mid: Long, face: String): Any {
    val context = androidx.compose.ui.platform.LocalContext.current
    val imageCache = androidx.compose.runtime.remember(context) { com.aritxonly.bdfun.ui.ProfileImageCache(context) }
    val cachedAvatar = androidx.compose.runtime.remember(mid) { imageCache.avatarFile(mid) }
    val faceRequest = rememberImageRequest(face, crossfade = false)
    var avatarModel by androidx.compose.runtime.remember(mid, face) {
        androidx.compose.runtime.mutableStateOf<Any>(if (cachedAvatar.exists()) cachedAvatar else faceRequest)
    }
    androidx.compose.runtime.LaunchedEffect(mid, face) {
        val loaded = com.aritxonly.bdfun.ui.loadProfileAvatarBitmap(context, face) ?: return@LaunchedEffect
        if (mid > 0L) imageCache.saveAvatar(mid, loaded)
        avatarModel = loaded
    }
    return avatarModel
}

@Composable
internal fun SpaceProfileAvatar(
    content: SpaceScreenContent, actions: SpaceScreenActions,
) {
    val colors = MiuixTheme.colorScheme
    val profile = content.snapshot.profile.space?.profile
    val avatarModel = rememberCachedAvatarModel(content.mid, profile?.face.orEmpty())
    Box(
        modifier = Modifier
            .offset(y = (-38).dp)
            .size(96.dp)
            // 描边画在外侧，头像缩到 86dp：外径仍是原来的 96dp。
            .drawBehind {
                drawCircle(
                    color = colors.background,
                    radius = size.minDimension / 2f - 2.5.dp.toPx(),
                    style = Stroke(width = 5.dp.toPx()),
                )
            },
    ) {
        AsyncImage(
            model = avatarModel,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(86.dp)
                .align(Alignment.Center)
                .clip(CircleShape)
                .background(Color.White),
        )
        // 头像右侧的浅色弧线 + 小圆点：与「我的」页共用同一份等比点缀。
        AvatarAccent(outerSize = 96.dp)
    }
}

@Composable
internal fun SpaceAvatarStats(
    content: SpaceScreenContent, actions: SpaceScreenActions,
) {
    val context = actions.context
    val colors = MiuixTheme.colorScheme
    val mid = content.mid
    val profile = content.snapshot.profile.space?.profile
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        // 头像放大，压在头图上；右侧加一颗主题色小圆点做点缀。
        SpaceProfileAvatar(content, actions)
        Spacer(Modifier.width(12.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .offset(y = (-4).dp),
        ) {
            // 粉丝 / 关注 / 获赞：和其他圆角卡片同款白色底；经典样式直接平铺在资料区里。
            val classic = com.aritxonly.teak.common.theme.TeakUiStyle.isClassic
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = if (classic) 12.dp else 16.dp)
                    .then(
                        if (classic) Modifier else Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(colors.surfaceContainer),
                    )
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SpaceStat("粉丝", profile?.follower ?: 0, Modifier.weight(1f).clickable { RelationActivity.open(context, mid, true) })
                SpaceStat("关注", profile?.following ?: 0, Modifier.weight(1f).clickable { RelationActivity.open(context, mid, false) })
                SpaceStat("获赞", profile?.likes ?: 0, Modifier.weight(1f))
            }

        }
    }
}
