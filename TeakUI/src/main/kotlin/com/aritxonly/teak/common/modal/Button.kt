// Migrated from Deadliner 3b561656 working tree; see docs/deadliner-modal-migration.md.
package com.aritxonly.teak.common.modal

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape

// 为官方和 MIUIX 的组件起别名，防止重名冲突
import androidx.compose.material3.LocalContentColor as Material3LocalContentColor
import top.yukonga.miuix.kmp.basic.Button as MiuixButton
import top.yukonga.miuix.kmp.basic.ButtonColors as MiuixButtonColors
import top.yukonga.miuix.kmp.basic.ButtonDefaults as MiuixButtonDefaults
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton
import top.yukonga.miuix.kmp.basic.TextButtonColors as MiuixTextButtonColors
import top.yukonga.miuix.kmp.theme.LocalContentColor as MiuixLocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal enum class MiuixDialogActionStyle {
    Default,
    Secondary,
    Primary,
}

internal val LocalMiuixDialogActionStyle = staticCompositionLocalOf {
    MiuixDialogActionStyle.Default
}

@Composable
private fun resolveMiuixButtonColors(
    style: MiuixDialogActionStyle, fallback: ButtonColors, destructive: Boolean = false,
): MiuixButtonColors {
    if (style == MiuixDialogActionStyle.Default) return MiuixButtonDefaults.buttonColors(
        color = fallback.containerColor, contentColor = fallback.contentColor,
        disabledColor = fallback.disabledContainerColor, disabledContentColor = fallback.disabledContentColor)
    return MiuixButtonDefaults.buttonColors(
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = .85f),
        disabledColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = .85f),
        contentColor = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSecondaryContainer,
        disabledContentColor = MiuixTheme.colorScheme.disabledOnSurface)
}

@Composable
private fun resolveMiuixTextButtonColors(
    style: MiuixDialogActionStyle, destructive: Boolean = false,
): MiuixTextButtonColors {
    if (style == MiuixDialogActionStyle.Default) return MiuixButtonDefaults.textButtonColors()
    return MiuixButtonDefaults.textButtonColors(
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = .85f),
        disabledColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = .85f),
        textColor = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSecondaryContainer,
        disabledTextColor = MiuixTheme.colorScheme.disabledOnSurface)
}

private fun Modifier.miuixDialogActionWidth(style: MiuixDialogActionStyle): Modifier =
    if (style == MiuixDialogActionStyle.Default) this else fillMaxWidth()

/**
 * Deadliner 基础 Button 组件 (实心按钮)
 */
@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    forceMaterial3: Boolean = false,
    destructive: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    val dialogActionStyle = LocalMiuixDialogActionStyle.current
    val miuixColors = resolveMiuixButtonColors(dialogActionStyle, colors, destructive)
    val resolvedContentColor = if (enabled) miuixColors.contentColor else miuixColors.disabledContentColor
    MiuixButton(
        onClick = onClick,
        modifier = modifier.miuixDialogActionWidth(dialogActionStyle),
        enabled = enabled,
        cornerRadius = TeakModalDefaults.ButtonCornerRadius,
        colors = miuixColors,
    ) {
        CompositionLocalProvider(
            Material3LocalContentColor provides resolvedContentColor,
            MiuixLocalContentColor provides resolvedContentColor,
        ) {
            content()
        }
    }
}

/**
 * Deadliner 基础 TextButton 组件 (文本按钮)
 */
@Composable
fun TextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.textShape,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    forceMaterial3: Boolean = false,

    // MIUIX 专属辅助参数
    miuixText: String = "",
    useMiuixDialogActionColors: Boolean = true,

    destructive: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    val dialogActionStyle = LocalMiuixDialogActionStyle.current
    val miuixModifier = modifier.miuixDialogActionWidth(dialogActionStyle)
    if (miuixText.isNotBlank()) {
        MiuixTextButton(
            text = miuixText,
            onClick = onClick,
            modifier = miuixModifier,
            enabled = enabled,
            cornerRadius = TeakModalDefaults.ButtonCornerRadius,
            colors = resolveMiuixTextButtonColors(dialogActionStyle, destructive),
        )
    } else {
        val miuixColors = resolveMiuixButtonColors(
            style = dialogActionStyle,
            fallback = colors, destructive = destructive,
        )
        val resolvedContentColor = if (enabled) miuixColors.contentColor else miuixColors.disabledContentColor
        MiuixButton(
            onClick = onClick,
            modifier = miuixModifier,
            enabled = enabled,
            cornerRadius = TeakModalDefaults.ButtonCornerRadius,
            colors = miuixColors,
        ) {
            CompositionLocalProvider(
                Material3LocalContentColor provides resolvedContentColor,
                MiuixLocalContentColor provides resolvedContentColor,
            ) {
                content()
            }
        }
    }
}
