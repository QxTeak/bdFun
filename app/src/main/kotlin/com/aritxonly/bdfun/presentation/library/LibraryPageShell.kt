package com.aritxonly.bdfun.ui

import android.content.Context
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.aritxonly.bdfun.LibraryScreenRoute
import com.aritxonly.bdfun.feature.settings.presentation.SettingsBindings
import com.aritxonly.teak.common.navigation.TeakMorphMenuHost
import com.aritxonly.teak.common.navigation.TeakMorphMenuState
import com.aritxonly.teak.common.search.TeakSearchTransitionDefaults

/** 媒体库搜索「一镜到底」的一条时间线，含义与首页底部搜索一致。 */
@Immutable
internal data class LibrarySearchTransition(
    val progress: Float,
    val underlayAlpha: Float,
    val blur: Float,
    val topBarAlpha: Float,
    val contentAlpha: Float,
    val maskAlpha: Float,
    val windowAlpha: Float,
)

/**
 * 收藏 / 历史 / 稍后再看共用的页面外壳：顶栏、批量条、形变菜单和搜索层。
 *
 * 搜索层和首页底部搜索同一套「一镜到底」：底层内容弹簧上移 106dp、150ms
 * 模糊淡出，顶栏同步淡掉；搜索页 300ms 淡入并从下方弹上来，退出整套反向。
 * 搜索页画在 overlay 槽（顶栏之下），退场动画结束后才卸载，所以不会留下
 * 透明层挡住列表滑动，也不会影响顶栏的渐进模糊采样。
 */
@Composable
internal fun LibraryPageShell(
    context: Context,
    settings: SettingsBindings,
    title: String,
    fixedMode: Int,
    onBack: () -> Unit,
) {
    val menuState = remember { TeakMorphMenuState() }
    val searchHost = remember { LibrarySearchOverlayHost() }
    val (search, overlayVisible) = rememberLibrarySearchTransition(searchHost.searching)
    TeakActivityScaffold(
        settings = settings,
        onBack = onBack,
        options = ActivityPageOptions(
            title = title,
            // 宽屏交给网格按首页的 280..560dp 卡片自动加列，不再压成单栏 680dp。
            limitContentWidth = false,
            underlay = PageUnderlayOptions(
                progress = search.progress,
                alpha = search.underlayAlpha,
                blur = search.blur,
                topBarAlpha = search.topBarAlpha,
            ),
        ),
        // 搜索层是整页覆盖，唱片不该浮在搜索页上；等退场动画彻底结束再放回来。
        disc = ActivityDiscOptions(showBackgroundDisc = !overlayVisible),
        slots = ActivityPageSlots(
            topBarActions = { LibraryTopActions(menuState) },
            overlay = {
                LibraryBatchOverlay()
            },
            // 和首页底部搜索一致：搜索层画在最顶层、盖住页面自己的顶栏，
            // 这样旧顶栏淡出时不会和搜索页头部叠在一起被看成「撕裂」。
            topOverlay = {
                if (overlayVisible) {
                    val runtime = searchHost.runtime
                    if (runtime != null) {
                        val data by runtime.data.state.collectAsState()
                        val interaction by runtime.interaction.state.collectAsState()
                        LibrarySearchOverlay(
                            runtime, data, interaction,
                            windowAlpha = search.windowAlpha,
                            maskAlpha = search.maskAlpha,
                            contentAlpha = search.contentAlpha,
                            rise = search.progress,
                        )
                    }
                }
            },
            menuOverlay = {
                TeakMorphMenuHost(menuState) { dismiss ->
                    LibraryMorphMenuContent(dismiss)
                }
            },
        ),
    ) {
        LibraryScreenRoute(context, fixedMode, title, searchOverlayHost = searchHost)
    }
}

/**
 * 搜索展开的一条时间线。这里刻意不把 [LibrarySearchTransition] 放进 StateHolder：
 * 动画全部由 `searching` 这一个开关驱动，Activity 重组时直接读到最新帧。
 *
 * 返回值第二项是搜索层是否还需要留在组合里（含退场动画），
 * 关闭动画播完才卸载，避免透明层继续吞掉页面滑动。
 */
@Composable
private fun rememberLibrarySearchTransition(searching: Boolean): Pair<LibrarySearchTransition, Boolean> {
    val transition = updateTransition(searching, label = "library-search")
    val progress by transition.animateFloat(
        transitionSpec = { TeakSearchTransitionDefaults.spec() },
        label = "library-search-progress",
    ) { if (it) 1f else 0f }
    val underlayAlpha by transition.animateFloat(
        transitionSpec = { tween(TeakSearchTransitionDefaults.UnderlayAlphaMs, easing = LinearEasing) },
        label = "library-search-underlay-alpha",
    ) { if (it) 0f else 1f }
    val blur by transition.animateFloat(
        transitionSpec = { tween(TeakSearchTransitionDefaults.UnderlayBlurMs, easing = LinearEasing) },
        label = "library-search-blur",
    ) { if (it) 1f else 0f }
    val topBarAlpha by transition.animateFloat(
        transitionSpec = {
            // 打开时旧顶栏要赶在搜索页底色上来之前退干净，避免两条顶栏叠在同一位置
            // 被看成一条硬边；关闭时和搜索页一起慢慢露回来。
            if (targetState) tween(90, easing = LinearEasing)
            else tween(TeakSearchTransitionDefaults.UnderlayAlphaMs, easing = LinearEasing)
        },
        label = "library-search-topbar-alpha",
    ) { if (it) 0f else 1f }
    val contentAlpha by transition.animateFloat(
        transitionSpec = {
            if (targetState) {
                tween(
                    TeakSearchTransitionDefaults.ChromeFadeMs,
                    delayMillis = TeakSearchTransitionDefaults.ChromeFadeDelayMs,
                    easing = LinearEasing,
                )
            } else {
                tween(TeakSearchTransitionDefaults.ChromeFadeMs, easing = LinearEasing)
            }
        },
        label = "library-search-content-alpha",
    ) { if (it) 1f else 0f }
    val windowAlpha by transition.animateFloat(
        transitionSpec = {
            if (targetState) TeakSearchTransitionDefaults.windowInSpec()
            else TeakSearchTransitionDefaults.windowOutSpec()
        },
        label = "library-search-window-alpha",
    ) { if (it) 1f else 0f }
    // 和首页一致：白色底单独一条 100ms 时间线（进场延迟 50ms、退场也延迟 50ms）。
    // 退场时底色先透明，页面就不会拖一条长长的白影，看起来才跟首页一样利落。
    val maskAlpha by transition.animateFloat(
        transitionSpec = {
            tween(
                TeakSearchTransitionDefaults.ChromeFadeMs,
                delayMillis = TeakSearchTransitionDefaults.ChromeFadeDelayMs,
                easing = LinearEasing,
            )
        },
        label = "library-search-mask-alpha",
    ) { if (it) 1f else 0f }
    return LibrarySearchTransition(
        progress = progress,
        underlayAlpha = underlayAlpha,
        blur = blur,
        topBarAlpha = topBarAlpha,
        contentAlpha = contentAlpha,
        maskAlpha = maskAlpha,
        windowAlpha = windowAlpha,
    ) to (searching || progress > 0.001f)
}
