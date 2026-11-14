package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner

private class MessagePaneLifecycle : LifecycleOwner {
    val registry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = registry
}

/** 暂时覆盖的页面保留草稿与列表，但停止 RESUMED 轮询并移出无障碍树。 */
@Composable
internal fun MessagePaneVisible(visible: Boolean, content: @Composable () -> Unit) {
    val parent = LocalLifecycleOwner.current
    val owner = remember(parent) { MessagePaneLifecycle() }
    val focus = androidx.compose.ui.platform.LocalFocusManager.current
    LaunchedEffect(visible) { if (!visible) focus.clearFocus(force = true) }
    DisposableEffect(parent, owner, visible) {
        fun update() {
            val ceiling = if (visible) Lifecycle.State.RESUMED else Lifecycle.State.STARTED
            owner.registry.currentState = minOf(parent.lifecycle.currentState, ceiling)
        }
        val observer = LifecycleEventObserver { _, _ -> update() }
        parent.lifecycle.addObserver(observer)
        update()
        onDispose { parent.lifecycle.removeObserver(observer) }
    }
    DisposableEffect(owner) { onDispose { owner.registry.currentState = Lifecycle.State.DESTROYED } }
    Box(Modifier.fillMaxSize().clipToBounds().graphicsLayer { alpha = if (visible) 1f else 0f }
        .then(if (visible) Modifier else Modifier.clearAndSetSemantics {})) {
        CompositionLocalProvider(LocalLifecycleOwner provides owner, content = content)
    }
}
