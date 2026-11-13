package com.aritxonly.bdfun.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.aritxonly.teak.common.settings.TeakSettingsCard
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun NoticePage(
    accepted: Boolean, termsViewed: Boolean, onAcceptedChange: (Boolean) -> Unit,
    onOpenTerms: () -> Unit, onBack: () -> Unit, onNext: () -> Unit,
) {
    val scroll = rememberScrollState()
    GuidePageFrame(onBack = onBack) {
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(scroll).padding(horizontal = 12.dp),
        ) {
            NoticeHeading()
            NoticeTipsCard()
            Spacer(Modifier.height(12.dp))
            NoticeTermsCard(termsViewed, onOpenTerms)
            Spacer(Modifier.height(24.dp))
        }
        NoticeConsentRow(accepted, onAcceptedChange)
        GuidePrimaryPill(
            text = "同意并继续", modifier = Modifier.widthIn(max = 336.dp).fillMaxWidth(),
            enabled = accepted, onClick = onNext,
        )
        Spacer(Modifier.height(44.dp))
    }
}

@Composable
private fun NoticeHeading() {
    val colors = MiuixTheme.colorScheme
    Spacer(Modifier.height(10.dp))
    Box(
        modifier = Modifier.fillMaxWidth().height(82.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = MiuixIcons.Info,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = colors.primary,
        )
    }
    Spacer(Modifier.height(8.dp))
    Box(
        modifier = Modifier.fillMaxWidth().heightIn(min = 42.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "重要提醒",
            style = MiuixTheme.textStyles.title1.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center,
            color = colors.onBackground,
        )
    }
    Spacer(Modifier.height(30.dp))
}

@Composable
private fun NoticeTipsCard() {
    val colors = MiuixTheme.colorScheme
    TeakSettingsCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            GuideNote("本应用是第三方哔哩哔哩客户端，与哔哩哔哩官方无隶属、授权或合作关系。")
            GuideNote("所有视频、评论、账号数据均来自哔哩哔哩公开接口，请遵守官方协议和当地法律。")
            GuideNote("请勿用于商业用途、批量抓取或绕过风控；由此产生的账号风险由使用者自行承担。")
            GuideNote("接口随时可能调整，部分功能可能失效；遇到问题可在「设置 → 关于 → 日志抓取」导出日志反馈。")
            GuideNote("登录凭据只保存在本机，不会上传到任何第三方服务器。")
        }
    }
}

@Composable
private fun NoticeTermsCard(termsViewed: Boolean, onOpenTerms: () -> Unit) {
    val colors = MiuixTheme.colorScheme
    TeakSettingsCard(title = "使用条款", modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(interactionSource = null, indication = null, onClick = onOpenTerms)
                .padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "使用条款与免责声明",
                    style = MiuixTheme.textStyles.headline1,
                    color = colors.onBackground,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = if (termsViewed) "已查看" else "使用前请完整阅读",
                    style = MiuixTheme.textStyles.footnote1,
                    color = if (termsViewed) colors.primary else colors.onSurfaceVariantSummary,
                )
            }
            Text(
                text = "›",
                fontSize = 22.sp,
                color = colors.onSurfaceVariantSummary,
            )
        }
    }
}

@Composable
private fun NoticeConsentRow(accepted: Boolean, onAcceptedChange: (Boolean) -> Unit) {
    val colors = MiuixTheme.colorScheme
    Row(
        modifier = Modifier
            .widthIn(max = 440.dp)
            .fillMaxWidth()
            .toggleable(
                value = accepted,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null,
                role = androidx.compose.ui.semantics.Role.Checkbox,
                onValueChange = onAcceptedChange,
            )
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Checkbox(
            state = androidx.compose.ui.state.ToggleableState(accepted),
            onClick = null,
        )
        Text(
            text = "我已阅读并理解以上提醒",
            modifier = Modifier.weight(1f),
            style = MiuixTheme.textStyles.body2,
            color = colors.onSurfaceContainer,
        )
    }
}

@Composable
private fun GuideNote(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "•",
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            style = MiuixTheme.textStyles.body2.copy(lineHeight = 1.5.em),
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
    }
}
