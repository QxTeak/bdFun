package com.aritxonly.teak.common.chrome

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 页面内容里的 [TeakLargeTitle] 是否已被 [TeakPinnedLargeTitleHost] 挪到不随滚动位移的覆盖层。
 *
 * 设成 true 时，列表里的那一份只保留占位、不再绘制，避免下拉回弹把大标题一起带下去。
 */
internal val LocalTeakLargeTitleHidden = staticCompositionLocalOf { false }

/**
 * 大标题固定底位：列表里的 [TeakLargeTitle] 只占位，标题本身画在滚动容器之外。
 *
 * 这样内容上滑时大标题照常被推走（[scrollOffsetPx] 为 0 时贴住列表原位，
 * 变大后向上平移），但列表在顶部下拉触发回弹时只有内容位移，大标题不动。
 *
 * 调用方需要把标题原样留在列表头部当占位（高度/间距与原来完全一致），
 * 并传入列表实际的内容顶部内边距 [topInset] 与左右内边距 [horizontalInset]。
 */
@Composable
fun TeakPinnedLargeTitleHost(
    title: String,
    topInset: Dp,
    scrollOffsetPx: () -> Float,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    pageState: TeakPageScrollState = LocalTeakPageScrollState.current,
    horizontalInset: Dp = TeakChromeDefaults.ContentHorizontalPadding,
    topSpacer: Dp = TeakChromeDefaults.LargeTitleTopSpacer,
    titleColor: Color = MiuixTheme.colorScheme.onBackground,
    subtitleColor: Color = MiuixTheme.colorScheme.onBackgroundVariant,
    content: @Composable () -> Unit,
) {
    // 经典样式与宽屏折叠屏本来就不在内容里画大标题，直接透传，避免多画一份。
    if (com.aritxonly.teak.common.theme.TeakUiStyle.isClassic && LocalTeakClassicChrome.current) {
        content()
        return
    }
    if (isWideFoldTopBar()) {
        content()
        return
    }
    Box(modifier = modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalTeakLargeTitleHidden provides true) {
            content()
        }
        TeakLargeTitle(
            title = title,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = horizontalInset, end = horizontalInset, top = topInset)
                .graphicsLayer {
                    // 只抵消内容上滑；回弹位移不在滚动偏移里，所以标题不会被带下去。
                    translationY = -scrollOffsetPx().coerceAtLeast(0f)
                },
            subtitle = subtitle,
            pageState = pageState,
            topSpacer = topSpacer,
            titleColor = titleColor,
            subtitleColor = subtitleColor,
            drawnAsPinnedCopy = true,
        )
    }
}
