package com.aritxonly.teak.common.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.aritxonly.teak.common.chrome.TeakChromeDefaults
import com.aritxonly.teak.common.chrome.TeakContinuousCornerShape
import com.aritxonly.teak.common.chrome.TeakLargeTitle
import com.aritxonly.teak.common.chrome.TeakMotion
import com.aritxonly.teak.common.chrome.TeakPageChrome
import com.aritxonly.teak.common.chrome.TeakPageScrollState
import com.aritxonly.teak.common.chrome.TrackTeakPageScroll
import com.aritxonly.teak.common.material.TeakGlassPanel
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val CardShape = TeakContinuousCornerShape(22.dp)

/** 退出动画结束后 Dialog 还留多久（略长于 240ms 的退场），之后必须拆掉，否则会吞点击。 */
private const val ExitHoldMs = 300L

/**
 * 设置页外壳：滚动大标题 + 返回键进度 + 统一列表间距。
 *
 * 页面内容按 [LazyListScope] 追加；卡片本身用 [TeakSettingsCard]，
 * 行用 [TeakSettingsSwitchRow] / [TeakSettingsDropdownRow] / [TeakSettingsActionRow]。
 */
@Composable
fun TeakSettingsPage(
    title: String,
    modifier: Modifier = Modifier,
    pageState: TeakPageScrollState = com.aritxonly.teak.common.chrome.LocalTeakPageScrollState.current,
    subtitle: String? = null,
    contentPadding: PaddingValues = PaddingValues(
        start = if (com.aritxonly.teak.common.theme.TeakUiStyle.isClassic) {
            0.dp
        } else {
            TeakChromeDefaults.ContentHorizontalPadding
        },
        end = if (com.aritxonly.teak.common.theme.TeakUiStyle.isClassic) {
            0.dp
        } else {
            TeakChromeDefaults.ContentHorizontalPadding
        },
        top = TeakChromeDefaults.ContentTopPadding,
        bottom = TeakChromeDefaults.ContentBottomPadding,
    ),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: LazyListScope.() -> Unit,
) {
    val listState = rememberLazyListState()
    TrackTeakPageScroll(listState, pageState)
    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = contentPadding,
        verticalArrangement = verticalArrangement,
    ) {
        item(key = "teak_settings_header") {
            TeakLargeTitle(
                title = title,
                subtitle = subtitle,
                pageState = pageState,
            )
        }
        content()
    }
}

/** 设置分组卡片：可选的小标题 + 连续曲率圆角白 / 深色卡片。 */
@Composable
fun TeakSettingsCard(
    title: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val classic = com.aritxonly.teak.common.theme.TeakUiStyle.isClassic
    Column(modifier = modifier.padding(top = if (classic) 0.dp else 12.dp)) {
        if (!title.isNullOrBlank()) {
            Text(
                text = title,
                modifier = Modifier.padding(
                    start = if (classic) 12.dp else 14.dp,
                    top = if (classic) 12.dp else 0.dp,
                    bottom = 5.dp,
                ),
                color = colors.onBackgroundVariant,
                fontSize = 11.5.sp,
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (classic) {
                        Modifier
                            .background(colors.surface)
                            .drawBehind {
                                val y = size.height - 1.dp.toPx() / 2f
                                drawLine(
                                    color = colors.onBackground.copy(alpha = 0.12f),
                                    start = Offset(0f, y),
                                    end = Offset(size.width, y),
                                    strokeWidth = 1.dp.toPx(),
                                )
                            }
                    } else {
                        Modifier.clip(CardShape).background(colors.surface)
                    },
                ),
            content = content,
        )
    }
}

@Composable
fun TeakSettingsSwitchRow(
    title: String,
    description: String = "",
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = colors.onBackground, fontSize = 16.sp)
            if (description.isNotBlank()) {
                Text(
                    text = description,
                    color = colors.onBackgroundVariant,
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun TeakSettingsValueRow(
    title: String,
    value: String,
) {
    val colors = MiuixTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, color = colors.onBackground, fontSize = 16.sp)
        Spacer(Modifier.weight(1f))
        Text(text = value, color = colors.onBackgroundVariant, fontSize = 13.5.sp)
    }
}

