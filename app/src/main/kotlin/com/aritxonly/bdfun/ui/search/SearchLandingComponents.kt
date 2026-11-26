package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.teak.common.icons.AppGlyph
import com.kyant.shapes.Capsule
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.*
import top.yukonga.miuix.kmp.icon.extended.*
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun SearchSuggestGlyph() {
    AppGlyph(MiuixIcons.Search, MiuixTheme.colorScheme.onBackgroundVariant, size = 16.dp)
}

/** 历史胶囊最多展示的字数，超出部分用省略号。 */
private const val HISTORY_PILL_MAX_CHARS = 7

/**
 * 搜索历史胶囊：尺寸沿用直播页分区胶囊（32dp 高 + 全圆角 + 16dp 横向内边距），
 * 颜色沿用「我的」页「个人空间」按钮的灰色 surfaceContainerHigh。
 *
 * 编辑态下右侧多一颗叉号：整颗胶囊的点击变成无效（避免误触发搜索），
 * 只有叉号可点。
 */
@Composable
internal fun HistoryPill(
    word: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    roomy: Boolean = false,
    /** 长按进入编辑；为 null 时不做长按（媒体库搜索不走编辑态）。 */
    onLongClick: (() -> Unit)? = null,
    /** 非 null 表示处于编辑态，点击胶囊不再搜索，改为显示叉号删除。 */
    onRemove: (() -> Unit)? = null,
) {
    val colors = MiuixTheme.colorScheme
    val haptics = LocalHapticFeedback.current
    val editMode = onRemove != null
    Box(
        modifier = modifier
            .height(if (roomy) 36.dp else 32.dp)
            .clip(Capsule())
            .background(colors.surfaceContainerHigh)
            .combinedClickable(
                interactionSource = null,
                indication = null,
                enabled = !editMode,
                onClick = onClick,
                onLongClick = onLongClick?.let { longPress ->
                    { haptics.performHapticFeedback(HapticFeedbackType.LongPress); longPress() }
                },
            )
            .padding(
                start = if (roomy) 18.dp else 16.dp,
                end = if (editMode) 6.dp else if (roomy) 18.dp else 16.dp,
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = historyPillText(word),
                color = colors.onBackground,
                fontSize = if (roomy) 13.5.sp else 13.sp,
                maxLines = 1,
                softWrap = false,
            )
            if (onRemove != null) {
                // 叉号热区比字形大一圈（24dp），否则 14dp 的字形很难点准。
                Box(
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .clickable(interactionSource = null, indication = null, onClick = onRemove),
                    contentAlignment = Alignment.Center,
                ) {
                    AppGlyph(MiuixIcons.Close, colors.onBackgroundVariant, size = 14.dp, contentDescription = "删除")
                }
            }
        }
    }
}

/** 按 code point 截断，避免把 emoji 之类的代理对拆成半个字。 */
internal fun historyPillText(word: String, maxChars: Int = HISTORY_PILL_MAX_CHARS): String {
    if (word.codePointCount(0, word.length) <= maxChars) return word
    return word.take(word.offsetByCodePoints(0, maxChars)) + "…"
}

@Composable
internal fun SectionHeader(
    title: String,
    action: String?,
    onAction: (() -> Unit)?,
    /** 第二颗右侧按钮（编辑态的「保存」）；为空时只有 [action] 一颗。 */
    primaryAction: String? = null,
    onPrimaryAction: (() -> Unit)? = null,
) {
    val colors = MiuixTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth()
            .padding(
                start = if (com.aritxonly.teak.common.theme.TeakUiStyle.isClassic) 16.dp else 0.dp,
                end = if (com.aritxonly.teak.common.theme.TeakUiStyle.isClassic) 16.dp else 0.dp,
                bottom = 10.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, color = colors.onBackground, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.weight(1f))
        if (action != null && onAction != null) {
            Text(
                text = action,
                color = colors.onBackgroundVariant,
                fontSize = 12.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(interactionSource = null, indication = null, onClick = onAction)
                    // 文字本身只有 12sp 高，补一圈内边距当热区，两颗按钮之间也不至于贴死。
                    .padding(horizontal = 6.dp, vertical = 4.dp),
            )
        }
        if (primaryAction != null && onPrimaryAction != null) {
            Spacer(Modifier.width(4.dp))
            Text(
                text = primaryAction,
                color = colors.primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(interactionSource = null, indication = null, onClick = onPrimaryAction)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
            )
        }
    }
}

/** 热搜 / 搜索发现的两列网格单元：一行一个词条，两个字列等宽。 */
@Composable
internal fun HotWordCell(
    word: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** 词条后面的小标（B 站下发的透明 PNG，例如「独家」）；空串表示没有。 */
    badgeIcon: String = "",
) {
    val colors = MiuixTheme.colorScheme
    Row(
        modifier = modifier
            // 按压水波纹也按全 App 的平滑圆角裁，不用普通圆角。
            .clip(ContinuousCornerShape(12.dp))
            .clickable(interactionSource = null, indication = null, onClick = onClick)
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = word,
            color = colors.onBackground,
            fontSize = 13.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (badgeIcon.isNotBlank()) {
            // 认识的小标直接本地画（省一次图片请求，边缘也更清楚）；不认识的仍走图片兜底。
            val badge = hotBadgeFor(badgeIcon)
            if (badge != null) {
                HotBadgeChip(badge)
            } else {
                coil.compose.AsyncImage(
                    model = rememberImageRequest(
                        com.aritxonly.bdfun.core.common.biliImageUrl(badgeIcon),
                        crossfade = false,
                    ),
                    contentDescription = null,
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .height(15.dp)
                        .widthIn(max = 44.dp),
                )
            }
        }
    }
}

/**
 * 热搜词条后面的小标。
 *
 * B 站下发的是一张 42×42（「独家」是 72×42）的 PNG：底色 + 白色单字。
 * 常见就这几枚且文件名固定，所以按文件名认出来本地画，不再为每个词条拉一张图。
 */
private data class HotBadge(val label: String, val color: Color, val wide: Boolean = false)

private fun hotBadgeFor(iconUrl: String): HotBadge? {
    val file = iconUrl.substringBefore('?').substringAfterLast('/').substringBeforeLast('.').lowercase()
    return when (file) {
        "lrx9rnko24" -> HotBadge("热", Color(0xFFF75858))
        "uf7b1wvkt2" -> HotBadge("新", Color(0xFFFFAF28))
        "eeuqbmwao9" -> HotBadge("梗", Color(0xFFFF6897))
        "obqyphmtcv" -> HotBadge("独家", Color(0xFFFF6897), wide = true)
        else -> null
    }
}

@Composable
private fun HotBadgeChip(badge: HotBadge) {
    Box(
        modifier = Modifier
            .padding(start = 4.dp)
            .height(15.dp)
            .widthIn(min = 15.dp)
            // 跟全 App 一致用连续曲率圆角（squircle），不是普通圆角。
            .clip(ContinuousCornerShape(3.dp))
            .background(badge.color)
            .padding(horizontal = if (badge.wide) 3.dp else 0.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = badge.label,
            color = Color.White,
            fontSize = 9.sp,
            lineHeight = 9.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}
