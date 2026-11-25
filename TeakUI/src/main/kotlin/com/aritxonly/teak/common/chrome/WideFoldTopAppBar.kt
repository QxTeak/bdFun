package com.aritxonly.teak.common.chrome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBarDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 分栏保留真实窗口大小供弹层定位；页面壳可显式沿用外层宽屏策略。 */
val LocalWideFoldTopBarOverride = androidx.compose.runtime.staticCompositionLocalOf<Boolean?> { null }

/** Window policy, not a device/model whitelist. Uses the window before IME insets. */
internal fun usesFixedWideFoldTopBar(widthDp: Float, heightDp: Float): Boolean =
    widthDp >= 400f && heightDp > 0f && heightDp / widthDp <= 1.65f

@Composable
fun isWideFoldTopBar(): Boolean {
    LocalWideFoldTopBarOverride.current?.let { return it }
    val window = LocalWindowInfo.current.containerSize
    val density = LocalDensity.current.density
    return usesFixedWideFoldTopBar(window.width / density, window.height / density)
}

/** App-owned fixed variant of MIUIX 0.9.4-rc01; retains the surrounding glass host. */
@Composable
fun WideFoldTopAppBar(
    title: String,
    subtitle: String = "",
    modifier: Modifier = Modifier,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    titleColor: Color = MiuixTheme.colorScheme.onSurface,
    subtitleColor: Color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
    titleTextStyle: TextStyle = MiuixTheme.textStyles.title2,
    bottomContent: @Composable () -> Unit = {},
) {
    Column(modifier.fillMaxWidth()
        .windowInsetsPadding(WindowInsets.statusBars.only(WindowInsetsSides.Top))) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = TopAppBarDefaults.CollapsedHeight)
                .padding(start = TopAppBarDefaults.NavigationIconPadding,
                    end = TeakChromeDefaults.TopOverlayHorizontalPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            navigationIcon?.invoke()
            Column(Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 4.dp)) {
                if (title.isNotEmpty()) Text(title, color = titleColor, style = titleTextStyle,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() })
                if (subtitle.isNotEmpty()) {
                    Text(subtitle, color = subtitleColor, style = MiuixTheme.textStyles.footnote1,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(TeakChromeDefaults.TopOverlayItemSpacing),
                verticalAlignment = Alignment.CenterVertically, content = actions)
        }
        bottomContent()
    }
}

/** Same call shape used by the modern list, with the upstream compact behavior intact. */
@Composable
fun AdaptiveMiuixTopAppBar(
    title: String,
    largeTitle: String = title,
    modifier: Modifier = Modifier,
    color: Color = MiuixTheme.colorScheme.surface,
    scrollBehavior: ScrollBehavior? = null,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    bottomContent: @Composable () -> Unit = {},
) {
    if (isWideFoldTopBar()) {
        WideFoldTopAppBar(title = largeTitle, modifier = modifier.background(color),
            navigationIcon = navigationIcon, actions = actions, bottomContent = bottomContent)
    } else {
        top.yukonga.miuix.kmp.basic.TopAppBar(title = title, largeTitle = largeTitle,
            modifier = modifier, color = color, scrollBehavior = scrollBehavior,
            navigationIcon = navigationIcon, actions = actions, bottomContent = bottomContent)
    }
}
