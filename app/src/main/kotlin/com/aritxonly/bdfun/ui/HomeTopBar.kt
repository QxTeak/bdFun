package com.aritxonly.bdfun.ui

import com.aritxonly.teak.common.chrome.TeakGlassBackButton

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.teak.common.chrome.teakProgressiveBarBlur
import com.aritxonly.teak.common.material.TeakGlassRecipes
import com.aritxonly.teak.common.material.TeakGlassIconButton
import com.aritxonly.teak.common.material.TeakGlassSurface
import com.aritxonly.teak.common.material.LocalTeakBackdrop
import com.aritxonly.teak.common.material.LocalTeakMaterialSpec
import com.aritxonly.bdfun.BrandPink
import com.aritxonly.bdfun.data.UserProfile
import com.kyant.shapes.Capsule
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Messages
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 顶栏内容高度（不含状态栏）。首页列表的顶部内边距按它 + 状态栏留。 */
val HomeTopBarHeight: Dp = 64.dp

/**
 * 首页顶栏：头像 + 玻璃搜索条 + 玻璃消息键，背景是渐变模糊。
 *
 * 它挂在 App 层（内容采集层之外），所以能直接用底栏那套 [TeakGlassSurface]（同一个 recipe 和
 * 同一块 backdrop），玻璃与底栏完全同源。
 */
