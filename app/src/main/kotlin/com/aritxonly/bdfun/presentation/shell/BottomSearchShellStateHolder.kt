package com.aritxonly.bdfun

import androidx.compose.runtime.*
import androidx.compose.animation.core.*
import com.aritxonly.teak.common.search.TeakSearchTransitionDefaults
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class BottomSearchShellStateHolder(
    private val main: MainShellUiStateHolder,
    private val appScope: CoroutineScope,
    private val resetChrome: () -> Unit,
    private val animationsEnabled: () -> Boolean,
) {
    var showBottomSearch by mutableStateOf(false)
    var bottomQuery by mutableStateOf("")
    var bottomSubmitted by mutableStateOf<String?>(null)
    val bottomSearchProgress = Animatable(0f)
    // 反编译里透明度是几条独立的线性时间线，和几何弹簧并行：
    //   遮罩    进入 linear 100ms          / 退出 linear 100ms + 50ms 延迟
    //   容器与搜索栏 进入 linear 100ms + 50ms 延迟 / 退出 linear 100ms
    //   底层页面 进入/退出 linear 150ms
    val bottomMaskAlpha = Animatable(0f)
    val bottomContentAlpha = Animatable(0f)
    val bottomUnderlayAlpha = Animatable(1f)
    // 模糊是独立的 150ms 线性（官方用 ValueAnimator 0→50），不跟弹簧。
    val bottomBlurProgress = Animatable(0f)
    // 顶栏那条：官方 alpha 是 linear(150)，缩放则跟主弹簧。
    val bottomTopBarAlpha = Animatable(0f)
    // 官方点搜索会新开 Activity，带 300ms 的 window 过渡；这里是同一 Activity，
    // 用整页 alpha 复刻同一层：进入 Fade 300ms + delay 60 + SineEaseInOut，
    // 退出 Fade 300ms + spring(0.99, 0.67)。
    val bottomWindowAlpha = Animatable(0f)
    // 底栏单独一条弹簧（0.97 / 0.3），和内容那条（0.97 / 0.4）不是同一个时间线。
    val bottomTabBarProgress = Animatable(0f)
    fun close() {
        // 输入/搜索结果在收起动画期间保持原样，避免动画中先闪出搜索首页。
        if (animationsEnabled()) {
            appScope.launch {
                bottomSearchProgress.animateTo(0f, TeakSearchTransitionDefaults.spec())
                showBottomSearch = false
                bottomQuery = ""
                bottomSubmitted = null
            }
            appScope.launch {
                bottomUnderlayAlpha.animateTo(1f, tween(TeakSearchTransitionDefaults.UnderlayAlphaMs, easing = LinearEasing))
            }
            appScope.launch {
                bottomBlurProgress.animateTo(0f, tween(TeakSearchTransitionDefaults.UnderlayBlurMs, easing = LinearEasing))
            }
            appScope.launch {
                bottomContentAlpha.animateTo(0f, tween(TeakSearchTransitionDefaults.ChromeFadeMs, easing = LinearEasing))
            }
            appScope.launch {
                bottomMaskAlpha.animateTo(
                    0f,
                    tween(TeakSearchTransitionDefaults.ChromeFadeMs, delayMillis = TeakSearchTransitionDefaults.ChromeFadeDelayMs, easing = LinearEasing),
                )
            }
            appScope.launch {
                bottomTabBarProgress.animateTo(0f, TeakSearchTransitionDefaults.tabBarSpec())
            }
            appScope.launch {
                bottomWindowAlpha.animateTo(0f, TeakSearchTransitionDefaults.windowOutSpec())
            }
            appScope.launch {
                bottomTopBarAlpha.animateTo(
                    0f,
                    tween(TeakSearchTransitionDefaults.UnderlayAlphaMs, easing = LinearEasing),
                )
            }
        } else {
            appScope.launch {
                bottomSearchProgress.snapTo(0f)
                bottomUnderlayAlpha.snapTo(1f)
                bottomContentAlpha.snapTo(0f)
                bottomMaskAlpha.snapTo(0f)
                bottomTabBarProgress.snapTo(0f)
                bottomBlurProgress.snapTo(0f)
                bottomTopBarAlpha.snapTo(0f)
                bottomWindowAlpha.snapTo(0f)
            }
            showBottomSearch = false
            bottomQuery = ""
            bottomSubmitted = null
        }
    }
    fun open() {
        resetChrome()
        showBottomSearch = true
        bottomQuery = ""
        bottomSubmitted = null
        main.searchQuery = ""
        main.searchSubmitted = null
        if (animationsEnabled()) {
            appScope.launch {
                bottomSearchProgress.animateTo(1f, TeakSearchTransitionDefaults.spec())
            }
            appScope.launch {
                bottomUnderlayAlpha.animateTo(0f, tween(TeakSearchTransitionDefaults.UnderlayAlphaMs, easing = LinearEasing))
            }
            appScope.launch {
                bottomBlurProgress.animateTo(1f, tween(TeakSearchTransitionDefaults.UnderlayBlurMs, easing = LinearEasing))
            }
            appScope.launch {
                bottomMaskAlpha.animateTo(1f, tween(TeakSearchTransitionDefaults.ChromeFadeMs, easing = LinearEasing))
            }
            appScope.launch {
                bottomContentAlpha.animateTo(
                    1f,
                    tween(TeakSearchTransitionDefaults.ChromeFadeMs, delayMillis = TeakSearchTransitionDefaults.ChromeFadeDelayMs, easing = LinearEasing),
                )
            }
            appScope.launch {
                bottomTabBarProgress.animateTo(1f, TeakSearchTransitionDefaults.tabBarSpec())
            }
            appScope.launch {
                bottomWindowAlpha.animateTo(1f, TeakSearchTransitionDefaults.windowInSpec())
            }
            appScope.launch {
                bottomTopBarAlpha.animateTo(
                    1f,
                    tween(TeakSearchTransitionDefaults.UnderlayAlphaMs, easing = LinearEasing),
                )
            }
        } else {
            appScope.launch {
                bottomSearchProgress.snapTo(1f)
                bottomUnderlayAlpha.snapTo(0f)
                bottomContentAlpha.snapTo(1f)
                bottomMaskAlpha.snapTo(1f)
                bottomTabBarProgress.snapTo(1f)
                bottomBlurProgress.snapTo(1f)
                bottomTopBarAlpha.snapTo(1f)
                bottomWindowAlpha.snapTo(1f)
            }
        }
    }
}
