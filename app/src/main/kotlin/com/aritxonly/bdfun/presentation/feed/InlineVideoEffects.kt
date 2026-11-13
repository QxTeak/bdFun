package com.aritxonly.bdfun.ui

import android.view.TextureView
import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.aritxonly.bdfun.AppGraph
import com.aritxonly.bdfun.core.model.video.PlayInfo
import com.aritxonly.bdfun.feature.feed.application.selectInlineVideoMedia

@Composable
internal fun InlineVideoEffects(
    loaded: PlayInfo, muted: Boolean,
    runtime: InlinePlaybackRuntime, textureView: TextureView,
) {
    val controller = runtime.controller
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(muted, controller) { controller.setMuted(muted) }
    DisposableEffect(controller, textureView) {
        runtime.bindSurface(textureView)
        onDispose { controller.release() }
    }
    val preferredQuality = AppGraph.settings.dynamicAutoPlayQuality
    LaunchedEffect(loaded, controller, preferredQuality) {
        val media = selectInlineVideoMedia(loaded, preferredQuality) ?: return@LaunchedEffect
        controller.prepare(media)
    }
    DisposableEffect(lifecycleOwner, controller) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> controller.pause()
                Lifecycle.Event.ON_START -> controller.play()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}
