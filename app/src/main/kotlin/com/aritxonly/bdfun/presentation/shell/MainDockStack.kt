package com.aritxonly.bdfun

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.aritxonly.bdfun.core.model.video.VideoItem
import com.aritxonly.bdfun.feature.settings.presentation.SettingsBindings
import com.aritxonly.bdfun.ui.BackgroundPlayDisc
import com.aritxonly.bdfun.ui.BackgroundPlayback
import com.aritxonly.bdfun.ui.DiscShadowRoom
import com.aritxonly.teak.common.interaction.blockTouches
import com.aritxonly.teak.common.material.teakGlassControlShadow
import com.aritxonly.teak.common.navigation.TeakFloatingTabBarDefaults

/**
 * HyperOS 4 底栏槽位：搜索键 / 唱片共用一个位置。
 *
 * 静止时只显示一张卡；切换时按小米 WidgetStack 的层样式
 * 从后层翻到前层：back scale 0.95 / rotation 3° / left 6 / top -10。
 *
 * 两张卡的互换有两套入口，共用 [DockStackStateHolder] 的同一个动画和代数守卫：
 * 手指下滑切换，后台播放开始/结束时自动切换（不再硬切）。
 */
private const val HORIZONTAL_TRIGGER_DP = 20f
private const val STACK_BACK_SCALE = 0.95f
private const val STACK_BACK_ROTATION = 3f
private val STACK_BACK_LEFT = 6.dp
private val STACK_BACK_TOP = (-10).dp

/**
 * 玻璃材质自带的周边阴影半径最大约 26dp，外加 5dp 偏移。
 * 动画容器必须比 54dp 的卡片大出这段空间，否则阴影会被容器边界裁成方形。
 */
private val SHADOW_ROOM = DiscShadowRoom

