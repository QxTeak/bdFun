package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.core.model.video.DanmakuCommand
import com.aritxonly.bdfun.core.model.video.DanmakuCommandKind
import com.aritxonly.teak.common.icons.AppGlyph
import com.aritxonly.teak.common.icons.LucideIcons
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.theme.MiuixTheme
/** 互动卡里随视频/弹幕变化的一组状态与回调；集中成一个对象，避免卡片函数参数超限。 */
internal data class DanmakuCommandInteraction(
    val selected: Int?,
    val busy: Boolean,
    val actions: VideoInteractionActions,
    val videoLiked: Boolean,
    val voteCounts: Map<Int, Long>,
    val showVoteRatio: Boolean,
    val onLink: () -> Unit,
    val onSubmit: (Int) -> Unit,
    val authorFace: String = "",
    val followed: Boolean = false,
    val avatar: @Composable (String, Modifier) -> Unit = { _, _ -> },
    val onFollow: () -> Unit = {},
)

@Composable
internal fun DanmakuCommandCard(
    command: DanmakuCommand, interaction: DanmakuCommandInteraction, modifier: Modifier,
    onDismiss: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    val contentColor = Color.White
    // 投票卡按官方样式给更大的内边距和标题字号。
    val vote = command.kind == DanmakuCommandKind.Vote
    Column(
        modifier = modifier
            .clip(shape)
            .background(Color.Black.copy(alpha = 0.68f))
            .border(0.5.dp, Color.White.copy(alpha = 0.22f), shape)
            // 投票/互动卡一次性完整展示，不再用滚动条把选项藏起来。
            .padding(if (vote) 12.dp else 10.dp),
        verticalArrangement = Arrangement.spacedBy(if (vote) 6.dp else 6.dp),
    ) {
        CommandCardHeader(command, contentColor, onDismiss, if (vote) 15.sp else 12.sp)
        CommandCardBody(command, interaction)
    }
}

@Composable
private fun CommandCardHeader(command: DanmakuCommand, contentColor: Color, onDismiss: () -> Unit, titleSize: androidx.compose.ui.unit.TextUnit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = command.text,
            color = contentColor,
            fontSize = titleSize,
            lineHeight = titleSize,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.width(4.dp))
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .clickable(interactionSource = null, indication = null, onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            AppGlyph(MiuixIcons.Close, contentColor.copy(alpha = 0.75f), size = 12.dp, contentDescription = "关闭")
        }
    }
}

@Composable
private fun CommandCardBody(
    command: DanmakuCommand,
    interaction: DanmakuCommandInteraction,
) {
    val contentColor = Color.White
    val selected = interaction.selected
    val busy = interaction.busy
    val actions = interaction.actions
    val videoLiked = interaction.videoLiked
    val voteCounts = interaction.voteCounts
    val showVoteRatio = interaction.showVoteRatio
    val onLink = interaction.onLink
    val onSubmit = interaction.onSubmit
    val authorFace = interaction.authorFace
    val followed = interaction.followed
    val avatar = interaction.avatar
    val onFollow = interaction.onFollow
    when (command.kind) {
        DanmakuCommandKind.Notice -> Unit
        DanmakuCommandKind.Link -> CapsuleButton("打开关联视频", enabled = true, onClick = onLink)
        DanmakuCommandKind.Follow -> {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // 互动项仍保留点赞/投币/收藏；只有「关注」这一项换成播放器同款头像胶囊。
                if (command.attentionType in 1..2) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        CapsuleIconButton(
                            TeakIcon.like(videoLiked),
                            if (videoLiked) MiuixTheme.colorScheme.primary else contentColor,
                            description = if (videoLiked) "取消点赞视频" else "点赞视频",
                            onClick = actions.onLikeClick,
                        )
                        CapsuleIconButton(LucideIcons.CoinsFilled, contentColor, "投币", actions.onCommandCoin)
                        CapsuleIconButton(LucideIcons.Star, contentColor, "收藏", actions.onCommandFavorite)
                    }
                }
                if (command.attentionType != 1) {
                    UpFollowChip(
                        authorFace = authorFace, followed = followed,
                        avatar = avatar, onClick = onFollow,
                    )
                }
            }
        }
        DanmakuCommandKind.Vote -> {
            val total = command.options.sumOf { voteCounts[it.value] ?: it.count }
            command.options.forEach { option ->
                val count = voteCounts[option.value] ?: option.count
                val ratio = if (total > 0L) (count.toFloat() / total).coerceIn(0f, 1f) else 0f
                VoteOptionRow(
                    label = option.label,
                    selected = selected == option.value,
                    showRatio = showVoteRatio,
                    ratio = ratio,
                    enabled = !busy && selected == null && command.interactionId > 0,
                    onClick = { onSubmit(option.value) },
                )
            }
            if (command.participants != null) {
                Text(
                    "${command.participants} 人参与",
                    color = contentColor.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                )
            }
        }
        DanmakuCommandKind.Grade -> {
            // 评分不再显示 1/2/3/4/5 的文字胶囊，改成星星：点第几颗就前几颗变黄。
            GradeStarRow(
                options = command.options,
                selected = selected,
                enabled = !busy && selected == null && command.interactionId > 0,
                onClick = onSubmit,
            )
            val summary = listOfNotNull(
                command.average?.let { "平均 ${String.format(java.util.Locale.ROOT, "%.1f", it)} 分" },
                command.participants?.let { "${it} 人参与" },
            ).joinToString(" · ")
            if (summary.isNotEmpty()) {
                Text(summary, color = contentColor.copy(alpha = 0.7f), fontSize = 11.sp)
            }
        }
    }
}
