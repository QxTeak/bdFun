package com.aritxonly.bdfun.ui

import com.aritxonly.bdfun.core.common.formatCount

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.teak.common.icons.AppGlyph
import com.kyant.shapes.Capsule
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun VideoMetadataAuthor(content: VideoMetadataContent, interaction: com.aritxonly.bdfun.feature.video.application.VideoInteractionState, actions: VideoMetadataActions, appearance: VideoMetadataAppearance, slots: VideoMetadataSlots) {
    val colors = MiuixTheme.colorScheme
    val detail = content.detail
    val video = content.video
    val upFollowers = content.upFollowers
    val upVideos = content.upVideos
    val followed = interaction.followed
    Row(verticalAlignment = Alignment.CenterVertically) {
        // 整块「头像 + 昵称 + 粉丝信息」都是进入 UP 主空间的热区，不再只有头像能点。
        Row(
            modifier = Modifier
                .weight(1f)
                .clickable(
                    interactionSource = null,
                    indication = null,
                ) {
                    actions.onOpenSpace()
                }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
        slots.avatar(
            detail?.ownerFace ?: video.ownerFace,
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(colors.surfaceContainerHigh),
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f).offset(y = 1.dp)) {
            androidx.compose.foundation.text.selection.SelectionContainer {
                Text(
                    text = detail?.ownerName ?: video.ownerName,
                    color = colors.onBackground,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                )
            }
            Text(
                text = when {
                    upFollowers > 0 || upVideos > 0 ->
                        "粉丝 ${formatCount(upFollowers)} · 视频 ${formatCount(upVideos)}"
                    else -> detail?.ownerSign.orEmpty().ifBlank {
                        detail?.tname.orEmpty().ifEmpty { "UP 主" }
                    }
                },
                color = colors.onBackgroundVariant,
                fontSize = 11.sp,
                maxLines = 1,
                modifier = Modifier.offset(x = 1.dp),
            )
        }
        }
        Spacer(Modifier.width(10.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier
                .clip(Capsule())
                .background(if (followed) colors.surfaceContainerHigh else appearance.accentColor)
                .clickable(interactionSource = null, indication = null) {
                    actions.toggleFollow()
                }
                .padding(horizontal = 14.dp, vertical = 5.dp),
        ) {
            if (!followed) AppGlyph(MiuixIcons.Add, Color.White, size = 12.dp)
            Text(
                text = if (followed) "已关注" else "关注",
                color = if (followed) colors.onBackgroundVariant else Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
            )
        }
    }
}