@Composable
fun HomeTopBar(
    profile: UserProfile?,
    onSearch: () -> Unit,
    onOpenMessages: () -> Unit,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier,
    unreadCount: Int = 0,
    searchActive: Boolean = false,
    searchQuery: String = "",
    onQueryChange: (String) -> Unit = {},
    onSubmitSearch: () -> Unit = {},
    onCancelSearch: () -> Unit = {},
    onClearQuery: () -> Unit = {},
    searchReveal: Float = 0f,
    /** 搜索结果页：点搜索框重新进入搜索页（历史/联想），而不是直接改当前关键词。 */
    searchResultsVisible: Boolean = false,
    onOpenSearchPage: () -> Unit = {},
) {
    val windowSize = androidx.compose.ui.platform.LocalWindowInfo.current.containerSize
    val density = androidx.compose.ui.platform.LocalDensity.current
    val wide = with(density) { windowSize.width.toDp() >= 600.dp } &&
        com.aritxonly.teak.common.chrome.isWideFoldTopBar()
    val searchWidth = with(density) { (windowSize.width.toDp() * 0.45f).coerceIn(280.dp, 480.dp) }
    val advancedMaterial = LocalTeakMaterialSpec.current
    val recipe = TeakGlassRecipes.floatingNavigation(advancedMaterial.fineTuning)
    val backdrop = LocalTeakBackdrop.current
    val searchInteraction = remember { MutableInteractionSource() }
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(HomeTopBarHeight + statusBarTop),
    ) {
        // 背景单独一层：它自己会按矩形裁剪，不能把外面的东西一起裁掉。
        // 搜索展开时顶栏背景渐隐，让白纸接管，整屏变成同一个颜色。
        val classic = com.aritxonly.teak.common.theme.TeakUiStyle.isClassic
        val classicSurface = MiuixTheme.colorScheme.surface
        val classicLine = MiuixTheme.colorScheme.onBackground.copy(alpha = 0.12f)
        Box(
            modifier = Modifier
                .matchParentSize()
                .then(
                    if (classic) {
                        Modifier
                            .background(classicSurface)
                            .drawBehind {
                                drawLine(
                                    color = classicLine,
                                    start = Offset(0f, size.height),
                                    end = Offset(size.width, size.height),
                                    strokeWidth = 1.dp.toPx(),
                                )
                            }
                    } else {
                        Modifier.teakProgressiveBarBlur()
                    },
                ),
        )

        // 搜索态顶栏是一层“压在内容上的浮层”：空白处的点击不能再穿透到
        // 后面的搜索结果/视频卡片，否则键盘弹起时点搜索框周围会误触。
        if (searchActive) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .blockTouches(true),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .height(HomeTopBarHeight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Crossfade(
                targetState = searchActive,
                animationSpec = tween(durationMillis = 160),
                label = "left-slot",
            ) { searching ->
                if (searching) {
                    // 搜索态返回键：和顶栏/底栏同一套玻璃（真模糊）
                    TeakGlassBackButton(
                        onClick = onCancelSearch,
                        revealed = true,
                        size = 38.dp,
                    )
                } else {
                    TeakAvatar(faceUrl = profile?.face, onClick = onOpenProfile, size = 38.dp)
                }
            }
            Spacer(Modifier.width(10.dp))

            TeakGlassSurface(
                modifier = Modifier
                    .then(if (wide) Modifier.width(searchWidth) else Modifier.weight(1f))
                    .height(40.dp)
                    .then(
                        if (classic) {
                            Modifier.background(
                                MiuixTheme.colorScheme.surfaceContainerHigh,
                                Capsule(),
                            )
                        } else {
                            Modifier
                        },
                    )
                    .then(
                        when {
                            !searchActive -> Modifier.clickable(
                                interactionSource = searchInteraction,
                                indication = null,
                                onClick = onSearch,
                            )
                            searchResultsVisible -> Modifier
                            else -> Modifier
                        },
                    ),
                shape = Capsule(),
                materialAlpha = com.aritxonly.teak.common.chrome.LocalTopBarButtonMaterialProgress.current,
                flatBackgroundAlpha = 0.9f,
                recipe = recipe,
                backdrop = if (classic) null else backdrop,
                contentAlignment = Alignment.CenterStart,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = MiuixIcons.Search,
                        contentDescription = "搜索",
                        tint = MiuixTheme.colorScheme.onBackgroundVariant,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    if (searchActive) {
                        val focusRequester = remember { FocusRequester() }
                        val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
                        val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
                        LaunchedEffect(searchResultsVisible) {
                            if (searchResultsVisible) {
                                // 提交搜索后进入结果页：主动退出输入框并收起输入法。
                                focusManager.clearFocus()
                                keyboard?.hide()
                            } else {
                                // 从结果页回到搜索页时，自动聚焦并弹出输入法。
                                runCatching { focusRequester.requestFocus() }
                                keyboard?.show()
                            }
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = onQueryChange,
                            singleLine = true,
                            textStyle = ComposerTextStyle.copy(
                                color = MiuixTheme.colorScheme.onBackground,
                            ),
                            cursorBrush = SolidColor(BrandPink),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { onSubmitSearch() }),
                            modifier = Modifier.weight(1f, fill = false).focusRequester(focusRequester),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "搜索视频、UP 主",
                                        color = MiuixTheme.colorScheme.onBackgroundVariant,
                                        style = ComposerTextStyle,
                                        maxLines = 1,
                                    )
                                }
                                innerTextField()
                            },
                        )
                    } else {
                        Text(
                            text = "搜索视频、UP 主",
                            color = MiuixTheme.colorScheme.onBackgroundVariant,
                            fontSize = 13.sp,
                            maxLines = 1,
                        )
                    }
                }
                if (searchActive && searchResultsVisible) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable(
                                interactionSource = searchInteraction,
                                indication = null,
                                onClick = onOpenSearchPage,
                            ),
                    )
                }
            }

            if (wide) Spacer(Modifier.weight(1f))
            Spacer(Modifier.width(10.dp))

            Crossfade(
                targetState = searchActive,
                animationSpec = tween(durationMillis = 160),
                label = "right-slot",
            ) { searching ->
                if (searching) {
                TeakGlassIconButton(
                    materialAlpha = com.aritxonly.teak.common.chrome.LocalTopBarButtonMaterialProgress.current,
                    onClick = onClearQuery,
                    size = 40.dp,
                    recipe = recipe,
                    backdrop = backdrop,
                ) {
                    Icon(MiuixIcons.Close, contentDescription = "清空搜索", tint = BrandPink,
                        modifier = Modifier.size(18.dp))
                }
                } else {
                UnreadBadgeHost(count = unreadCount) {
                    TeakGlassIconButton(
                        materialAlpha = com.aritxonly.teak.common.chrome.LocalTopBarButtonMaterialProgress.current,
                        flatBackgroundAlpha = 0.9f,
                        onClick = onOpenMessages, size = 40.dp, recipe = recipe, backdrop = backdrop,
                    ) {
                        Icon(MiuixIcons.Messages, contentDescription = "消息", tint = BrandPink,
                            modifier = Modifier.size(19.dp))
                    }
                }
                }
            }
        }
    }
}

/**
 * 下拉刷新的小电视指示器。**故意挂在 App 层**（不在顶栏内部）：
 * 顶栏背景是 `progressiveTextureBlur`，会给整层建离屏图层并按矩形裁剪，
 * 放里面的话探出顶栏的部分会被切掉。
 *
 * 出现：快速淡入 + 轻微下移；松手开始加载后播放 GIF 动画；加载完带一点回弹缩回。
 */
@Composable
fun HomePullIndicator(
    reveal: Float,
    animated: Boolean,
    modifier: Modifier = Modifier,
) {
    val alpha = reveal.coerceIn(0f, 1f)
    if (alpha <= 0.005f) return

    TeakTvLoading(
        size = 48.dp,
        animated = animated,
        modifier = modifier.graphicsLayer {
            this.alpha = alpha
            translationY = (1f - alpha) * -18f
        },
    )
}

/**
 * 顶栏渐变模糊：实现已抽到 TeakUI 的 `Modifier.teakProgressiveBarBlur()`。
 * 这里保留旧名字，避免首页 / 动态页的调用点到处改。
 */
@Composable
fun Modifier.teakBarBlur(): Modifier = teakProgressiveBarBlur()
