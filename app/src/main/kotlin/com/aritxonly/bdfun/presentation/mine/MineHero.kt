package com.aritxonly.bdfun.ui

import com.aritxonly.teak.common.theme.TeakUiStyle

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.SpaceBanner
import com.aritxonly.bdfun.core.model.user.UserProfile
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun MineHero(profile: UserProfile?, navigation: MineNavigation, wide: Boolean = false) {
    val context = LocalContext.current
    val banner = remember(profile?.mid) { MineBannerStateHolder() }
    LaunchedEffect(banner, profile?.mid, profile?.banner, profile?.bannerFallbacks) {
        banner.load(context, profile, profile?.mid)
    }
    val height = if (wide) 208.dp else 250.dp
    val classic = TeakUiStyle.isClassic
    val shape: androidx.compose.ui.graphics.Shape =
        TeakUiStyle.design.cardShape(ContinuousCornerShape(24.dp))
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val bannerAspect = maxWidth / height
        Box(Modifier.fillMaxWidth()) {
            Box(Modifier.fillMaxWidth().then(
                if (wide && !classic) Modifier.clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)) else Modifier,
            )) {
                // 头图只按图片自身比例裁切，不再套用收藏集装扮的裁切坐标：那套坐标是给竖长图用的，
                // 套到通栏头图上只会显示图片的一个角。
                val bannerLoading = banner.bitmap == null && !profile?.banner.isNullOrBlank()
                if (bannerLoading) {
                    // 头图还没回来：纯色底 + Miuix 默认加载动画，不再用扫光骨架。
                    Box(
                        Modifier.fillMaxWidth().aspectRatio(bannerAspect)
                            .background(MiuixTheme.colorScheme.surfaceContainerHigh),
                        contentAlignment = Alignment.Center,
                    ) {
                        MiuixLoadingRow(verticalPadding = 0.dp)
                    }
                } else {
                    SpaceBanner(banner.bitmap, aspect = bannerAspect)
                }
                Box(Modifier.fillMaxWidth().height(height).background(Brush.verticalGradient(
                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.18f)))))
            }
            Box(Modifier.fillMaxWidth().padding(top = height - 30.dp)
                .padding(horizontal = if (wide || classic) 0.dp else 14.dp)) {
                if (profile != null) MineProfileCard(profile, shape, navigation)
                else MineGuest(shape, navigation.onLogin)
            }
        }
    }
}