@Composable
fun TeakSettingsActionRow(
    title: String,
    trailing: String = "",
    color: Color? = null,
    onClick: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = null, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, color = color ?: colors.onBackground, fontSize = 16.sp)
        Spacer(Modifier.weight(1f))
        if (trailing.isNotBlank()) {
            Text(text = trailing, color = colors.onBackgroundVariant, fontSize = 13.sp)
            Spacer(Modifier.width(6.dp))
        }
        Text(text = "›", color = colors.onBackgroundVariant, fontSize = 21.sp)
    }
}

/**
 * 点击整行后弹出选项：弹簧 + 由模糊变清晰的玻璃二级菜单。
 *
 * 菜单宽度 196dp，右对齐到当前行，纵向盖住行本身；点外部收起，打开后的 260ms 内不响应外部点击。
 */
@Composable
fun TeakSettingsDropdownRow(
    title: String,
    description: String = "",
    options: List<String>,
    selected: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color? = null,
    panelWidth: androidx.compose.ui.unit.Dp = 220.dp,
    /** 关闭时右侧显示的文字；为空时用当前选项。用于 CDN 这种“收起不显示测速”的场景。 */
    closedValue: String? = null,
) {
    if (options.isEmpty()) return
    val colors = MiuixTheme.colorScheme
    val accent = accentColor ?: MaterialTheme.colorScheme.primary
    // 字号 15sp；颜色仍用 bdFun 主题色。
    val optionTextStyle = remember {
        TextStyle(fontSize = 15.sp, lineHeight = 21.sp, fontWeight = FontWeight.Normal)
    }
    var open by remember { mutableStateOf(false) }
    var rowSize by remember { mutableStateOf(IntSize.Zero) }
    var rowPos by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    val anchorRect = remember(rowPos, rowSize) {
        Rect(
            left = rowPos.x,
            top = rowPos.y,
            right = rowPos.x + rowSize.width,
            bottom = rowPos.y + rowSize.height,
        )
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned {
                rowSize = it.size
                rowPos = it.positionInWindow()
            },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(interactionSource = null, indication = null) { open = !open }
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, color = colors.onBackground, fontSize = 16.sp)
                if (description.isNotBlank()) {
                    Text(
                        text = description,
                        color = colors.onBackgroundVariant,
                        fontSize = 12.5.sp,
                        lineHeight = 17.sp,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = closedValue ?: options.getOrElse(selected) { options.first() },
                color = colors.onBackgroundVariant,
                fontSize = 14.sp,
            )
            Spacer(Modifier.width(6.dp))
            TeakChevronUpDown(color = colors.onBackgroundVariant)
        }

        TeakDropdownPanel(
            visible = open,
            onDismiss = { open = false },
            width = panelWidth,
            anchorBounds = anchorRect,
            anchorTopRatio = 0.36f,
            anchorEndGap = 16.dp,
            contentPadding = 6.dp,
        ) {
            val interactive = LocalTeakDropdownInteractive.current
            options.forEachIndexed { index, option ->
                val active = index == selected
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clickable(
                            interactionSource = null,
                            indication = null,
                            enabled = interactive,
                        ) {
                            onSelected(index)
                            open = false
                        }
                        .padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = option,
                        style = optionTextStyle,
                        color = if (active) accent else colors.onBackground,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.weight(1f))
                    if (active) {
                        Text(
                            text = "✓",
                            style = optionTextStyle.copy(fontWeight = FontWeight.SemiBold),
                            color = accent,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TeakChevronUpDown(color: Color) {    androidx.compose.foundation.Canvas(
        modifier = Modifier
            .width(10.dp)
            .height(14.dp),
    ) {
        val w = size.width
        val h = size.height
        val stroke = 1.5.dp.toPx()
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.12f, h * 0.36f)
            lineTo(w * 0.5f, h * 0.14f)
            lineTo(w * 0.88f, h * 0.36f)
            moveTo(w * 0.12f, h * 0.64f)
            lineTo(w * 0.5f, h * 0.86f)
            lineTo(w * 0.88f, h * 0.64f)
        }
        drawPath(
            path = path,
            color = color,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = stroke,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round,
            ),
        )
    }
}
