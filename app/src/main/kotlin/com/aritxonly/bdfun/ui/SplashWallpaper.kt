package com.aritxonly.bdfun.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

/**
 * 开屏：默认使用内置品牌图居中展示；用户自定义壁纸时优先用壁纸铺满。
 *
 * 退出沿用「第一次从开始页进入首页」那套系统过渡：logo 原地淡出，
 * 首页在下面直接显影，不做横向滑出，也不改变 logo 位置。
 */
@Composable
fun SplashWallpaper(uri: String, dark: Boolean = androidx.compose.foundation.isSystemInDarkTheme()) {
    val custom = uri.isNotBlank()
    var visible by remember(uri) { mutableStateOf(true) }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(320, easing = FastOutSlowInEasing),
        label = "splash-alpha",
    )
    LaunchedEffect(uri) {
        delay(1200)
        visible = false
    }
    if (alpha <= 0.01f && !visible) return
    // 开屏在 TeakTheme 之外组合，不能读 MiuixTheme（拿不到宿主深浅色），
    // 因此由 MainActivity 显式传入当前主题，避免深色模式仍闪白底。
    val background = if (dark) androidx.compose.ui.graphics.Color.Black else androidx.compose.ui.graphics.Color(0xFFF3F3F3)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { this.alpha = alpha }
            // 图片是透明 PNG：浅色模式跟页面白底，深色模式跟黑底。
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        if (custom) {
            AsyncImage(
                model = rememberImageRequest(uri, crossfade = false),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Image(
                painter = painterResource(com.aritxonly.bdfun.R.drawable.bdfun_splash),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth(0.52f)
                    .aspectRatio(1536f / 1024f),
            )
        }
    }
}
