package com.aritxonly.bdfun.ui

import android.view.TextureView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView

@Composable
internal fun InlineVideoSurface(textureView: TextureView, videoAspect: Float, modifier: Modifier) {
    Box(modifier = modifier.background(Color.Black), contentAlignment = Alignment.Center) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val containerAspect = maxWidth / maxHeight
            val videoModifier = if (videoAspect >= containerAspect) {
                Modifier.fillMaxWidth().aspectRatio(videoAspect)
            } else {
                Modifier.fillMaxHeight().aspectRatio(videoAspect)
            }
            AndroidView(factory = { textureView }, modifier = videoModifier)
        }
    }
}