/** 快速收进后层的淡出段：0.55 之前保持不透明，之后平滑淡出。 */
private fun backFade(progress: Float): Float {
    val t = ((progress - 0.55f) / 0.45f).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

@Composable
internal fun MainDockStack(
    settings: SettingsBindings,
    visual: MainBottomNavigationVisual,
    onSearch: () -> Unit,
    onOpenVideo: (VideoItem) -> Unit,
) {
    val slot = TeakFloatingTabBarDefaults.CurrentHeight
    val discAvailable = visual.discSlot > 0.01f
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val state = remember(density, scope) {
        DockStackStateHolder(
            slotPx = with(density) { slot.toPx() },
            scope = scope,
            horizontalTriggerPx = with(density) { HORIZONTAL_TRIGGER_DP.dp.toPx() },
        )
    }
    val slotPx = with(density) { slot.toPx() }
    val tapSlop = with(density) { 6.dp.toPx() }

    LaunchedEffect(discAvailable) { state.syncAvailability(discAvailable) }
    // 只跟踪“是否正在切换”这个布尔值；每帧的进度都在 graphicsLayer 里读，避免逐帧重组两张卡。
    val transitioning by remember(state) { derivedStateOf { state.transition > 0.001f } }
    // 切换过半后把后层升到最上面，让被划走的卡真正“钻到后面”，而不是一直压在上面淡出。
    val incomingOnTop by remember(state) {
        derivedStateOf { state.dismiss > 0.001f || state.switch >= 0.55f }
    }

    Box(
        modifier = Modifier
            .size(slot)
            .pointerInput(discAvailable, state.showDisc) {
                dockStackGestures(
                    state = state,
                    discAvailable = discAvailable,
                    slotPx = slotPx,
                    tapSlop = tapSlop,
                    onTapSearch = onSearch,
                    onTapDisc = { BackgroundPlayback.video?.let(onOpenVideo) },
                )
            },
    ) {
        DockFrontCard(state, slot, slotPx, if (incomingOnTop) 0f else 1f, settings, onSearch, onOpenVideo)
        if (transitioning) {
            DockBackCard(state, slot, if (incomingOnTop) 1f else 0f, settings, onSearch, onOpenVideo)
        }
    }
}

@Composable
private fun DockFrontCard(
    state: DockStackStateHolder,
    slot: Dp,
    slotPx: Float,
    zIndex: Float,
    settings: SettingsBindings,
    onSearch: () -> Unit,
    onOpenVideo: (VideoItem) -> Unit,
) {
    val isDisc = state.currentIsDisc
    Box(Modifier.fillMaxSize().zIndex(zIndex), contentAlignment = Alignment.Center) {
        // 图层比卡片大出一圈阴影空间，材质阴影不会被 54dp 的槽位裁成方形。
        Box(
            modifier = Modifier
                .requiredSize(slot + SHADOW_ROOM * 2)
                .graphicsLayer {
                    val dismiss = state.dismiss
                    val horizontalShift = if (isDisc) state.horizontal * slotPx * 0.35f else 0f
                    if (dismiss > 0f) {
                        // 上滑关闭：唱片向上淡出，后面的搜索键同时升到前层。
                        translationX = horizontalShift
                        translationY = -dismiss * slotPx * 0.9f
                        rotationZ = dismiss * 6f
                        val s = 1f - dismiss * 0.05f
                        scaleX = s
                        scaleY = s
                        alpha = (1f - dismiss).coerceIn(0f, 1f)
                    } else {
                        // 下滑切换：先跟手下沉一点，再收进后层（缩小、旋转、移到后层偏移位）并淡出；
                        // 后层卡同时升到前层。这样被划走的卡是“跑到后面”，不会在底边直接消失。
                        val p = state.switch.coerceIn(0f, 1f)
                        translationX = STACK_BACK_LEFT.toPx() * p + horizontalShift
                        translationY = slotPx * 1.4f * p * (1f - p) + STACK_BACK_TOP.toPx() * p
                        rotationZ = STACK_BACK_ROTATION * p
                        val s = 1f - (1f - STACK_BACK_SCALE) * p
                        scaleX = s
                        scaleY = s
                        alpha = (1f - backFade(p)).coerceIn(0f, 1f)
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            DockCard(isDisc, slot, settings, onSearch, onOpenVideo)
        }
    }
}

@Composable
private fun DockBackCard(
    state: DockStackStateHolder,
    slot: Dp,
    zIndex: Float,
    settings: SettingsBindings,
    onSearch: () -> Unit,
    onOpenVideo: (VideoItem) -> Unit,
) {
    Box(Modifier.fillMaxSize().zIndex(zIndex), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .requiredSize(slot + SHADOW_ROOM * 2)
                .graphicsLayer {
                    // 后层卡从后层偏移位升到前层；同样给阴影留出图层空间。
                    val p = state.transition.coerceIn(0f, 1f)
                    val s = STACK_BACK_SCALE + (1f - STACK_BACK_SCALE) * p
                    scaleX = s
                    scaleY = s
                    rotationZ = STACK_BACK_ROTATION * (1f - p)
                    translationX = STACK_BACK_LEFT.toPx() * (1f - p)
                    translationY = STACK_BACK_TOP.toPx() * (1f - p)
                    alpha = p
                },
            contentAlignment = Alignment.Center,
        ) {
            DockCard(state.incomingIsDisc, slot, settings, onSearch, onOpenVideo)
        }
    }
}

@Composable
private fun DockCard(
    isDisc: Boolean,
    slot: Dp,
    settings: SettingsBindings,
    onSearch: () -> Unit,
    onOpenVideo: (VideoItem) -> Unit,
) {
    // 触摸全部由最上层的堆叠手势层接管，卡片本身不再接收点击。
    // 阴影画在卡片外、但仍在放大后的动画图层内；只有唱片内容需要按圆裁切。
    Box(
        modifier = Modifier
            .size(slot)
            .then(if (isDisc) Modifier.teakGlassControlShadow(CircleShape) else Modifier)
            .blockTouches(true),
        contentAlignment = Alignment.Center,
    ) {
        if (isDisc) {
            Box(Modifier.size(slot).graphicsLayer {
                shape = CircleShape
                clip = true
            }) {
                BackgroundPlayDisc(
                    settings = settings,
                    dependencies = AppGraph.discDependencies,
                    modifier = Modifier.fillMaxSize(),
                    size = slot,
                    outerSize = slot,
                    breathEnabled = false,
                    // 阴影由外层 Deadliner 控件阴影统一绘制，避免两套阴影叠加。
                    shadowEnabled = false,
                    onOpenFallback = onOpenVideo,
                )
            }
        } else {
            MainBottomSearchButton(alpha = 1f, onClick = onSearch)
        }
    }
}
