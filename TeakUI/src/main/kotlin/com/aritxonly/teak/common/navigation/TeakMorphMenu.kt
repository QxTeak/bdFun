package com.aritxonly.teak.common.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aritxonly.teak.common.chrome.LocalProgressiveTopBarMaterialHosted
import com.aritxonly.teak.common.chrome.LocalTopBarButtonMaterialProgress
import com.aritxonly.teak.common.material.LocalTeakBackdrop
import com.aritxonly.teak.common.theme.TeakUiStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.glass.GlassPopupAnchor
import top.yukonga.miuix.kmp.glass.GlassPopupDefaults
import top.yukonga.miuix.kmp.glass.GlassPopupSizing
import top.yukonga.miuix.kmp.glass.GlassTransformPopup
import top.yukonga.miuix.kmp.glass.glassPopupAnchor

/**
 * 顶栏“省略号 / 更多”这类按钮弹出二级菜单的状态。
 *
 * 动画由 Miuix 的玻璃形变弹层负责（[GlassTransformPopup]）：面板从按钮自身的矩形和圆角
 * 长成菜单，按钮副本参与形变并淡出/淡入。按钮的玻璃材质保持 TeakUI 原样，
 * 这里只记录按钮副本与几何锚点，不接管按钮外观。
 */
@Stable
class TeakMorphMenuState {
    internal var open by mutableStateOf(false)

    /** 宿主窗口可据此在收起动画一开始就释放输入，而不必等面板卸载。 */
    val isOpen: Boolean get() = open

    /** 是否绘制按钮副本：省略号点击时显示；长按卡片时同一套动画但不画省略号。 */
    internal var anchorCopyVisible by mutableStateOf(true)

    /** 代码触发打开（长按用）：同一套形变，可选择不画按钮副本。 */
    fun openWithAnchorCopy(showAnchorCopy: Boolean) {
        anchorCopyVisible = showAnchorCopy
        open = true
    }

    /** 由外部控制器收起。 */
    fun close() {
        open = false
    }

    internal val anchor = GlassPopupAnchor()

    /** 触发按钮的可复用副本；形变起点由 Miuix 使用。 */
    internal var triggerContent by mutableStateOf<(@Composable () -> Unit)?>(null)
}

/** 菜单收起后等待形变回落完再执行的动作延迟。 */
private const val DismissActionDelayMillis = 240L

/**
 * 把触发按钮登记为菜单锚点。
 *
 * [content] 只负责画按钮外观；点击事件由本组件注入，所以可以是玻璃圆钮、纯图标或任意自定义触发器。
 */
@Composable
fun TeakMorphMenuAnchor(
    state: TeakMorphMenuState,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    onClick: (() -> Unit)? = null,
    /** false 时只上报锚点并由 onClick 自行打开菜单（注册表模式），不在这里直接 open。 */
    autoOpen: Boolean = true,
    content: @Composable (onClick: () -> Unit) -> Unit,
) {
    // 副本必须继承触发按钮所在的顶栏材质状态；否则在菜单层里会退回默认值，
    // 顶部未滚动时凭空多出一个玻璃圆底。这两个值会随滚动逐帧变化，用 State 承接，
    // 避免每帧重建副本 lambda 并把宿主标记为脏。
    val hostedNow = LocalProgressiveTopBarMaterialHosted.current
    val progressNow = LocalTopBarButtonMaterialProgress.current
    val hostedState = remember { mutableStateOf(hostedNow) }
    val progressState = remember { mutableFloatStateOf(progressNow) }
    SideEffect {
        hostedState.value = hostedNow
        progressState.value = progressNow
    }
    val currentContent by rememberUpdatedState(content)
    val copy = remember {
        @Composable {
            androidx.compose.runtime.CompositionLocalProvider(
                LocalProgressiveTopBarMaterialHosted provides hostedState.value,
                LocalTopBarButtonMaterialProgress provides progressState.value,
            ) {
                currentContent {}
            }
        }
    }
    SideEffect {
        if (state.triggerContent !== copy) state.triggerContent = copy
    }

    Box(
        modifier = modifier
            .glassPopupAnchor(state.anchor, cornerRadius = cornerRadius),
    ) {
        val scope = rememberCoroutineScope()
        content {
            onClick?.invoke()
            if (autoOpen) {
                // 首次打开时宿主可能还没组合：先让关闭态画一帧，再张开，
                // 否则第一次点省略号会直接出现在全展开位置、没有形变入场。
                scope.launch {
                    withFrameNanos { }
                    withFrameNanos { }
                    state.anchorCopyVisible = true
                    state.open = true
                }
            }
        }
    }
}

/** 长按：把形变起点放在手指位置，不画按钮副本。 */
@Composable
fun TeakMorphPointAnchor(
    state: TeakMorphMenuState,
    position: androidx.compose.ui.geometry.Offset,
) {
    Box(
        Modifier
            .offset { androidx.compose.ui.unit.IntOffset(position.x.toInt(), position.y.toInt()) }
            .size(1.dp)
            .glassPopupAnchor(state.anchor, cornerRadius = 1.dp),
    )
}

/**
 * 玻璃形变菜单宿主，必须和 [TeakMorphMenuAnchor] 放在同一个根 Box 里（根坐标一致）。
 *
 * [content] 里的行通过传入的 `dismiss(after)` 收起菜单，并在回落动画结束后执行 `after`，
 * 保持旧实现“先收菜单再执行动作”的时序。
 */
@Composable
fun TeakMorphMenuHost(
    state: TeakMorphMenuState,
    modifier: Modifier = Modifier,
    minWidth: Dp = GlassPopupDefaults.MinWidth,
    maxWidth: Dp = GlassPopupDefaults.MaxWidth,
    maxHeight: Dp = GlassPopupDefaults.MaxHeight,
    onDismiss: (() -> Unit)? = null,
    content: @Composable ColumnScope.(dismiss: (after: () -> Unit) -> Unit) -> Unit,
) {
    val backdrop = LocalTeakBackdrop.current
    val scope = rememberCoroutineScope()

    val dismiss: (after: () -> Unit) -> Unit = remember(scope) {
        { after ->
            if (state.open) {
                state.open = false
                scope.launch {
                    delay(DismissActionDelayMillis)
                    // 回落期间又被重新打开时，不再执行过期动作。
                    if (!state.open) after()
                }
            } else {
                after()
            }
        }
    }

    BackHandler(enabled = state.open) {
        state.open = false
        onDismiss?.invoke()
    }

    Box(modifier = modifier.fillMaxSize()) {
        GlassTransformPopup(
            show = state.open,
            onDismissRequest = {
                state.open = false
                onDismiss?.invoke()
            },
            anchor = state.anchor,
            backdrop = if (TeakUiStyle.isClassic) null else backdrop,
            anchorContent = {
                val copy = state.triggerContent
                Box(Modifier.fillMaxSize()) {
                    if (copy != null && state.anchorCopyVisible) copy()
                }
            },
            cornerRadius = GlassPopupDefaults.CornerRadius,
            sizing = GlassPopupSizing(minWidth = minWidth, maxWidth = maxWidth, maxHeight = maxHeight),
        ) {
            content(dismiss)
        }
    }
}
