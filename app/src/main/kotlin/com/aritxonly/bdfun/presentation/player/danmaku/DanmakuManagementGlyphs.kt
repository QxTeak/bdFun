package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aritxonly.teak.common.icons.AppGlyph
import com.aritxonly.teak.common.icons.LucideIcons
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Blocklist
import top.yukonga.miuix.kmp.icon.extended.Copy
import top.yukonga.miuix.kmp.icon.extended.Link
import top.yukonga.miuix.kmp.icon.extended.Play
import top.yukonga.miuix.kmp.icon.extended.Report
import top.yukonga.miuix.kmp.icon.extended.Undo

@Composable
internal fun DanmakuManagementGlyph(action: PoolAction, tint: Color, liked: Boolean) {
    when (action) {
        PoolAction.Like -> AppGlyph(if (liked) TeakIcon.Like else TeakIcon.LikeOutline, tint, size = 16.dp)
        PoolAction.FollowSend -> AppGlyph(LucideIcons.SendHorizontalFilled, tint, size = 16.dp)
        else -> {
            val icon = when (action) {
                PoolAction.Seek -> MiuixIcons.Play
                PoolAction.Timestamp -> MiuixIcons.Link
                PoolAction.Copy -> MiuixIcons.Copy
                PoolAction.Report -> MiuixIcons.Report
                PoolAction.Recall -> MiuixIcons.Undo
                PoolAction.Block -> MiuixIcons.Blocklist
                else -> error("此操作使用项目专用图标")
            }
            AppGlyph(icon, tint, size = 16.dp)
        }
    }
}
